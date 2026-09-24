// Mapper : convertit les donnees liees a governance builder entre modeles.

package com.fintrack.reporting.model.mapper.superadmin;

import static com.fintrack.reporting.model.mapper.superadmin.SuperAdminMaps.rate;

import com.fintrack.reporting.client.incident.dto.IncidentClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentDashboardMetricsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AgencyClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.ServiceClientResponse;
import com.fintrack.reporting.client.superadmin.dto.SuperAdminUserClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserStatsClientResponse;
import com.fintrack.reporting.model.dto.response.superadmin.DataQualityIssueResponse;
import com.fintrack.reporting.model.dto.response.superadmin.DataQualityOverviewResponse;
import com.fintrack.reporting.model.dto.response.superadmin.GovernanceSectionResponse;
import com.fintrack.reporting.model.dto.response.superadmin.SuperAdminKpisResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// Assure les conversions du domaine governance builder.

public final class GovernanceBuilder {

  private static final Set<String> STATUSES_REQUIRING_ASSIGNEE = Set.of(
    "VALIDATED",
    "TRANSFERRED",
    "ASSIGNED",
    "IN_PROGRESS",
    "BLOCKED",
    "REOPENED",
    "UNRESOLVED_PROLONGED_WAIT"
  );

  // Initialise le mapper avec ses dependances de construction.

  private GovernanceBuilder() {}

  // Construit la representation de rapport attendue par le cas d'usage.
  public static GovernanceSectionResponse build(
    IncidentDashboardMetricsClientResponse incidentMetrics,
    UserStatsClientResponse userStats,
    List<IncidentClientResponse> incidents,
    List<SuperAdminUserClientResponse> users,
    List<AgencyClientResponse> agencies,
    List<ServiceClientResponse> services,
    NotificationStatsClientResponse notificationStats,
    DataQualityOverviewResponse dataQuality
  ) {
    if (incidentMetrics == null) {
      incidentMetrics = new IncidentDashboardMetricsClientResponse(
        0L,
        0L,
        0.0,
        0.0,
        0.0,
        List.of(),
        List.of()
      );
    }
    if (dataQuality == null) {
      dataQuality = DataQualityOverviewResponse.builder()
        .issues(List.of())
        .build();
    }

    long rejected = incidents
      .stream()
      .filter(inc -> "REJECTED".equalsIgnoreCase(inc.getStatus()))
      .count();
    long reopened = incidents
      .stream()
      .filter(inc -> "REOPENED".equalsIgnoreCase(inc.getStatus()))
      .count();
    long transferred = incidents
      .stream()
      .filter(inc -> "TRANSFERRED".equalsIgnoreCase(inc.getStatus()))
      .count();
    long totalIncidents = incidents.size();

    long activeUsersVal =
      userStats != null ? userStats.getActiveCount() : activeUserCount(users);
    long failedNotificationsVal =
      notificationStats != null ? notificationStats.getFailed() : 0L;
    List<DataQualityIssueResponse> visibleAnomalies =
      dataQuality.getIssues() != null ? dataQuality.getIssues() : List.of();

    Map<String, Long> byStatus = incidents
      .stream()
      .map(IncidentClientResponse::getStatus)
      .filter(Objects::nonNull)
      .collect(
        Collectors.groupingBy(
          Function.identity(),
          LinkedHashMap::new,
          Collectors.counting()
        )
      );

    Map<String, Long> byCriticality = incidents
      .stream()
      .map(IncidentClientResponse::getCriticality)
      .filter(Objects::nonNull)
      .collect(
        Collectors.groupingBy(
          Function.identity(),
          LinkedHashMap::new,
          Collectors.counting()
        )
      );

    long criticalAnomaliesCount = 0;
    if (dataQuality.getIssues() != null) {
      for (DataQualityIssueResponse issue : dataQuality.getIssues()) {
        if ("high".equalsIgnoreCase(issue.getSeverity())) {
          criticalAnomaliesCount += issue.getCount();
        }
      }
    }

    long lockedAccountsCount =
      userStats != null ? userStats.getLockedCount() : 0L;
    long unassignedIncidentsCount = incidents
      .stream()
      .filter(
        inc ->
          inc.getAssignedTo() == null &&
          inc.getStatus() != null &&
          STATUSES_REQUIRING_ASSIGNEE.contains(
            inc.getStatus().toUpperCase(Locale.ROOT)
          )
      )
      .count();

    SuperAdminKpisResponse kpis = new SuperAdminKpisResponse(
      criticalAnomaliesCount,
      failedNotificationsVal,
      lockedAccountsCount,
      unassignedIncidentsCount
    );

    return GovernanceSectionResponse.builder()
      .totalIncidents(totalIncidents)
      .activeIncidents(incidentMetrics.getActiveIncidents())
      .activeUsers(activeUsersVal)
      .agencies(agencies.size())
      .services(services.size())
      .avgClosureHours(incidentMetrics.getAvgClosureHours())
      .medianClosureHours(incidentMetrics.getMedianClosureHours())
      .p90ClosureHours(incidentMetrics.getP90ClosureHours())
      .byStatus(byStatus)
      .byCriticality(byCriticality)
      .topAgencies(
        incidentMetrics.getTopAgencies() != null
          ? incidentMetrics.getTopAgencies()
          : List.of()
      )
      .topServices(
        incidentMetrics.getTopServices() != null
          ? incidentMetrics.getTopServices()
          : List.of()
      )
      .rejectedRate(rate(rejected, totalIncidents))
      .reopenedRate(rate(reopened, totalIncidents))
      .transferredRate(rate(transferred, totalIncidents))
      .notificationsFailed(failedNotificationsVal)
      .visibleAnomalies(visibleAnomalies)
      .superAdminKpis(kpis)
      .build();
  }

  // Realise l'intention metier active user count.

  private static long activeUserCount(
    List<SuperAdminUserClientResponse> users
  ) {
    return users
      .stream()
      .filter(SuperAdminUserClientResponse::isActive)
      .count();
  }
}
