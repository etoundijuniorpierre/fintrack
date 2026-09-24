// Composant backend : porte la logique liee a super admin snapshot.

package com.fintrack.reporting.model.readmodel.superadmin;

import com.fintrack.reporting.client.incident.dto.IncidentDashboardMetricsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AuditStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserStatsClientResponse;
import com.fintrack.reporting.model.entity.GeneratedReport;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Snapshot interne des donnees Super Admin avant conversion en DTO de reponse.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminSnapshot {

  private IncidentDashboardMetricsClientResponse incidentMetrics;
  private List<?> incidents;
  private List<?> incidentTypeConfigs;
  private List<?> users;
  private UserStatsClientResponse userStats;
  private List<?> roles;
  private List<?> agencies;
  private List<?> services;
  private List<?> permissions;
  private NotificationStatsClientResponse notificationStats;
  private AuditStatsClientResponse auditStats;
  private List<GeneratedReport> reports;
  private SystemThresholds thresholds;
  private SystemThresholds appliedThresholds;
  private SuperAdminOverviewMeta overviewMeta;
  private SuperAdminSectionMetadata metadata;
  private Map<String, HealthProbe> healthProbes;
}
