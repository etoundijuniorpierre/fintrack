// Constantes metier : centralise les valeurs stables liees a incident cause.

package com.fintrack.incident.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Cause principale de l'incident.
 * La précision de la cause est stockée dans le champ causeDetail de l'entité Incident.
 */
@Getter
@RequiredArgsConstructor
// Modelise la responsabilite applicative liee a incident.
public enum IncidentCause implements LocalizableEnum {
  HUMAN(
    "HUMAN",
    "enum.incident_cause.HUMAN.name",
    "enum.incident_cause.HUMAN.description"
  ),
  TECHNICAL(
    "TECHNICAL",
    "enum.incident_cause.TECHNICAL.name",
    "enum.incident_cause.TECHNICAL.description"
  ),
  ORGANIZATIONAL(
    "ORGANIZATIONAL",
    "enum.incident_cause.ORGANIZATIONAL.name",
    "enum.incident_cause.ORGANIZATIONAL.description"
  ),
  ENVIRONMENTAL(
    "ENVIRONMENTAL",
    "enum.incident_cause.ENVIRONMENTAL.name",
    "enum.incident_cause.ENVIRONMENTAL.description"
  ),
  EXTERNAL(
    "EXTERNAL",
    "enum.incident_cause.EXTERNAL.name",
    "enum.incident_cause.EXTERNAL.description"
  ),
  STRUCTURAL(
    "STRUCTURAL",
    "enum.incident_cause.STRUCTURAL.name",
    "enum.incident_cause.STRUCTURAL.description"
  ),
  OTHER(
    "OTHER",
    "enum.incident_cause.OTHER.name",
    "enum.incident_cause.OTHER.description"
  );

  /** Identifiant technique stable (stocké en base, utilisé dans le code). */
  private final String name;
  /** Clé i18n pour le libellé affiché à l'utilisateur. */
  private final String nameKey;
  /** Clé i18n pour la description affichée à l'utilisateur. */
  private final String descriptionKey;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(IncidentCause::getName)
      .toArray(String[]::new);
  }
}
