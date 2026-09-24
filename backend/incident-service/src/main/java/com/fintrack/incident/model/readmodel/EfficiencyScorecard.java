// Composant backend : porte la logique liee a efficiency scorecard.

package com.fintrack.incident.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a efficiency scorecard.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EfficiencyScorecard {

  private double delayScore;
  private double qualityScore;
  private double throughputScore;
  private double compositeScore;
}
