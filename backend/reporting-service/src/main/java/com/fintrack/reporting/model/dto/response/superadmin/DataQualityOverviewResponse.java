// DTO : transporte les donnees liees a data quality overview entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fintrack.reporting.client.superadmin.dto.NotificationClientResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de qualite des donnees vue d ensemble.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityOverviewResponse {

  private List<DataQualityIssueResponse> issues;
  private long totalIssues;
  private List<UnusedIncidentTypeResponse> unusedIncidentTypes;
  private List<SimpleEntityResponse> agenciesWithoutHead;
  private List<SimpleEntityResponse> servicesWithoutHead;
  private List<SimpleEntityResponse> reportsWithoutFile;
  private List<NotificationClientResponse> sentNotificationsWithoutTrace;
  private List<NotificationClientResponse> invalidRecipients;
  private List<SimpleEntityResponse> incidentsWithoutAgency;
  private List<SimpleEntityResponse> incidentsWithoutService;
  private List<SimpleEntityResponse> incidentsAssignedInactiveUser;
  private List<SimpleEntityResponse> usersWithoutRole;
  private List<SimpleEntityResponse> usersWithoutScope;
  private String checkedAt;
}
