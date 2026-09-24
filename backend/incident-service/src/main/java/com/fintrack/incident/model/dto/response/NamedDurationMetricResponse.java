// DTO : transporte les donnees liees a named duration metric entre les couches.

package com.fintrack.incident.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse portant le delai moyen d'une categorie et son effectif.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedDurationMetricResponse {

  private String name;
  private double avgHours;
  private long sampleSize;
}
