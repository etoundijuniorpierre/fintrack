// DTO : transporte les donnees liees a incident dashboard metrics client entre les couches.

package com.fintrack.reporting.client.incident.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a inincident tableau de bord metrics client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDashboardMetricsClientResponse {

  private long totalIncidents;
  private long activeIncidents;
  private double avgClosureHours;
  private double medianClosureHours;
  private double p90ClosureHours;
  private List<NamedIncidentCountClientResponse> topAgencies;
  private List<ServiceIncidentCountClientResponse> topServices;
}
