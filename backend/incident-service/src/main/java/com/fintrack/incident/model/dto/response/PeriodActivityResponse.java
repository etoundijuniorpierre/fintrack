// DTO : transporte les incidents de flux d'une periode (traites/resolus/clotures).

package com.fintrack.incident.model.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse listant, pour une periode, les incidents adossant chaque compteur de flux.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodActivityResponse {

  private List<IncidentSummaryResponse> treated;
  private List<IncidentSummaryResponse> resolved;
  private List<IncidentSummaryResponse> closed;
}
