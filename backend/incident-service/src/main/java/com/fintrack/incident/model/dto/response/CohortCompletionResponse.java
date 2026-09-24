// DTO : transporte les donnees liees a cohort completion entre les couches.

package com.fintrack.incident.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse portant le temps de completion de la cohorte creee.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortCompletionResponse {

  private long size;
  private long closedCount;
  private double p50Hours;
  private boolean p50Reached;
  private double p90Hours;
  private boolean p90Reached;
  private double openMedianAgeHours;
  private double maxElapsedHours;
}
