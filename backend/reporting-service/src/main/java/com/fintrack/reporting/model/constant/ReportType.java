// Constantes metier : centralise les valeurs stables liees a report type.

package com.fintrack.reporting.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration des types de rapport selon leur periodicite
@Getter
@RequiredArgsConstructor
public enum ReportType implements LocalizableEnum {
  DAILY(
    "DAILY",
    "enum.report_type.DAILY.name",
    "enum.report_type.DAILY.description"
  ),
  WEEKLY(
    "WEEKLY",
    "enum.report_type.WEEKLY.name",
    "enum.report_type.WEEKLY.description"
  ),
  MONTHLY(
    "MONTHLY",
    "enum.report_type.MONTHLY.name",
    "enum.report_type.MONTHLY.description"
  ),
  CUSTOM(
    "CUSTOM",
    "enum.report_type.CUSTOM.name",
    "enum.report_type.CUSTOM.description"
  );

  /** Identifiant technique stable (stocké en base, utilisé dans le code). */
  private final String name;
  /** Clé i18n pour le libellé affiché à l'utilisateur. */
  private final String nameKey;
  /** Clé i18n pour la description affichée à l'utilisateur. */
  private final String descriptionKey;

  // Retourne les identifiants techniques de tous les types
  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(ReportType::getName)
      .toArray(String[]::new);
  }
}
