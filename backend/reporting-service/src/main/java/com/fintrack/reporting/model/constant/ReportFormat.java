// Constantes metier : centralise les valeurs stables liees a report format.

package com.fintrack.reporting.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration des formats de sortie possibles pour un rapport
@Getter
@RequiredArgsConstructor
public enum ReportFormat implements LocalizableEnum {
  PDF(
    "PDF",
    "enum.report_format.PDF.name",
    "enum.report_format.PDF.description"
  ),
  EXCEL(
    "EXCEL",
    "enum.report_format.EXCEL.name",
    "enum.report_format.EXCEL.description"
  ),
  JSON(
    "JSON",
    "enum.report_format.JSON.name",
    "enum.report_format.JSON.description"
  );

  /** Identifiant technique stable (stocké en base, utilisé dans le code). */
  private final String name;
  /** Clé i18n pour le libellé affiché à l'utilisateur. */
  private final String nameKey;
  /** Clé i18n pour la description affichée à l'utilisateur. */
  private final String descriptionKey;

  // Retourne les identifiants techniques de tous les formats
  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(ReportFormat::getName)
      .toArray(String[]::new);
  }
}
