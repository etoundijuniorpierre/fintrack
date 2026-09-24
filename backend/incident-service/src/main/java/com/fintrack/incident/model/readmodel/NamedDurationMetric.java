// Composant backend : porte la logique liee a named duration metric.

package com.fintrack.incident.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Delai moyen d'une categorie (type, criticite), avec l'effectif qui le sous-tend.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedDurationMetric {

  private String name;
  private double avgHours;
  private long sampleSize;
}
