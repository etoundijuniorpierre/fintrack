// Constantes metier : centralise les valeurs stables liees a resolution cycle outcome.

package com.fintrack.incident.model.constant;

// Issue d'un cycle de traitement. Interne aux metriques : jamais rendue a l'utilisateur.
public enum ResolutionCycleOutcome {
  OPEN,
  CLOSED,
  REJECTED,
  CANCELLED,
  REOPENED,
}
