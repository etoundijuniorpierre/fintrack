// Mapper : convertit les donnees liees a incident entre modeles.

package com.fintrack.incident.model.mapper;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.dto.request.IncidentRequest;
import com.fintrack.incident.model.dto.request.IncidentSearchRequest;
import com.fintrack.incident.model.dto.request.IncidentUpdateRequest;
import com.fintrack.incident.model.dto.response.AgencySummaryResponse;
import com.fintrack.incident.model.dto.response.CohortCompletionResponse;
import com.fintrack.incident.model.dto.response.ComparisonEntry;
import com.fintrack.incident.model.dto.response.ComparisonResponse;
import com.fintrack.incident.model.dto.response.DashboardMetricsResponse;
import com.fintrack.incident.model.dto.response.EfficiencyScorecardResponse;
import com.fintrack.incident.model.dto.response.IncidentCommentResponse;
import com.fintrack.incident.model.dto.response.IncidentHistoryResponse;
import com.fintrack.incident.model.dto.response.IncidentResponse;
import com.fintrack.incident.model.dto.response.IncidentSummaryResponse;
import com.fintrack.incident.model.dto.response.IncidentTypeConfigResponse;
import com.fintrack.incident.model.dto.response.MonthlyMetricResponse;
import com.fintrack.incident.model.dto.response.NamedDurationMetricResponse;
import com.fintrack.incident.model.dto.response.NamedIncidentCountResponse;
import com.fintrack.incident.model.dto.response.ServiceIncidentCountResponse;
import com.fintrack.incident.model.dto.response.ServiceSummaryResponse;
import com.fintrack.incident.model.dto.response.UserSummaryResponse;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentComment;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.model.entity.IncidentSearchCriteria;
import com.fintrack.incident.model.readmodel.CohortCompletion;
import com.fintrack.incident.model.readmodel.DashboardMetrics;
import com.fintrack.incident.model.readmodel.EfficiencyScorecard;
import com.fintrack.incident.model.readmodel.MonthlyMetric;
import com.fintrack.incident.model.readmodel.NamedDurationMetric;
import com.fintrack.incident.model.readmodel.NamedIncidentCount;
import com.fintrack.incident.model.readmodel.ServiceIncidentCount;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import com.fintrack.incident.repository.IncidentHistoryRepository;
import com.fintrack.incident.service.DashboardService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

// Mapper MapStruct pour la conversion des incidents entre entites, DTOs et requetes.

@Mapper(
  componentModel = "spring",
  uses = { IncidentHistoryMapper.class, IncidentCommentMapper.class },
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
// Assure les conversions du domaine incident.
public abstract class IncidentMapper {

  @Autowired
  protected UserClientService userClientService;

  @Autowired
  protected IncidentTypeConfigRepository incidentTypeConfigRepository;

  @Autowired
  protected IncidentHistoryRepository incidentHistoryRepository;

  @Autowired
  protected IncidentTypeConfigMapper incidentTypeConfigMapper;

  @Autowired
  protected ReportingSystemConfigClientService reportingSystemConfigClientService;

  private final ThreadLocal<
    Map<UUID, IncidentTypeConfigResponse>
  > incidentTypeCache = new ThreadLocal<>();

  // Precise l'intention metier associee a incident.

  @Mapping(
    target = "type",
    source = "typeId",
    qualifiedByName = "uuidToTypeConfigResponse"
  )
  @Mapping(
    target = "createdBy",
    source = "createdBy",
    qualifiedByName = "uuidToUserSummary"
  )
  @Mapping(
    target = "validatedBy",
    source = "validatedBy",
    qualifiedByName = "uuidToUserSummary"
  )
  @Mapping(
    target = "assignedTo",
    source = "assignedTo",
    qualifiedByName = "uuidToUserSummary"
  )
  @Mapping(
    target = "blockedBy",
    source = "blockedBy",
    qualifiedByName = "uuidToUserSummary"
  )
  @Mapping(
    target = "reopenedBy",
    source = "reopenedBy",
    qualifiedByName = "uuidToUserSummary"
  )
  @Mapping(
    target = "transferredToService",
    source = "transferredToService",
    qualifiedByName = "uuidToServiceSummary"
  )
  @Mapping(
    target = "agency",
    source = "agencyId",
    qualifiedByName = "uuidToAgencySummary"
  )
  @Mapping(
    target = "history",
    source = "history",
    qualifiedByName = "historySetToList"
  )
  @Mapping(
    target = "comments",
    source = "comments",
    qualifiedByName = "commentSetToList"
  )
  public abstract IncidentResponse toResponse(Incident entity);

  @AfterMapping
  protected void computeIsReopenExpired(
    Incident entity,
    @MappingTarget IncidentResponse response
  ) {
    if (
      entity.getTransferredAt() != null &&
      entity.getTransferredToService() == null
    ) {
      response.setTransferredToAgency(
        userClientService.resolveAgency(entity.getAgencyId())
      );
    }
    long maxReopenCount = reportingSystemConfigClientService.getThresholdLong(
      "maxReopenCount",
      2
    );
    response.setIsMaxReopenReached(entity.getReopenCount() >= maxReopenCount);

    if (entity.getStatus() == IncidentStatus.REJECTED) {
      long limitHours = reportingSystemConfigClientService.getThresholdLong(
        "reopenTimeLimitHours",
        48
      );
      LocalDateTime referenceTime = resolveReopenReferenceTime(entity);
      if (
        referenceTime != null &&
        LocalDateTime.now().isAfter(referenceTime.plusHours(limitHours))
      ) {
        response.setIsReopenExpired(true);
      } else {
        response.setIsReopenExpired(false);
      }
    } else {
      response.setIsReopenExpired(false);
    }
  }

  private LocalDateTime resolveReopenReferenceTime(Incident incident) {
    if (incident.getHistory() == null) return incident.getUpdatedAt();
    return incident
      .getHistory()
      .stream()
      .filter(
        h ->
          IncidentStatus.RESOLVED.getName().equals(h.getNewValue()) ||
          IncidentStatus.REJECTED.getName().equals(h.getNewValue())
      )
      .max(Comparator.comparing(IncidentHistory::getCreatedAt))
      .map(IncidentHistory::getCreatedAt)
      .orElse(incident.getUpdatedAt());
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  @Mapping(
    target = "type",
    source = "typeId",
    qualifiedByName = "uuidToTypeConfigResponse"
  )
  @Mapping(
    target = "createdBy",
    source = "createdBy",
    qualifiedByName = "uuidToUserSummary"
  )
  @Mapping(
    target = "assignedTo",
    source = "assignedTo",
    qualifiedByName = "uuidToUserSummary"
  )
  @Mapping(
    target = "agency",
    source = "agencyId",
    qualifiedByName = "uuidToAgencySummary"
  )
  @Mapping(
    target = "transferredToService",
    source = "transferredToService",
    qualifiedByName = "uuidToServiceSummary"
  )
  public abstract IncidentSummaryResponse toSummaryResponse(Incident entity);

  // Convertit les donnees du domaine incident entre les modeles utilises.

  public List<IncidentSummaryResponse> toSummaryResponseList(
    List<Incident> entities
  ) {
    if (entities == null) return null;
    Set<UUID> userIds = new HashSet<>();
    Set<UUID> agencyIds = new HashSet<>();
    Set<UUID> serviceIds = new HashSet<>();
    Set<UUID> typeIds = new HashSet<>();
    Set<UUID> incidentIds = new HashSet<>();
    for (Incident i : entities) {
      if (i.getId() != null) incidentIds.add(i.getId());
      if (i.getCreatedBy() != null) userIds.add(i.getCreatedBy());
      if (i.getAssignedTo() != null) userIds.add(i.getAssignedTo());
      if (i.getAgencyId() != null) agencyIds.add(i.getAgencyId());
      if (i.getTransferredToService() != null) serviceIds.add(
        i.getTransferredToService()
      );
      if (i.getTypeId() != null) typeIds.add(i.getTypeId());
    }
    userClientService.prefetchUsers(userIds);
    userClientService.prefetchAgencies(agencyIds);
    userClientService.prefetchServices(serviceIds);

    Map<UUID, IncidentTypeConfigResponse> typesById =
      incidentTypeConfigRepository
        .findAllById(typeIds)
        .stream()
        .map(incidentTypeConfigMapper::toResponse)
        .collect(
          Collectors.toMap(
            IncidentTypeConfigResponse::getId,
            Function.identity()
          )
        );
    incidentTypeCache.set(typesById);
    try {
      Map<UUID, Set<UUID>> participantsByIncident = participantUsers(
        incidentIds
      );
      return entities
        .stream()
        .map(entity -> {
          IncidentSummaryResponse response = toSummaryResponse(entity);
          response.setParticipantUserIds(
            participantsByIncident.getOrDefault(entity.getId(), Set.of())
          );
          return response;
        })
        .toList();
    } finally {
      incidentTypeCache.remove();
    }
  }

  // Regroupe les acteurs d'historique par incident a partir d'une requete batch.
  private Map<UUID, Set<UUID>> participantUsers(Set<UUID> incidentIds) {
    if (incidentIds.isEmpty()) return Map.of();
    Map<UUID, Set<UUID>> participants = new HashMap<>();
    for (
      Object[] row : incidentHistoryRepository.findParticipantRowsByIncidentIds(
        incidentIds
      )
    ) {
      if (row[0] instanceof UUID incidentId && row[1] instanceof UUID userId) {
        participants
          .computeIfAbsent(incidentId, ignored -> new HashSet<>())
          .add(userId);
      }
    }
    return participants;
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  public abstract IncidentSearchCriteria toSearchCriteria(
    IncidentSearchRequest request
  );

  // Convertit incident vers le modele attendu par la couche appelante.
  public ComparisonResponse toComparisonResponse(
    String entityType,
    List<DashboardService.ComparisonEntry> entries
  ) {
    List<ComparisonEntry> mapped = (
      entries == null ? List.<DashboardService.ComparisonEntry>of() : entries
    )
      .stream()
      .map(e ->
        ComparisonEntry.builder()
          .id(e.getId())
          .name(e.getName())
          .metrics(toDashboardResponse(e.getMetrics()))
          .build()
      )
      .toList();
    return ComparisonResponse.builder()
      .entityType(entityType)
      .entries(mapped)
      .build();
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  public DashboardMetricsResponse toDashboardResponse(
    DashboardMetrics metrics
  ) {
    if (metrics == null) {
      return null;
    }
    return DashboardMetricsResponse.builder()
      .activeIncidents(metrics.getActiveIncidents())
      .closedIncidents(metrics.getClosedIncidents())
      .resolvedIncidents(metrics.getResolvedIncidents())
      .treatedIncidents(metrics.getTreatedIncidents())
      .rejectedIncidents(metrics.getRejectedIncidents())
      .cancelledIncidents(metrics.getCancelledIncidents())
      .blockedIncidents(metrics.getBlockedIncidents())
      .totalIncidents(metrics.getTotalIncidents())
      .openCount(metrics.getOpenCount())
      .pendingCount(metrics.getPendingCount())
      .openCountPreviousPeriod(metrics.getOpenCountPreviousPeriod())
      .pendingCountPreviousPeriod(metrics.getPendingCountPreviousPeriod())
      .resolvedCountPreviousPeriod(metrics.getResolvedCountPreviousPeriod())
      .period(metrics.getPeriod())
      .effectiveDateFrom(metrics.getEffectiveDateFrom())
      .effectiveDateTo(metrics.getEffectiveDateTo())
      .generatedAt(metrics.getGeneratedAt())
      .transferRate(metrics.getTransferRate())
      .transferCount(metrics.getTransferCount())
      .transferDenominator(metrics.getTransferDenominator())
      .slaComplianceRate(metrics.getSlaComplianceRate())
      .slaCompliantCount(metrics.getSlaCompliantCount())
      .slaDenominator(metrics.getSlaDenominator())
      .resolutionReopenRate(metrics.getResolutionReopenRate())
      .reopenedCount(metrics.getReopenedCount())
      .reopenDenominator(metrics.getReopenDenominator())
      .avgTimeToFirstResponse(metrics.getAvgTimeToFirstResponse())
      .inflow(metrics.getInflow())
      .outflow(metrics.getOutflow())
      .exitsClosed(metrics.getExitsClosed())
      .exitsRejected(metrics.getExitsRejected())
      .exitsCancelled(metrics.getExitsCancelled())
      .backlogReEntries(metrics.getBacklogReEntries())
      .netBacklog(metrics.getNetBacklog())
      .ageDistribution(metrics.getAgeDistribution())
      .slaBreachNow(metrics.getSlaBreachNow())
      .cohortOutcome(metrics.getCohortOutcome())
      .workload(toNamedIncidentCountResponseList(metrics.getWorkload()))
      .efficiencyScorecard(
        toEfficiencyScorecardResponse(metrics.getEfficiencyScorecard())
      )
      .cohortCompletion(
        toCohortCompletionResponse(metrics.getCohortCompletion())
      )
      .avgClosureHours(metrics.getAvgClosureHours())
      .medianClosureHours(metrics.getMedianClosureHours())
      .p90ClosureHours(metrics.getP90ClosureHours())
      .closureSampleSize(metrics.getClosureSampleSize())
      .avgNetClosureHours(metrics.getAvgNetClosureHours())
      .medianNetClosureHours(metrics.getMedianNetClosureHours())
      .avgResolutionHours(metrics.getAvgResolutionHours())
      .medianResolutionHours(metrics.getMedianResolutionHours())
      .p90ResolutionHours(metrics.getP90ResolutionHours())
      .resolutionSampleSize(metrics.getResolutionSampleSize())
      .transferredByMe(metrics.getTransferredByMe())
      .closedByMe(metrics.getClosedByMe())
      .createdByMe(metrics.getCreatedByMe())
      .resolvedByMe(metrics.getResolvedByMe())
      .distributionByType(metrics.getDistributionByType())
      .distributionByCriticality(metrics.getDistributionByCriticality())
      .distributionByStatus(metrics.getDistributionByStatus())
      .recentActivities(
        metrics.getRecentActivities() == null
          ? List.of()
          : toHistoryResponseList(metrics.getRecentActivities())
      )
      .assignedToMe(metrics.getAssignedToMe())
      .topServices(toServiceIncidentCountResponseList(metrics.getTopServices()))
      .topAgencies(toNamedIncidentCountResponseList(metrics.getTopAgencies()))
      .topResolvers(toNamedIncidentCountResponseList(metrics.getTopResolvers()))
      .monthlyClosures(
        toMonthlyMetricResponseList(metrics.getMonthlyClosures())
      )
      .monthlyAvgClosureHours(
        toMonthlyMetricResponseList(metrics.getMonthlyAvgClosureHours())
      )
      .closureHoursByType(
        toNamedDurationMetricResponseList(metrics.getClosureHoursByType())
      )
      .closureHoursByCriticality(
        toNamedDurationMetricResponseList(
          metrics.getClosureHoursByCriticality()
        )
      )
      .build();
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  private CohortCompletionResponse toCohortCompletionResponse(
    CohortCompletion c
  ) {
    if (c == null) {
      return null;
    }
    return CohortCompletionResponse.builder()
      .size(c.getSize())
      .closedCount(c.getClosedCount())
      .p50Hours(c.getP50Hours())
      .p50Reached(c.isP50Reached())
      .p90Hours(c.getP90Hours())
      .p90Reached(c.isP90Reached())
      .openMedianAgeHours(c.getOpenMedianAgeHours())
      .maxElapsedHours(c.getMaxElapsedHours())
      .build();
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  private EfficiencyScorecardResponse toEfficiencyScorecardResponse(
    EfficiencyScorecard s
  ) {
    if (s == null) {
      return null;
    }
    return EfficiencyScorecardResponse.builder()
      .delayScore(s.getDelayScore())
      .qualityScore(s.getQualityScore())
      .throughputScore(s.getThroughputScore())
      .compositeScore(s.getCompositeScore())
      .build();
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  private List<MonthlyMetricResponse> toMonthlyMetricResponseList(
    List<MonthlyMetric> items
  ) {
    if (items == null) {
      return List.of();
    }
    return items
      .stream()
      .map(m -> new MonthlyMetricResponse(m.getMonth(), m.getValue()))
      .toList();
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  private List<NamedDurationMetricResponse> toNamedDurationMetricResponseList(
    List<NamedDurationMetric> items
  ) {
    if (items == null) {
      return List.of();
    }
    return items
      .stream()
      .map(d ->
        new NamedDurationMetricResponse(
          d.getName(),
          d.getAvgHours(),
          d.getSampleSize()
        )
      )
      .toList();
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  private List<NamedIncidentCountResponse> toNamedIncidentCountResponseList(
    List<NamedIncidentCount> counts
  ) {
    if (counts == null) {
      return List.of();
    }
    return counts
      .stream()
      .map(count ->
        new NamedIncidentCountResponse(count.getName(), count.getCount())
      )
      .toList();
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  private List<ServiceIncidentCountResponse> toServiceIncidentCountResponseList(
    List<ServiceIncidentCount> counts
  ) {
    if (counts == null) {
      return List.of();
    }
    return counts
      .stream()
      .map(count ->
        new ServiceIncidentCountResponse(
          count.getServiceName(),
          count.getCount()
        )
      )
      .toList();
  }

  // Precise l'intention metier associee a incident.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "validatedBy", ignore = true)
  @Mapping(target = "assignedTo", ignore = true)
  @Mapping(target = "transferredToService", ignore = true)
  @Mapping(target = "transferReason", ignore = true)
  @Mapping(target = "rejectReason", ignore = true)
  @Mapping(target = "agencyId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "validatedAt", ignore = true)
  @Mapping(target = "transferredAt", ignore = true)
  @Mapping(target = "resolvedAt", ignore = true)
  @Mapping(target = "closedAt", ignore = true)
  @Mapping(target = "history", ignore = true)
  @Mapping(target = "comments", ignore = true)
  @Mapping(target = "typeId", source = "typeId")
  public abstract Incident toEntity(IncidentRequest request);

  // Applique le changement demande apres validation metier.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "validatedBy", ignore = true)
  @Mapping(target = "assignedTo", ignore = true)
  @Mapping(target = "transferredToService", ignore = true)
  @Mapping(target = "transferReason", ignore = true)
  @Mapping(target = "rejectReason", ignore = true)
  @Mapping(target = "agencyId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "validatedAt", ignore = true)
  @Mapping(target = "transferredAt", ignore = true)
  @Mapping(target = "resolvedAt", ignore = true)
  @Mapping(target = "closedAt", ignore = true)
  @Mapping(target = "history", ignore = true)
  @Mapping(target = "comments", ignore = true)
  @Mapping(target = "typeId", source = "typeId")
  public abstract void updateFromRequest(
    IncidentUpdateRequest request,
    @MappingTarget Incident entity
  );

  // Realise l'intention metier uuid to type config reponse.

  @Named("uuidToTypeConfigResponse")
  protected IncidentTypeConfigResponse uuidToTypeConfigResponse(UUID id) {
    if (id == null) return null;
    Map<UUID, IncidentTypeConfigResponse> cachedTypes = incidentTypeCache.get();
    if (cachedTypes != null && cachedTypes.containsKey(id)) {
      return cachedTypes.get(id);
    }
    return incidentTypeConfigRepository
      .findById(id)
      .map(incidentTypeConfigMapper::toResponse)
      .orElse(null);
  }

  // Realise l'intention metier uuid to user summary.

  @Named("uuidToUserSummary")
  protected UserSummaryResponse uuidToUserSummary(UUID id) {
    return userClientService.resolveUser(id);
  }

  // Realise l'intention metier uuid to agency summary.

  @Named("uuidToAgencySummary")
  protected AgencySummaryResponse uuidToAgencySummary(UUID id) {
    if (id == null) return null;
    return userClientService.resolveAgency(id);
  }

  // Realise l'intention metier uuid to service summary.

  @Named("uuidToServiceSummary")
  protected ServiceSummaryResponse uuidToServiceSummary(UUID id) {
    return userClientService.resolveService(id);
  }

  // Realise l'intention metier history ensemble to liste.

  @Named("historySetToList")
  protected List<IncidentHistoryResponse> historySetToList(
    Set<IncidentHistory> history
  ) {
    if (history == null) return new ArrayList<>();
    return history.stream().map(this::mapHistoryToResponse).toList();
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  public List<IncidentHistoryResponse> toHistoryResponseList(
    List<IncidentHistory> history
  ) {
    if (history == null) return new ArrayList<>();
    return history.stream().map(this::mapHistoryToResponse).toList();
  }

  // Realise l'intention metier comment ensemble to liste.

  @Named("commentSetToList")
  protected List<IncidentCommentResponse> commentSetToList(
    Set<IncidentComment> comments
  ) {
    if (comments == null) return new ArrayList<>();
    return comments.stream().map(this::mapCommentToResponse).toList();
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  private IncidentHistoryResponse mapHistoryToResponse(IncidentHistory h) {
    IncidentHistoryResponse r = new IncidentHistoryResponse();
    r.setId(h.getId());
    r.setCreatedAt(h.getCreatedAt());
    r.setUpdatedAt(h.getUpdatedAt());
    r.setModifiedBy(h.getModifiedBy());
    r.setAction(h.getAction());
    r.setOldValue(h.getOldValue());
    r.setNewValue(h.getNewValue());
    r.setComment(h.getComment());
    r.setUser(uuidToUserSummary(h.getUserId()));

    if (h.getIncident() != null) {
      r.setIncidentId(h.getIncident().getId());
      r.setIncidentTitle(h.getIncident().getTitle());
    }

    return r;
  }

  // Convertit les donnees du domaine incident entre les modeles utilises.

  private IncidentCommentResponse mapCommentToResponse(IncidentComment c) {
    IncidentCommentResponse r = new IncidentCommentResponse();
    r.setId(c.getId());
    r.setCreatedAt(c.getCreatedAt());
    r.setUpdatedAt(c.getUpdatedAt());
    r.setModifiedBy(c.getModifiedBy());
    r.setContent(c.getContent());
    r.setInternal(c.isInternal());
    r.setAuthor(uuidToUserSummary(c.getUserId()));
    if (c.getParentComment() != null) {
      r.setParentCommentId(c.getParentComment().getId());
      r.setParentAuthor(uuidToUserSummary(c.getParentComment().getUserId()));
    }
    return r;
  }
}
