// Constantes metier : centralise les valeurs stables liees a period type.

package com.fintrack.user.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration definissant les types de periodes pour les statistiques.

@Getter
@RequiredArgsConstructor
public enum PeriodType {
  TODAY(
    "TODAY",
    "enum.period_type.TODAY.name",
    "enum.period_type.TODAY.description"
  ),
  LAST_7_DAYS(
    "LAST_7_DAYS",
    "enum.period_type.LAST_7_DAYS.name",
    "enum.period_type.LAST_7_DAYS.description"
  ),
  LAST_30_DAYS(
    "LAST_30_DAYS",
    "enum.period_type.LAST_30_DAYS.name",
    "enum.period_type.LAST_30_DAYS.description"
  ),
  THIS_MONTH(
    "THIS_MONTH",
    "enum.period_type.THIS_MONTH.name",
    "enum.period_type.THIS_MONTH.description"
  ),
  LAST_365_DAYS(
    "LAST_365_DAYS",
    "enum.period_type.LAST_365_DAYS.name",
    "enum.period_type.LAST_365_DAYS.description"
  ),
  LAST_MONTH(
    "LAST_MONTH",
    "enum.period_type.LAST_MONTH.name",
    "enum.period_type.LAST_MONTH.description"
  ),
  LAST_2_MONTHS(
    "LAST_2_MONTHS",
    "enum.period_type.LAST_2_MONTHS.name",
    "enum.period_type.LAST_2_MONTHS.description"
  ),
  LAST_6_MONTHS(
    "LAST_6_MONTHS",
    "enum.period_type.LAST_6_MONTHS.name",
    "enum.period_type.LAST_6_MONTHS.description"
  ),
  CUSTOM(
    "CUSTOM",
    "enum.period_type.CUSTOM.name",
    "enum.period_type.CUSTOM.description"
  ),
  ALL("ALL", "enum.period_type.ALL.name", "enum.period_type.ALL.description");

  private final String name;
  private final String nameKey;
  private final String descriptionKey;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(PeriodType::getName)
      .toArray(String[]::new);
  }
}
