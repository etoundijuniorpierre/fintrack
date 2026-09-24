// DTO : transporte les donnees liees a super admin overview entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de super-administration vue d ensemble.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminOverviewResponse {

  private GovernanceSectionResponse governance;
  private List<HealthRowResponse> systemHealth;
  private AuditSectionResponse audit;
  private PermissionsOverviewResponse permissions;
  private SystemThresholdsResponse systemConfig;
  private DataQualityOverviewResponse dataQuality;
  private ReportingSectionResponse reporting;
  private NotificationsOverviewResponse notifications;
  private SuperAdminOverviewMetaResponse meta;
}
