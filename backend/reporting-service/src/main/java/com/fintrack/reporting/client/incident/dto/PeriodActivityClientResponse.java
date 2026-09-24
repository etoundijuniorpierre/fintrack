// DTO : transporte les listes de flux (traites/resolus/clotures) d'une periode.

package com.fintrack.reporting.client.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les incidents adossant chaque compteur de flux de la periode.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeriodActivityClientResponse {

  private List<IncidentClientResponse> treated;
  private List<IncidentClientResponse> resolved;
  private List<IncidentClientResponse> closed;
}
