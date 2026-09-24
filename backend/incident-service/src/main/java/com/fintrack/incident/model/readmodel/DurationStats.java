// Composant backend : porte la logique liee a duration stats.

package com.fintrack.incident.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Statistiques d'une famille de delai, agregees en base en une passe.
// L'effectif accompagne toujours les valeurs : une moyenne sur trois incidents
// ne se lit pas comme une moyenne sur trois mille.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DurationStats {

  private double avgHours;
  private double p50Hours;
  private double p90Hours;
  private long sampleSize;
  // Horloge arretee deduite ; vaut le brut pour les familles sans pause.
  private double avgNetHours;
  private double p50NetHours;
}
