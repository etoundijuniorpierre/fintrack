// Composant backend : porte la logique liee a cohort completion.

package com.fintrack.incident.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Temps de completion de la cohorte creee sur la periode.
// Les familles de delai ne mesurent que les incidents clotures : les plus longs,
// encore ouverts, n'y entrent jamais, et solder un arriere fait monter la mediane.
// Ici la cohorte est suivie en entier ; un percentile n'est annonce que si assez
// d'incidents sont clotures pour l'atteindre, sinon il est declare non atteint.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortCompletion {

  // Incidents crees sur la periode, hors rejetes et annules : ceux-ci ne peuvent
  // pas se cloturer et fausseraient le denominateur.
  private long size;
  private long closedCount;
  private double p50Hours;
  private boolean p50Reached;
  private double p90Hours;
  private boolean p90Reached;
  // Anciennete mediane des encore-ouverts : la part censuree, rendue visible.
  private double openMedianAgeHours;
  // Borne basse quand un percentile n'est pas atteint.
  private double maxElapsedHours;
}
