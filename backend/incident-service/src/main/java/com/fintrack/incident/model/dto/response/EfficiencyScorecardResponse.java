// DTO : transporte les donnees liees a efficiency scorecard entre les couches.

package com.fintrack.incident.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Fragment wire du tableau de bord : scorecard d'efficacite (mappe depuis le read-model).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EfficiencyScorecardResponse {

  private double delayScore;
  private double qualityScore;
  private double throughputScore;
  private double compositeScore;
}
