// DTO : transporte les donnees liees a monthly metric entre les couches.

package com.fintrack.incident.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Fragment wire du tableau de bord : bucket mensuel (mappe depuis le read-model).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyMetricResponse {

  private int month;
  private double value;
}
