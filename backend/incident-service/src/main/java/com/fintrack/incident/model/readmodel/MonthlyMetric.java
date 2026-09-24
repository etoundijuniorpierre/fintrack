// Composant backend : porte la logique liee a monthly metric.

package com.fintrack.incident.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a monthly metric.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyMetric {

  private int month;
  private double value;
}
