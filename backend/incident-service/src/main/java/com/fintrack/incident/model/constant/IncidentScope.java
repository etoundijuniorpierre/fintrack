// Constantes metier : centralise les valeurs stables liees a incident scope.

package com.fintrack.incident.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration definissant les portees d'un incident.

@Getter
@RequiredArgsConstructor
public enum IncidentScope implements LocalizableEnum {
  OWN(
    "OWN",
    "enum.incident_scope.OWN.name",
    "enum.incident_scope.OWN.description"
  ),
  SERVICE(
    "SERVICE",
    "enum.incident_scope.SERVICE.name",
    "enum.incident_scope.SERVICE.description"
  ),
  AGENCY(
    "AGENCY",
    "enum.incident_scope.AGENCY.name",
    "enum.incident_scope.AGENCY.description"
  ),
  ALL(
    "ALL",
    "enum.incident_scope.ALL.name",
    "enum.incident_scope.ALL.description"
  );

  private final String name;
  private final String nameKey;
  private final String descriptionKey;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(IncidentScope::getName)
      .toArray(String[]::new);
  }
}
