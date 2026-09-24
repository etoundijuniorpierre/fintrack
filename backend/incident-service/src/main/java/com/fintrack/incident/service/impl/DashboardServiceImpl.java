// Service metier : coordonne les operations du domaine tableau de bord.

package com.fintrack.incident.service.impl;

import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.exception.ErrorCode;
import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.PeriodType;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.entity.IncidentTypeDistribution;
import com.fintrack.incident.model.readmodel.DashboardMetrics;
import com.fintrack.incident.model.readmodel.CohortCompletion;
import com.fintrack.incident.model.readmodel.DateRange;
import com.fintrack.incident.model.readmodel.DurationStats;
import com.fintrack.incident.model.readmodel.EfficiencyScorecard;
import com.fintrack.incident.model.readmodel.MetricScope;
import com.fintrack.incident.model.readmodel.MonthlyMetric;
import com.fintrack.incident.model.readmodel.NamedDurationMetric;
import com.fintrack.incident.model.readmodel.NamedIncidentCount;
import com.fintrack.incident.model.readmodel.PeriodActivityIncidents;
import com.fintrack.incident.model.readmodel.ServiceIncidentCount;
import com.fintrack.incident.repository.IncidentHistoryRepository;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.repository.IncidentResolutionCycleRepository;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.DashboardService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

// Implemente les regles metier du domaine tableau de bord.

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

  private static final String VIEW_OWN = "own";
  private static final String VIEW_AGENCY = "agency";
  private static final String VIEW_SERVICE = "service";
  private static final String VIEW_ALL = "all";
  private static final String INCIDENT_VIEW_ALL = "INCIDENT_VIEW_ALL";

  private final IncidentRepository incidentRepository;
  private final IncidentResolutionCycleRepository cycleRepository;
  private final UserClientService userClientService;
  private final IncidentTypeConfigRepository incidentTypeConfigRepository;
  private final IncidentHistoryRepository incidentHistoryRepository;

  // Derive de la reference unique : reenumerer la liste ici avait fait diverger le
  // stock en cours (TREATED oublie) de la charge par personne (CANCELLED comptee).
  private static final List<IncidentStatus> ACTIVE_STATUSES = List.copyOf(
    IncidentStatus.OPEN_STATUSES
  );

  @Override
  @Cacheable(
    value = "dashboardMetrics",
    key = "{#view, #currentUser.id, #filterAgencyId, #filterServiceId, #filterUserId, #year, #period, #dateFrom, #dateTo}",
    unless = "#result == null"
  )
  // Fournit indicateurs au cas d usage appelant.
  public DashboardMetrics getMetrics(
    String view,
    UserDetailsImpl currentUser,
    UUID filterAgencyId,
    UUID filterServiceId,
    UUID filterUserId,
    int year,
    PeriodType period,
    LocalDateTime dateFrom,
    LocalDateTime dateTo
  ) {
    DashboardMetrics.DashboardMetricsBuilder builder =
      DashboardMetrics.builder();
    MetricScope scope = resolveScope(
      view,
      currentUser,
      filterAgencyId,
      filterServiceId,
      filterUserId
    );
    PeriodType effectivePeriod =
      period != null ? period : PeriodType.LAST_30_DAYS;
    DateRange currentRange = computeDateRange(
      effectivePeriod,
      dateFrom,
      dateTo
    );
    DateRange previousRange = computePreviousDateRange(
      effectivePeriod,
      currentRange,
      dateFrom,
      dateTo
    );

    builder.period(effectivePeriod.getName());
    builder.effectiveDateFrom(
      currentRange != null ? currentRange.getFrom() : null
    );
    builder.effectiveDateTo(currentRange != null ? currentRange.getTo() : null);
    builder.generatedAt(LocalDateTime.now());

    // 1. Incident Counts based on scope and period
    fillIncidentCounts(builder, scope, currentRange, previousRange);

    // 2. Deux jalons distincts : cloture (created -> closed_at) et resolution (created -> resolved_at).
    UUID sUser = scopeUser(scope);
    UUID sAgency = scopeAgency(scope);
    UUID sService = scopeService(scope);
    LocalDateTime from = currentRange != null ? currentRange.getFrom() : null;
    LocalDateTime to = currentRange != null ? currentRange.getTo() : null;
    fillDurationStats(
      builder,
      toDurationStats(
        cycleRepository.findClosureStatsScoped(
          sUser,
          sAgency,
          sService,
          from,
          to
        )
      ),
      toDurationStats(
        cycleRepository.findResolutionStatsScoped(
          sUser,
          sAgency,
          sService,
          from,
          to
        )
      )
    );

    // 3. Distributions
    fillDistributions(builder, scope, currentRange);

    // 4. Recent incidents
    builder.recentActivities(findRecentIncidents(scope));
    UUID personalUserId = VIEW_OWN.equals(scope.getView())
      ? scope.getUserId()
      : currentUser.getId();
    builder.assignedToMe(countActiveAssignedTo(personalUserId));

    // Cartes mes incidents uniquement en vue OWN.
    if (VIEW_OWN.equals(scope.getView()) && scope.getUserId() != null) {
      builder.transferredByMe(
        incidentHistoryRepository.countDistinctIncidentsByActionsAndUserBetween(
          List.of(ActionType.TRANSFER, ActionType.ROUTING),
          scope.getUserId(),
          currentRange != null ? currentRange.getFrom() : null,
          currentRange != null ? currentRange.getTo() : null
        )
      );
      builder.closedByMe(
        incidentHistoryRepository.countDistinctIncidentsByActionAndNewValueAndUserBetween(
          ActionType.STATUS_CHANGE,
          IncidentStatus.CLOSED.name(),
          scope.getUserId(),
          currentRange != null ? currentRange.getFrom() : null,
          currentRange != null ? currentRange.getTo() : null
        )
      );
      builder.resolvedByMe(
        incidentHistoryRepository.countDistinctIncidentsByActionAndNewValueAndUserBetween(
          ActionType.STATUS_CHANGE,
          IncidentStatus.RESOLVED.name(),
          scope.getUserId(),
          currentRange != null ? currentRange.getFrom() : null,
          currentRange != null ? currentRange.getTo() : null
        )
      );
      builder.createdByMe(
        incidentHistoryRepository.countDistinctIncidentsByActionAndUserBetween(
          ActionType.CREATION,
          scope.getUserId(),
          currentRange != null ? currentRange.getFrom() : null,
          currentRange != null ? currentRange.getTo() : null
        )
      );
    }

    fillTopResolvers(builder, scope, currentUser, currentRange);
    fillMonthlySeries(builder, scope, year);
    fillClosureBreakdowns(builder, sUser, sAgency, sService, from, to);
    fillCohortCompletion(builder, sUser, sAgency, sService, from, to);

    return builder.build();
  }

  @Override
  // Listes de flux adossant les compteurs traites/resolus/clotures : memes predicats,
  // meme portee (resolveScope) et meme fenetre (computeDateRange) que fillIncidentCounts.
  public PeriodActivityIncidents getPeriodActivity(
    String view,
    UserDetailsImpl currentUser,
    UUID filterAgencyId,
    UUID filterServiceId,
    UUID filterUserId,
    PeriodType period,
    LocalDateTime dateFrom,
    LocalDateTime dateTo
  ) {
    MetricScope scope = resolveScope(
      view,
      currentUser,
      filterAgencyId,
      filterServiceId,
      filterUserId
    );
    PeriodType effectivePeriod =
      period != null ? period : PeriodType.LAST_30_DAYS;
    DateRange range = computeDateRange(effectivePeriod, dateFrom, dateTo);
    UUID sUser = scopeUser(scope);
    UUID sAgency = scopeAgency(scope);
    UUID sService = scopeService(scope);
    LocalDateTime from = range != null ? range.getFrom() : null;
    LocalDateTime to = range != null ? range.to() : null;
    return PeriodActivityIncidents.builder()
      .treated(
        incidentRepository.findTreatedInPeriod(sUser, sAgency, sService, from, to)
      )
      .resolved(
        incidentRepository.findResolvedInPeriod(sUser, sAgency, sService, from, to)
      )
      .closed(
        incidentRepository.findClosedInPeriod(sUser, sAgency, sService, from, to)
      )
      .build();
  }

  @Override
  @Cacheable(
    value = "dashboardComparisons",
    key = "{#entityType, #ids, #currentUser.id, #year, #period, #dateFrom, #dateTo}",
    unless = "#result == null"
  )
  // Fournit comparison au cas d usage appelant.
  public List<DashboardService.ComparisonEntry> getComparison(
    String entityType,
    List<UUID> ids,
    UserDetailsImpl currentUser,
    int year,
    PeriodType period,
    LocalDateTime dateFrom,
    LocalDateTime dateTo
  ) {
    if (ids == null || ids.isEmpty()) return List.of();
    String type = entityType == null ? "" : entityType.toUpperCase(Locale.ROOT);
    List<DashboardService.ComparisonEntry> entries = new ArrayList<>();
    for (UUID id : ids) {
      DashboardMetrics metrics;
      String name;
      switch (type) {
        case "USER" -> {
          metrics = getMetrics(
            "all",
            currentUser,
            null,
            null,
            id,
            year,
            period,
            dateFrom,
            dateTo
          );
          name = resolveUserName(id);
        }
        case "AGENCY" -> {
          metrics = getMetrics(
            "all",
            currentUser,
            id,
            null,
            null,
            year,
            period,
            dateFrom,
            dateTo
          );
          name = resolveAgencyName(id);
        }
        case "SERVICE" -> {
          metrics = getMetrics(
            "all",
            currentUser,
            null,
            id,
            null,
            year,
            period,
            dateFrom,
            dateTo
          );
          name = resolveServiceName(id);
        }
        default -> throw new BusinessRuleViolationException(
          ErrorCode.STATS_INVALID_COMPARISON_ENTITY,
          ErrorCode.STATS_INVALID_COMPARISON_ENTITY.getMessageKey()
        );
      }
      entries.add(new DashboardService.ComparisonEntry(id, name, metrics));
    }
    return entries;
  }

  // Calcule date range.

  private DateRange computeDateRange(
    PeriodType period,
    LocalDateTime customFrom,
    LocalDateTime customTo
  ) {
    if (period == null || period == PeriodType.ALL) return null;
    LocalDateTime now = LocalDateTime.now();

    return switch (period) {
      case TODAY -> new DateRange(now.toLocalDate().atStartOfDay(), now);
      case LAST_7_DAYS -> new DateRange(now.minusDays(7), now);
      case LAST_30_DAYS -> new DateRange(now.minusDays(30), now);
      case THIS_MONTH -> new DateRange(
        now
          .with(TemporalAdjusters.firstDayOfMonth())
          .toLocalDate()
          .atStartOfDay(),
        now
      );
      case LAST_365_DAYS -> new DateRange(now.minusDays(365), now);
      case LAST_MONTH -> new DateRange(
        now
          .minusMonths(1)
          .with(TemporalAdjusters.firstDayOfMonth())
          .toLocalDate()
          .atStartOfDay(),
        now
          .minusMonths(1)
          .with(TemporalAdjusters.lastDayOfMonth())
          .toLocalDate()
          .atTime(23, 59, 59)
      );
      case LAST_2_MONTHS -> new DateRange(now.minusMonths(2), now);
      case LAST_6_MONTHS -> new DateRange(now.minusMonths(6), now);
      case CUSTOM -> {
        if (customFrom == null || customTo == null) yield null;
        // Realise l'intention metier date range.
        yield new DateRange(customFrom, customTo);
      }
      case ALL -> null;
    };
  }

  // Calcule previous date range.

  private DateRange computePreviousDateRange(
    PeriodType period,
    DateRange currentRange,
    LocalDateTime customFrom,
    LocalDateTime customTo
  ) {
    if (
      period == null || period == PeriodType.ALL || currentRange == null
    ) return null;
    LocalDateTime now = LocalDateTime.now();

    return switch (period) {
      case TODAY -> new DateRange(
        now.minusDays(1).toLocalDate().atStartOfDay(),
        now.minusDays(1).toLocalDate().atTime(23, 59, 59)
      );
      case
        LAST_7_DAYS,
        LAST_30_DAYS,
        LAST_365_DAYS,
        LAST_2_MONTHS,
        LAST_6_MONTHS -> {
        Duration duration = Duration.between(
          currentRange.getFrom(),
          currentRange.getTo()
        );
        LocalDateTime previousEnd = currentRange.getFrom().minusNanos(1);
        yield new DateRange(previousEnd.minus(duration), previousEnd);
      }
      case THIS_MONTH -> {
        Duration duration = Duration.between(
          currentRange.getFrom(),
          currentRange.getTo()
        );
        LocalDateTime previousStart = currentRange.getFrom().minusMonths(1);
        LocalDateTime previousMonthEnd = currentRange.getFrom().minusNanos(1);
        LocalDateTime previousEnd = previousStart.plus(duration);
        yield new DateRange(
          previousStart,
          previousEnd.isAfter(previousMonthEnd) ? previousMonthEnd : previousEnd
        );
      }
      case LAST_MONTH -> new DateRange(
        now
          .minusMonths(2)
          .with(TemporalAdjusters.firstDayOfMonth())
          .toLocalDate()
          .atStartOfDay(),
        now
          .minusMonths(2)
          .with(TemporalAdjusters.lastDayOfMonth())
          .toLocalDate()
          .atTime(23, 59, 59)
      );
      case CUSTOM -> {
        if (customFrom == null || customTo == null) yield null;
        Duration duration = Duration.between(customFrom, customTo);
        // Realise l'intention metier date range.
        yield new DateRange(
          customFrom.minus(duration).minusNanos(1),
          customFrom.minusNanos(1)
        );
      }
      case ALL -> null;
    };
  }

  // Les deux series suivent le perimetre demande : une tendance existe dans chaque vue.
  private void fillMonthlySeries(
    DashboardMetrics.DashboardMetricsBuilder builder,
    MetricScope scope,
    int year
  ) {
    UUID sUser = scopeUser(scope);
    UUID sAgency = scopeAgency(scope);
    UUID sService = scopeService(scope);
    builder.monthlyClosures(
      mapMonthlyMetric(
        cycleRepository.findMonthlyClosureCountsScoped(
          year,
          sUser,
          sAgency,
          sService
        )
      )
    );
    builder.monthlyAvgClosureHours(
      mapMonthlyMetric(
        cycleRepository.findMonthlyAvgClosureHoursScoped(
          year,
          sUser,
          sAgency,
          sService
        )
      )
    );
  }

  // Contrepoids au biais de survie des familles de delai : celles-ci ne voient que
  // les incidents clotures, donc jamais les plus longs. On suit ici la cohorte creee
  // sur la periode, encore-ouverts compris, et on refuse d'annoncer un percentile que
  // la cohorte n'a pas atteint.
  private void fillCohortCompletion(
    DashboardMetrics.DashboardMetricsBuilder builder,
    UUID userId,
    UUID agencyId,
    UUID serviceId,
    LocalDateTime from,
    LocalDateTime to
  ) {
    List<Object[]> rows = incidentRepository.findCohortCompletionScoped(
      userId,
      agencyId,
      serviceId,
      from,
      to,
      LocalDateTime.now()
    );
    if (rows == null || rows.isEmpty()) {
      builder.cohortCompletion(CohortCompletion.builder().build());
      return;
    }
    Object[] row = rows.get(0);
    // Un percentile vaut NULL quand la cohorte ne l'a pas atteint : trop d'incidents
    // sont encore ouverts pour que la moitie (ou les 90 %) soit deja cloturee.
    boolean p50Reached = row[2] != null;
    boolean p90Reached = row[3] != null;
    double maxElapsed = toDouble(row[5]);
    builder.cohortCompletion(
      CohortCompletion.builder()
        .size(row[0] == null ? 0L : ((Number) row[0]).longValue())
        .closedCount(row[1] == null ? 0L : ((Number) row[1]).longValue())
        .p50Reached(p50Reached)
        .p50Hours(p50Reached ? toDouble(row[2]) : maxElapsed)
        .p90Reached(p90Reached)
        .p90Hours(p90Reached ? toDouble(row[3]) : maxElapsed)
        .openMedianAgeHours(toDouble(row[4]))
        .maxElapsedHours(maxElapsed)
        .build()
    );
  }

  // Ou passe le temps : delai de cloture ventile par type et par criticite.
  private void fillClosureBreakdowns(
    DashboardMetrics.DashboardMetricsBuilder builder,
    UUID userId,
    UUID agencyId,
    UUID serviceId,
    LocalDateTime from,
    LocalDateTime to
  ) {
    List<Object[]> byType = cycleRepository.findClosureHoursByTypeScoped(
      userId,
      agencyId,
      serviceId,
      from,
      to
    );
    Map<UUID, String> typeNames = resolveTypeNames(
      byType.stream().map(row -> (UUID) row[0]).toList()
    );
    builder.closureHoursByType(
      byType
        .stream()
        .map(row ->
          NamedDurationMetric.builder()
            .name(typeLabel(typeNames, (UUID) row[0]))
            .avgHours(row[1] == null ? 0.0 : ((Number) row[1]).doubleValue())
            .sampleSize(((Number) row[2]).longValue())
            .build()
        )
        .sorted(Comparator.comparingDouble(NamedDurationMetric::getAvgHours).reversed())
        .toList()
    );
    builder.closureHoursByCriticality(
      cycleRepository
        .findClosureHoursByCriticalityScoped(
          userId,
          agencyId,
          serviceId,
          from,
          to
        )
        .stream()
        .map(row ->
          NamedDurationMetric.builder()
            .name(((Criticality) row[0]).getName())
            .avgHours(row[1] == null ? 0.0 : ((Number) row[1]).doubleValue())
            .sampleSize(((Number) row[2]).longValue())
            .build()
        )
        .sorted(Comparator.comparingDouble(NamedDurationMetric::getAvgHours).reversed())
        .toList()
    );
  }

  // Convertit les donnees du domaine tableau de bord entre les modeles utilises.

  private List<MonthlyMetric> mapMonthlyMetric(List<Object[]> rows) {
    return rows
      .stream()
      .map(obj ->
        new MonthlyMetric(
          ((Number) obj[0]).intValue(),
          ((Number) obj[1]).doubleValue()
        )
      )
      .collect(Collectors.toList());
  }

  // Replie une agregation groupee (cle, compte) en Map ; le calcul reste en base.

  private Map<String, Long> mapCountsByKey(List<Object[]> rows) {
    if (rows == null) return Map.of();
    Map<String, Long> counts = new LinkedHashMap<>();
    for (Object[] row : rows) {
      if (row == null || row[0] == null) continue;
      counts.merge(
        String.valueOf(row[0]),
        row[1] == null ? 0L : ((Number) row[1]).longValue(),
        Long::sum
      );
    }
    return counts;
  }

  // Realise l'intention metier fill top resolvers.
  private void fillTopResolvers(
    DashboardMetrics.DashboardMetricsBuilder builder,
    MetricScope scope,
    UserDetailsImpl currentUser,
    DateRange currentRange
  ) {
    String view = scope.getView();
    LocalDateTime from = currentRange != null ? currentRange.getFrom() : null;
    LocalDateTime to = currentRange != null ? currentRange.getTo() : null;

    if (VIEW_ALL.equals(view) && hasAuthority(currentUser, INCIDENT_VIEW_ALL)) {
      builder.topServices(
        mapServiceCounts(incidentRepository.findTopServices(from, to))
      );
      builder.topAgencies(
        mapAgencyCounts(incidentRepository.findTopAgencies(from, to))
      );
      builder.topResolvers(
        mapUserCounts(incidentRepository.findTopResolvers(from, to))
      );
      return;
    }
    if (VIEW_AGENCY.equals(view) && scope.getAgencyId() != null) {
      builder.topResolvers(
        mapUserCounts(
          incidentRepository.findTopResolversByAgency(
            scope.getAgencyId(),
            from,
            to
          )
        )
      );
      return;
    }
    if (VIEW_SERVICE.equals(view) && scope.getServiceId() != null) {
      builder.topResolvers(
        mapUserCounts(
          incidentRepository.findTopResolversByService(
            scope.getServiceId(),
            from,
            to
          )
        )
      );
    }
  }

  // Convertit les donnees du domaine tableau de bord entre les modeles utilises.

  private List<ServiceIncidentCount> mapServiceCounts(List<Object[]> rows) {
    return rows
      .stream()
      .limit(5)
      .map(obj ->
        new ServiceIncidentCount(
          resolveServiceName(obj[0]),
          ((Number) obj[1]).longValue()
        )
      )
      .collect(Collectors.toList());
  }

  // Convertit les donnees du domaine tableau de bord entre les modeles utilises.

  private List<NamedIncidentCount> mapAgencyCounts(List<Object[]> rows) {
    return rows
      .stream()
      .limit(5)
      .map(obj ->
        new NamedIncidentCount(
          resolveAgencyName(obj[0]),
          ((Number) obj[1]).longValue()
        )
      )
      .collect(Collectors.toList());
  }

  // Convertit les donnees du domaine tableau de bord entre les modeles utilises.

  private List<NamedIncidentCount> mapUserCounts(List<Object[]> rows) {
    List<Object[]> limitedRows = rows
      .stream()
      .limit(5)
      .collect(Collectors.toList());
    Set<UUID> userIds = limitedRows
      .stream()
      .filter(obj -> obj[0] != null)
      .map(obj ->
        obj[0] instanceof UUID id ? id : UUID.fromString(obj[0].toString())
      )
      .collect(Collectors.toSet());
    userClientService.prefetchUsers(userIds);

    return limitedRows
      .stream()
      .map(obj ->
        new NamedIncidentCount(
          resolveUserName(obj[0]),
          ((Number) obj[1]).longValue()
        )
      )
      .collect(Collectors.toList());
  }

  // Resolut scope.
  private MetricScope resolveScope(
    String view,
    UserDetailsImpl user,
    UUID filterAgencyId,
    UUID filterServiceId,
    UUID filterUserId
  ) {
    if (filterUserId != null) {
      return new MetricScope(VIEW_OWN, filterUserId, null, null);
    }
    if (filterServiceId != null) {
      return new MetricScope(VIEW_SERVICE, null, null, filterServiceId);
    }
    if (filterAgencyId != null) {
      return new MetricScope(VIEW_AGENCY, null, filterAgencyId, null);
    }
    return new MetricScope(
      normalizeView(view),
      user.getId(),
      user.getAgencyId(),
      user.getServiceId()
    );
  }

  // Realise l'intention metier fill incident counts.

  private void fillIncidentCounts(
    DashboardMetrics.DashboardMetricsBuilder builder,
    MetricScope scope,
    DateRange current,
    DateRange previous
  ) {
    LocalDateTime from = current != null ? current.getFrom() : null;
    LocalDateTime to = current != null ? current.to() : null;
    LocalDateTime pFrom = previous != null ? previous.getFrom() : null;
    LocalDateTime pTo = previous != null ? previous.to() : null;

    String normalizedView = scope.getView();
    UUID sUser = scopeUser(scope);
    UUID sAgency = scopeAgency(scope);
    UUID sService = scopeService(scope);

    long active = countStatuses(ACTIVE_STATUSES, scope, null, null); // CURRENT
    long blocked = countStatus(IncidentStatus.BLOCKED, scope, null, null); // CURRENT
    long open = countStatus(IncidentStatus.OPEN, scope, null, null); // CURRENT
    long pending = countStatus(
      IncidentStatus.PENDING_VALIDATION,
      scope,
      null,
      null
    ); // CURRENT

    long total = countTotal(scope, from, to); // CREATED_DURING
    long closed = incidentRepository.countClosedInPeriod(
      sUser,
      sAgency,
      sService,
      from,
      to
    ); // CLOSED_DURING
    long resolved = incidentRepository.countResolvedInPeriod(
      sUser,
      sAgency,
      sService,
      from,
      to
    ); // RESOLVED_DURING
    long treated = incidentRepository.countTreatedInPeriod(
      sUser,
      sAgency,
      sService,
      from,
      to
    ); // TREATED_DURING
    long rejected = incidentRepository.countRejectedInPeriod(
      sUser,
      sAgency,
      sService,
      from,
      to
    ); // ACTION_DURING

    builder.activeIncidents(active);
    builder.closedIncidents(closed);
    builder.resolvedIncidents(resolved);
    builder.treatedIncidents(treated);
    builder.rejectedIncidents(rejected);
    builder.blockedIncidents(blocked);
    builder.totalIncidents(total);
    builder.openCount(open);
    builder.pendingCount(pending);

    if (previous != null) {
      builder.openCountPreviousPeriod(
        countStatus(IncidentStatus.OPEN, scope, pFrom, pTo)
      );
      builder.pendingCountPreviousPeriod(
        countStatus(IncidentStatus.PENDING_VALIDATION, scope, pFrom, pTo)
      );
      builder.resolvedCountPreviousPeriod(
        incidentRepository.countResolvedInPeriod(
          sUser,
          sAgency,
          sService,
          pFrom,
          pTo
        )
      );
    }

    // Doctrine "base a risque" : chaque taux est rapporte a la population qui pouvait produire l'evenement.
    LocalDateTime today = LocalDateTime.now();

    // Lot 2 taux + effectif (denominateur)
    long transferred = cycleRepository.countTransferredScoped(
      sUser,
      sAgency,
      sService,
      from,
      to
    );
    long takenInCharge = cycleRepository.countTakenInChargeScoped(
      sUser,
      sAgency,
      sService,
      from,
      to
    );
    long closedInPeriod = closed;
    long closedWithSla = incidentRepository.countClosedWithSlaInPeriod(
      sUser,
      sAgency,
      sService,
      from,
      to
    );
    // Taux de reouverture : mesure de cycle, donc lue sur les cycles. L'historique
    // comptait des incidents distincts — un incident resolu, rouvert, resolu puis
    // rouvert a nouveau ne pesait qu'une fois de chaque cote du rapport. Les cycles
    // anterieurs a V2_5 sont repris en un seul par incident : sur une periode
    // anterieure a cette migration, les reouvertures passees ne sont pas restituees.
    long resolvedReached = cycleRepository.countResolvedCyclesScoped(
      sUser,
      sAgency,
      sService,
      from,
      to
    );
    long slaCompliant = countSlaCompliant(scope, from, to);
    long reopenedAfterResolution =
      cycleRepository.countReopenedAfterResolutionScoped(
        sUser,
        sAgency,
        sService,
        from,
        to
      );

    double slaCompliance =
      closedWithSla > 0 ? ((double) slaCompliant / closedWithSla) * 100.0 : 0.0;
    double reopenRate =
      resolvedReached > 0
        ? ((double) reopenedAfterResolution / resolvedReached) * 100.0
        : 0.0;
    double transferRate =
      takenInCharge > 0 ? ((double) transferred / takenInCharge) * 100.0 : 0.0;

    // Numerateurs exposes a cote des taux : un tableau de bord lisible annonce
    // « 16 sur 20 » plutot qu'un pourcentage que le lecteur doit reconvertir.
    builder.transferRate(transferRate);
    builder.transferCount(transferred);
    builder.transferDenominator(takenInCharge);
    builder.slaComplianceRate(slaCompliance);
    builder.slaCompliantCount(slaCompliant);
    builder.slaDenominator(closedWithSla);
    builder.resolutionReopenRate(reopenRate);
    builder.reopenedCount(reopenedAfterResolution);
    builder.reopenDenominator(resolvedReached);
    builder.avgTimeToFirstResponse(
      averageHoursBetween(
        cycleRepository.findFirstResponseDatesScoped(
          sUser,
          sAgency,
          sService,
          from,
          to
        )
      )
    );

    // Lot 3 KPI Directeur : flux + snapshots
    // Le solde doit compter TOUTES les sorties : compter les seules clotures faisait
    // gonfler le backlog des rejets et des annulations, qui quittent pourtant le stock.
    // La ventilation est la source unique du total affiche.
    Map<String, Long> exitsByStatus = mapCountsByKey(
      incidentRepository.findTerminalExitsByStatusInPeriod(
        IncidentStatus.namesOf(IncidentStatus.TERMINAL_STATUSES),
        sUser,
        sAgency,
        sService,
        from,
        to
      )
    );
    long exitsClosed = exitsByStatus.getOrDefault(
      IncidentStatus.CLOSED.getName(),
      0L
    );
    long exitsRejected = exitsByStatus.getOrDefault(
      IncidentStatus.REJECTED.getName(),
      0L
    );
    long exitsCancelled = exitsByStatus.getOrDefault(
      IncidentStatus.CANCELLED.getName(),
      0L
    );
    long terminalExits = exitsClosed + exitsRejected + exitsCancelled;
    // Sans les reentrees, le solde net ne boucle pas.
    long reEntries = incidentRepository.countBacklogReEntriesInPeriod(
      IncidentStatus.namesOf(IncidentStatus.TERMINAL_STATUSES),
      sUser,
      sAgency,
      sService,
      from,
      to
    );
    builder.inflow(total);
    builder.outflow(terminalExits);
    builder.exitsClosed(exitsClosed);
    builder.exitsRejected(exitsRejected);
    builder.exitsCancelled(exitsCancelled);
    builder.backlogReEntries(reEntries);
    builder.netBacklog(total + reEntries - terminalExits);
    builder.cancelledIncidents(
      incidentRepository.countCancelledInPeriod(
        sUser,
        sAgency,
        sService,
        from,
        to
      )
    );
    builder.ageDistribution(computeAgeDistribution(findActiveCreatedAt(scope)));
    builder.slaBreachNow(
      incidentRepository.countSlaBreachNow(
        IncidentStatus.NOT_LATE_STATUSES,
        sUser,
        sAgency,
        sService,
        today
      )
    );
    builder.cohortOutcome(
      computeCohortOutcome(sUser, sAgency, sService, from, to, today, total)
    );
    if (!VIEW_OWN.equals(normalizedView)) {
      List<Object[]> workloadRows = incidentRepository.findWorkloadScoped(
        IncidentStatus.OPEN_STATUSES,
        sAgency,
        sService
      );
      builder.workload(
        workloadRows == null ? List.of() : mapUserCounts(workloadRows)
      );
    }

    // Lot 4: Distribution by Status
    Map<String, Long> statusDist = new LinkedHashMap<>();
    for (IncidentStatus status : IncidentStatus.values()) {
      statusDist.put(status.name(), countStatus(status, scope, null, null));
    }
    builder.distributionByStatus(statusDist);

    // Lot 5-bis: Scorecard (poids explicites 0.33/0.33/0.34, "crees" exclu du scoring)
    if (
      VIEW_AGENCY.equals(normalizedView) || VIEW_SERVICE.equals(normalizedView)
    ) {
      double delayScore = slaCompliance / 100.0;
      double qualityScore = 1.0 - reopenRate / 100.0;
      double throughputScore =
        total > 0 ? Math.min(1.0, (double) closedInPeriod / total) : 0.0;
      double compositeScore =
        delayScore * 0.33 + qualityScore * 0.33 + throughputScore * 0.34;
      builder.efficiencyScorecard(
        EfficiencyScorecard.builder()
          .delayScore(delayScore * 100.0)
          .qualityScore(qualityScore * 100.0)
          .throughputScore(throughputScore * 100.0)
          .compositeScore(compositeScore * 100.0)
          .build()
      );
    }
  }

  // Recupere user id.
  private UUID scopeUser(MetricScope s) {
    return VIEW_OWN.equals(s.getView()) ? s.getUserId() : null;
  }

  // Recupere agency id.
  private UUID scopeAgency(MetricScope s) {
    return VIEW_AGENCY.equals(s.getView()) ? s.getAgencyId() : null;
  }

  // Recupere service id.
  private UUID scopeService(MetricScope s) {
    return VIEW_SERVICE.equals(s.getView()) ? s.getServiceId() : null;
  }

  // Calcule cohort outcome.
  private Map<String, Long> computeCohortOutcome(
    UUID user,
    UUID agency,
    UUID service,
    LocalDateTime from,
    LocalDateTime to,
    LocalDateTime today,
    long total
  ) {
    long closedOnTime = incidentRepository.countCohortClosedOnTime(
      user,
      agency,
      service,
      from,
      to
    );
    long closedLate = incidentRepository.countCohortClosedLate(
      user,
      agency,
      service,
      from,
      to
    );
    long openLate = incidentRepository.countCohortOpenLate(
      IncidentStatus.NOT_LATE_STATUSES,
      user,
      agency,
      service,
      today,
      from,
      to
    );
    long rejected = incidentRepository.countCohortRejected(
      user,
      agency,
      service,
      from,
      to
    );
    // Sans cette ligne, un incident annule n'etait ni cloture, ni en retard, ni rejete :
    // il tombait dans le reste et s'affichait comme « encore ouvert, dans les temps ».
    long cancelled = incidentRepository.countCohortCancelled(
      user,
      agency,
      service,
      from,
      to
    );
    long openInTime =
      total - closedOnTime - closedLate - openLate - rejected - cancelled;
    if (openInTime < 0) openInTime = 0;
    Map<String, Long> cohort = new LinkedHashMap<>();
    cohort.put("closedOnTime", closedOnTime);
    cohort.put("closedLate", closedLate);
    cohort.put("openInTime", openInTime);
    cohort.put("openLate", openLate);
    cohort.put("rejected", rejected);
    cohort.put("cancelled", cancelled);
    return cohort;
  }

  // Alimente les deux familles de delai a partir de leurs agregats respectifs.
  private void fillDurationStats(
    DashboardMetrics.DashboardMetricsBuilder builder,
    DurationStats closure,
    DurationStats resolution
  ) {
    builder.avgClosureHours(closure.getAvgHours());
    builder.medianClosureHours(closure.getP50Hours());
    builder.p90ClosureHours(closure.getP90Hours());
    builder.closureSampleSize(closure.getSampleSize());
    // Meme population, horloge arretee deduite : comparable au taux de conformite SLA.
    builder.avgNetClosureHours(closure.getAvgNetHours());
    builder.medianNetClosureHours(closure.getP50NetHours());

    builder.avgResolutionHours(resolution.getAvgHours());
    builder.medianResolutionHours(resolution.getP50Hours());
    builder.p90ResolutionHours(resolution.getP90Hours());
    builder.resolutionSampleSize(resolution.getSampleSize());
  }

  // Lit la ligne unique renvoyee par l'agregat SQL.
  private DurationStats toDurationStats(List<Object[]> rows) {
    if (rows == null || rows.isEmpty()) {
      return DurationStats.builder().build();
    }
    Object[] row = rows.get(0);
    if (row == null || row.length < 6) {
      return DurationStats.builder().build();
    }
    return DurationStats.builder()
      .avgHours(toDouble(row[0]))
      .p50Hours(toDouble(row[1]))
      .p90Hours(toDouble(row[2]))
      .sampleSize(row[3] == null ? 0L : ((Number) row[3]).longValue())
      .avgNetHours(toDouble(row[4]))
      .p50NetHours(toDouble(row[5]))
      .build();
  }

  // Realise l'intention metier conversion en reel.
  private double toDouble(Object value) {
    return value == null ? 0.0 : ((Number) value).doubleValue();
  }

  // Recherche les incidents pour actif creation at.

  private List<LocalDateTime> findActiveCreatedAt(MetricScope scope) {
    return switch (scope.getView()) {
      case VIEW_OWN -> incidentRepository.findActiveCreatedAtForOwn(
        ACTIVE_STATUSES,
        scope.getUserId()
      );
      case VIEW_AGENCY -> incidentRepository.findActiveCreatedAtInAgency(
        ACTIVE_STATUSES,
        scope.getAgencyId()
      );
      case VIEW_SERVICE -> incidentRepository.findActiveCreatedAtInService(
        ACTIVE_STATUSES,
        scope.getServiceId()
      );
      default -> incidentRepository.findActiveCreatedAtInAll(ACTIVE_STATUSES);
    };
  }

  // Calcule age distribution.

  private Map<String, Long> computeAgeDistribution(List<LocalDateTime> dates) {
    long days3 = 0;
    long days7 = 0;
    long days30 = 0;
    long daysOver30 = 0;
    LocalDateTime now = LocalDateTime.now();

    for (LocalDateTime date : dates) {
      long days = ChronoUnit.DAYS.between(
        date.toLocalDate(),
        now.toLocalDate()
      );
      if (days <= 3) {
        days3++;
      } else if (days <= 7) {
        days7++;
      } else if (days <= 30) {
        days30++;
      } else {
        daysOver30++;
      }
    }

    Map<String, Long> dist = new LinkedHashMap<>();
    dist.put("0-3", days3);
    dist.put("4-7", days7);
    dist.put("8-30", days30);
    dist.put(">30", daysOver30);
    return dist;
  }

  // Calcule le nombre de incidents pour transfert.

  // Calcule le nombre de incidents pour SLA conformite.

  private long countSlaCompliant(
    MetricScope scope,
    LocalDateTime from,
    LocalDateTime to
  ) {
    return switch (scope.getView()) {
      case VIEW_OWN -> incidentRepository.countSlaCompliantForOwn(
        scope.getUserId(),
        from,
        to
      );
      case VIEW_AGENCY -> incidentRepository.countSlaCompliantInAgency(
        scope.getAgencyId(),
        from,
        to
      );
      case VIEW_SERVICE -> incidentRepository.countSlaCompliantInService(
        scope.getServiceId(),
        from,
        to
      );
      default -> incidentRepository.countSlaCompliantInAll(from, to);
    };
  }

  // Calcule le nombre de incidents pour statut.

  private long countStatus(
    IncidentStatus status,
    MetricScope scope,
    LocalDateTime from,
    LocalDateTime to
  ) {
    return switch (scope.getView()) {
      case VIEW_OWN -> incidentRepository.countByStatusForOwn(
        status,
        scope.getUserId(),
        from,
        to
      );
      case VIEW_AGENCY -> from == null
        ? incidentRepository.countByStatusAndAgencyId(
            status,
            scope.getAgencyId()
          )
        : incidentRepository.countByStatusAndAgencyIdAndCreatedAtBetween(
            status,
            scope.getAgencyId(),
            from,
            to
          );
      case VIEW_SERVICE -> from == null
        ? incidentRepository.countByStatusForService(
            status,
            scope.getServiceId()
          )
        : incidentRepository.countByStatusForServiceBetween(
            status,
            scope.getServiceId(),
            from,
            to
          );
      default -> from == null
        ? incidentRepository.countByStatus(status)
        : incidentRepository.countByStatusAndCreatedAtBetween(status, from, to);
    };
  }

  // Calcule le nombre de incidents pour statuts.

  private long countStatuses(
    List<IncidentStatus> statuses,
    MetricScope scope,
    LocalDateTime from,
    LocalDateTime to
  ) {
    return switch (scope.getView()) {
      case VIEW_OWN -> incidentRepository.countByStatusesForOwn(
        statuses,
        scope.getUserId(),
        from,
        to
      );
      case VIEW_AGENCY -> incidentRepository.countByStatusInAgency(
        statuses,
        scope.getAgencyId(),
        from,
        to
      );
      case VIEW_SERVICE -> incidentRepository.countByStatusInService(
        statuses,
        scope.getServiceId(),
        from,
        to
      );
      default -> incidentRepository.countByStatusInAll(statuses, from, to);
    };
  }

  // Calcule le nombre de incidents pour total.

  private long countTotal(
    MetricScope scope,
    LocalDateTime from,
    LocalDateTime to
  ) {
    return switch (scope.getView()) {
      case VIEW_OWN -> incidentRepository.countTotalForOwn(
        scope.getUserId(),
        from,
        to
      );
      case VIEW_AGENCY -> from == null
        ? incidentRepository.countByAgencyId(scope.getAgencyId())
        : incidentRepository.countByAgencyIdAndCreatedAtBetween(
            scope.getAgencyId(),
            from,
            to
          );
      case VIEW_SERVICE -> from == null
        ? incidentRepository.countForService(scope.getServiceId())
        : incidentRepository.countForServiceBetween(
            scope.getServiceId(),
            from,
            to
          );
      default -> from == null
        ? incidentRepository.count()
        : incidentRepository.countByCreatedAtBetween(from, to);
    };
  }

  // Realise l'intention metier fill distributions.

  private void fillDistributions(
    DashboardMetrics.DashboardMetricsBuilder builder,
    MetricScope scope,
    DateRange range
  ) {
    LocalDateTime from = range != null ? range.getFrom() : null;
    LocalDateTime to = range != null ? range.getTo() : null;

    List<IncidentTypeDistribution> typeDist;
    List<Object[]> critDist;
    String normalizedView = scope.getView();

    if (VIEW_OWN.equals(normalizedView)) {
      // Perimetre OWN = union (cree OU assigne OU traite) + periode, coherent avec distributionByStatus.
      typeDist = incidentRepository.findTypeDistributionForOwn(
        scope.getUserId(),
        from,
        to
      );
      critDist = incidentRepository.countByCriticalityForOwn(
        scope.getUserId(),
        from,
        to
      );
    } else if (VIEW_AGENCY.equals(normalizedView)) {
      typeDist =
        from == null
          ? incidentRepository.findTypeDistributionByAgencyId(
              scope.getAgencyId()
            )
          : incidentRepository.findTypeDistributionByAgencyIdBetween(
              scope.getAgencyId(),
              from,
              to
            );
      critDist = incidentRepository.countByCriticalityAndAgencyId(
        scope.getAgencyId(),
        from,
        to
      );
    } else if (VIEW_SERVICE.equals(normalizedView)) {
      typeDist =
        from == null
          ? incidentRepository.findTypeDistributionForService(
              scope.getServiceId()
            )
          : incidentRepository.findTypeDistributionForServiceBetween(
              scope.getServiceId(),
              from,
              to
            );
      critDist = incidentRepository.countByCriticalityForService(
        scope.getServiceId(),
        from,
        to
      );
    } else {
      typeDist =
        from == null
          ? incidentRepository.findTypeDistributionAll()
          : incidentRepository.findTypeDistributionBetween(from, to);
      critDist = incidentRepository.countByCriticality(from, to);
    }

    // Repli sur l'identifiant seulement quand la projection ne porte pas le libelle :
    // ces types-la se resolvent en une requete, pas une par ligne.
    Map<UUID, String> distributionTypeNames = resolveTypeNames(
      typeDist
        .stream()
        .filter(item -> item != null && !hasTypeName(item))
        .map(IncidentTypeDistribution::getTypeId)
        .toList()
    );
    builder.distributionByType(
      typeDist
        .stream()
        .collect(
          Collectors.toMap(
            item -> resolveTypeName(item, distributionTypeNames),
            IncidentTypeDistribution::getCount,
            Long::sum,
            LinkedHashMap::new
          )
        )
    );

    builder.distributionByCriticality(
      critDist
        .stream()
        .collect(
          Collectors.toMap(
            obj -> ((Criticality) obj[0]).getName(),
            obj -> ((Number) obj[1]).longValue()
          )
        )
    );
  }

  // Selectionne les incidents recents pour recent incidents.

  private List<IncidentHistory> findRecentIncidents(MetricScope scope) {
    String normalizedView = scope.getView();

    if (VIEW_OWN.equals(normalizedView)) {
      return incidentHistoryRepository.findRecentForUser(
        scope.getUserId(),
        PageRequest.of(0, 10)
      );
    }

    if (VIEW_AGENCY.equals(normalizedView)) {
      return incidentHistoryRepository.findRecentForAgency(
        scope.getAgencyId(),
        PageRequest.of(0, 10)
      );
    }

    if (VIEW_SERVICE.equals(normalizedView)) {
      return incidentHistoryRepository.findRecentForService(
        scope.getServiceId(),
        PageRequest.of(0, 10)
      );
    }

    return incidentHistoryRepository
      .findAll(PageRequest.of(0, 10, Sort.by("createdAt").descending()))
      .getContent();
  }

  // Normalise les valeurs du domaine tableau de bord avant traitement.

  private String normalizeView(String view) {
    return view == null ? "" : view.toLowerCase(Locale.ROOT);
  }

  // Resout service name a partir du contexte disponible.

  private String resolveServiceName(Object rawServiceId) {
    if (rawServiceId == null) return "";
    try {
      UUID serviceId =
        rawServiceId instanceof UUID id
          ? id
          : UUID.fromString(rawServiceId.toString());
      return userClientService.resolveServiceName(serviceId);
    } catch (RuntimeException ex) {
      return rawServiceId.toString();
    }
  }

  // Resout agence name a partir du contexte disponible.

  private String resolveAgencyName(Object rawAgencyId) {
    if (rawAgencyId == null) return "";
    try {
      UUID agencyId =
        rawAgencyId instanceof UUID id
          ? id
          : UUID.fromString(rawAgencyId.toString());
      var agency = userClientService.resolveAgency(agencyId);
      return agency != null && agency.getName() != null
        ? agency.getName()
        : agencyId.toString();
    } catch (RuntimeException ex) {
      return rawAgencyId.toString();
    }
  }

  // Resout utilisateur name a partir du contexte disponible.

  private String resolveUserName(Object rawUserId) {
    if (rawUserId == null) return "";
    try {
      UUID userId =
        rawUserId instanceof UUID id
          ? id
          : UUID.fromString(rawUserId.toString());
      var user = userClientService.resolveUser(userId);
      if (user == null) return userId.toString();
      String first = user.getFirstName() != null ? user.getFirstName() : "";
      String last = user.getLastName() != null ? user.getLastName() : "";
      String fullName = (first + " " + last).trim();
      return fullName.isEmpty()
        ? user.getUsername() != null
          ? user.getUsername()
          : userId.toString()
        : fullName;
    } catch (RuntimeException ex) {
      return rawUserId.toString();
    }
  }

  // Verifie que les regles metier autorisent l operation sur incident.

  private boolean hasAuthority(UserDetailsImpl user, String authority) {
    return user
      .getAuthorities()
      .stream()
      .anyMatch(a -> authority.equalsIgnoreCase(a.getAuthority()));
  }

  // Calcule le nombre de incidents pour actif assigne fin.

  private long countActiveAssignedTo(UUID userId) {
    if (userId == null) return 0L;
    return incidentRepository.countByAssignedToAndStatusNotIn(
      userId,
      List.copyOf(IncidentStatus.TERMINAL_STATUSES)
    );
  }

  // Resout type name a partir du contexte disponible.

  private String resolveTypeName(
    IncidentTypeDistribution item,
    Map<UUID, String> names
  ) {
    if (item == null) return "-";
    if (hasTypeName(item)) return item.getTypeName();
    return typeLabel(names, item.getTypeId());
  }

  // Vrai si la projection porte deja le libelle du type.
  private boolean hasTypeName(IncidentTypeDistribution item) {
    return item.getTypeName() != null && !item.getTypeName().isBlank();
  }

  // Libelles des types demandes, en UNE requete. Resoudre ligne par ligne transformait
  // une ventilation en autant d'allers-retours en base que de types representes.
  private Map<UUID, String> resolveTypeNames(Collection<UUID> typeIds) {
    Set<UUID> ids = typeIds
      .stream()
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    if (ids.isEmpty()) return Map.of();
    try {
      return incidentTypeConfigRepository
        .findAllById(ids)
        .stream()
        .collect(
          Collectors.toMap(
            IncidentTypeConfig::getId,
            this::displayTypeName,
            (first, ignored) -> first
          )
        );
    } catch (RuntimeException ex) {
      // L'annuaire des types est indisponible : on affiche les identifiants bruts
      // plutot que de faire echouer tout le tableau de bord.
      return Map.of();
    }
  }

  // Libelle d'un type deja resolu, avec repli sur l'identifiant brut.
  private String typeLabel(Map<UUID, String> names, UUID typeId) {
    if (typeId == null) return "-";
    return names.getOrDefault(typeId, typeId.toString());
  }

  // Realise l'intention metier display type name.

  private String displayTypeName(IncidentTypeConfig typeConfig) {
    if (
      typeConfig.getDisplayName() != null &&
      !typeConfig.getDisplayName().isBlank()
    ) return typeConfig.getDisplayName();
    return typeConfig.getName() != null
      ? typeConfig.getName()
      : typeConfig.getId().toString();
  }

  // Moyenne, en heures, des couples (debut, fin) fournis par une requete de jalons.
  private double averageHoursBetween(List<Object[]> milestoneDates) {
    if (milestoneDates == null) return 0.0;
    return milestoneDates
      .stream()
      .filter(arr -> arr[0] != null && arr[1] != null)
      .mapToDouble(
        arr ->
          Duration.between(
            (LocalDateTime) arr[0],
            (LocalDateTime) arr[1]
          ).toMinutes() / 60.0
      )
      .average()
      .orElse(0.0);
  }
}
