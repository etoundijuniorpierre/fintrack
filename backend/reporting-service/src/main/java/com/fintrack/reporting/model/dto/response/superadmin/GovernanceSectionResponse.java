// DTO : transporte les donnees liees a governance section entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.reporting.client.incident.dto.NamedIncidentCountClientResponse;
import com.fintrack.reporting.client.incident.dto.ServiceIncidentCountClientResponse;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de gouvernance section.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GovernanceSectionResponse {

  private long totalIncidents;
  private long activeIncidents;
  private long activeUsers;
  private int agencies;
  private int services;
  private double avgClosureHours;
  private double medianClosureHours;
  private double p90ClosureHours;
  private Map<String, Long> byStatus;
  private Map<String, Long> byCriticality;
  private List<NamedIncidentCountClientResponse> topAgencies;
  private List<ServiceIncidentCountClientResponse> topServices;
  private double rejectedRate;
  private double reopenedRate;
  private double transferredRate;
  private long notificationsFailed;
  private List<DataQualityIssueResponse> visibleAnomalies;
  private SuperAdminKpisResponse superAdminKpis;

  @JsonProperty("_meta")
  private SectionMetadataResponse metadata;
}
