// Constantes metier : centralise les valeurs stables liees a report generation type.

package com.fintrack.reporting.model.constant;

// Distingue le mode de production d'un rapport : seuls les rapports MANUAL peuvent etre supprimes,
// les rapports AUTOMATIC sont conserves pour la tracabilite
public enum ReportGenerationType {
  MANUAL,
  AUTOMATIC,
}
