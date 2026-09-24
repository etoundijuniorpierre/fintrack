// Constantes metier : centralise les valeurs stables liees a criticality.

package com.fintrack.incident.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration definissant les niveaux de criticite des incidents.

@Getter
@RequiredArgsConstructor
public enum Criticality implements LocalizableEnum {
  LOW("LOW", "enum.criticality.LOW.name", "enum.criticality.LOW.description"),
  MEDIUM(
    "MEDIUM",
    "enum.criticality.MEDIUM.name",
    "enum.criticality.MEDIUM.description"
  ),
  HIGH(
    "HIGH",
    "enum.criticality.HIGH.name",
    "enum.criticality.HIGH.description"
  ),
  CRITICAL(
    "CRITICAL",
    "enum.criticality.CRITICAL.name",
    "enum.criticality.CRITICAL.description"
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
      .map(Criticality::getName)
      .toArray(String[]::new);
  }
}
