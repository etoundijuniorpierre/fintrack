// Constantes metier : distingue le contenu d'un rapport de sa periodicite.

package com.fintrack.reporting.model.constant;

// Enumere les modeles de contenu proposes lors de la generation d'un rapport.
public enum ReportContentType {
  OPERATIONAL,
  INCIDENT_TYPE_ANALYSIS,
  INCIDENT_STATUS_OVERVIEW;

  // Retourne le modele historique lorsque la valeur persistee est absente.
  public static ReportContentType orDefault(ReportContentType value) {
    return value != null ? value : OPERATIONAL;
  }
}
