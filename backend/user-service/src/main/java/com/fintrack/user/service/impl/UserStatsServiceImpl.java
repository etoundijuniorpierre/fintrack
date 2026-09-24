// Service metier : coordonne les operations du domaine utilisateur statistiques.

package com.fintrack.user.service.impl;

import com.fintrack.user.client.notification.NotificationClientService;
import com.fintrack.user.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.user.exception.BusinessRuleViolationException;
import com.fintrack.user.exception.ErrorCode;
import com.fintrack.user.model.constant.PeriodType;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.readmodel.DateRange;
import com.fintrack.user.model.readmodel.UserStats;
import com.fintrack.user.model.readmodel.UserStatsAggregate;
import com.fintrack.user.repository.UserRepository;
import com.fintrack.user.security.UserDetailsImpl;
import com.fintrack.user.service.UserStatsService;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementation du service d'analyse de l'activite des utilisateurs.

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

  private final UserRepository userRepository;
  private final MessageSource messageSource;
  private final NotificationClientService notificationClientService;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;

  private static final String NO_ONLINE_USER_SENTINEL =
    "__fintrack_no_online_user__";

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  // Fournit statistiques a la couche appelante.

  @Override
  @Transactional(readOnly = true)
  public UserStats getStats(
    PeriodType period,
    LocalDateTime dateFrom,
    LocalDateTime dateTo,
    String view,
    UUID filterAgencyId,
    UUID filterServiceId,
    UserDetailsImpl currentUser
  ) {
    PeriodType effectivePeriod = period != null ? period : PeriodType.ALL;
    validateCustomPeriod(effectivePeriod, dateFrom, dateTo);

    DateRange range = computeDateRange(effectivePeriod, dateFrom, dateTo);
    LocalDateTime now = LocalDateTime.now();

    Specification<User> baseSpec = buildScopeSpecification(
      view,
      filterAgencyId,
      filterServiceId,
      currentUser
    );

    UserStatsAggregate aggregate = userRepository.getUserStatsAggregate(
      baseSpec,
      resolveOnlineUsernames(),
      now.minusHours(24),
      reportingSystemConfigClientService.getThresholdInt(
        "loginMaxFailedAttempts",
        5
      )
    );

    long newUsersInPeriod = computeNewUsersInPeriod(
      baseSpec,
      range,
      aggregate.getTotalCount()
    );

    return UserStats.builder()
      .totalCount(aggregate.getTotalCount())
      .activeCount(aggregate.getActiveCount())
      .inactiveCount(aggregate.getInactiveCount())
      .connectedCount(aggregate.getConnectedCount())
      .neverConnectedCount(aggregate.getNeverConnectedCount())
      .lockedCount(aggregate.getLockedCount())
      .firstLoginPendingCount(aggregate.getFirstLoginPendingCount())
      .recentlyActiveCount(aggregate.getRecentlyActiveCount())
      .newUsersInPeriod(newUsersInPeriod)
      .build();
  }

  // Fournit les sessions WebSocket actives dans un format compatible avec l'agregation.
  private Set<String> resolveOnlineUsernames() {
    Set<String> onlineUsernames = notificationClientService.getOnlineUsernames();
    return onlineUsernames == null || onlineUsernames.isEmpty()
      ? Set.of(NO_ONLINE_USER_SENTINEL)
      : onlineUsernames;
  }

  // Construit la representation attendue pour le domaine utilisateur statistiques.

  private Specification<User> buildScopeSpecification(
    String view,
    UUID filterAgencyId,
    UUID filterServiceId,
    UserDetailsImpl currentUser
  ) {
    return (root, query, cb) -> {
      if ("all".equalsIgnoreCase(view)) {
        if (filterServiceId != null) {
          return cb.equal(root.get("service").get("id"), filterServiceId);
        } else if (filterAgencyId != null) {
          return cb.equal(root.get("agency").get("id"), filterAgencyId);
        }
        return cb.conjunction();
      } else if ("agency".equalsIgnoreCase(view)) {
        return cb.equal(
          root.get("agency").get("id"),
          currentUser.getAgencyId()
        );
      } else if ("service".equalsIgnoreCase(view)) {
        return cb.equal(
          root.get("service").get("id"),
          currentUser.getServiceId()
        );
      }
      return cb.conjunction();
    };
  }

  // Prepare l'enregistrement de la ressource selon les regles metier.

  private Specification<User> createdAtBetween(
    LocalDateTime from,
    LocalDateTime to
  ) {
    return (root, query, cb) -> cb.between(root.get("createdAt"), from, to);
  }

  // Verifie que les regles metier autorisent l operation sur service interne.

  private void validateCustomPeriod(
    PeriodType periodType,
    LocalDateTime dateFrom,
    LocalDateTime dateTo
  ) {
    if (periodType != PeriodType.CUSTOM) {
      return;
    }
    if (dateFrom == null || dateTo == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.STATS_CUSTOM_PERIOD_DATES_REQUIRED,
        t("user.error.stats_custom_period_dates_required")
      );
    }
    if (dateFrom.isAfter(dateTo)) {
      throw new BusinessRuleViolationException(
        ErrorCode.STATS_CUSTOM_PERIOD_DATE_ORDER,
        t("user.error.stats_custom_period_date_order")
      );
    }
  }

  // Calcule date range.

  private DateRange computeDateRange(
    PeriodType periodType,
    LocalDateTime dateFrom,
    LocalDateTime dateTo
  ) {
    LocalDateTime now = LocalDateTime.now();

    return switch (periodType) {
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
      case CUSTOM -> new DateRange(dateFrom, dateTo);
      case ALL -> null;
    };
  }

  // Calcule new users in period.

  private long computeNewUsersInPeriod(
    Specification<User> baseSpec,
    DateRange range,
    long totalCount
  ) {
    if (range == null) return totalCount;
    return userRepository.count(
      baseSpec.and(createdAtBetween(range.getFrom(), range.getTo()))
    );
  }
}
