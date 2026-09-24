// Constantes metier : centralise les valeurs stables liees a incident validator role.

package com.fintrack.incident.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration decrivant le valideur REELLEMENT attendu sur un incident, une fois la
// portee configuree (IncidentValidatorScope) resolue sur l'incident concret :
//  - SERVICE_MANAGER : le chef d'un service precis (source ou cible selon la portee).
//  - AGENCY_MANAGER  : le responsable de l'agence de l'incident (portee dediee ou repli).
//  - ADMIN           : un administrateur, sans autre acteur possible.

@Getter
@RequiredArgsConstructor
public enum IncidentValidatorRole implements LocalizableEnum {
  SERVICE_MANAGER(
    "SERVICE_MANAGER",
    "enum.incident_validator_role.SERVICE_MANAGER.name",
    "enum.incident_validator_role.SERVICE_MANAGER.description"
  ),
  AGENCY_MANAGER(
    "AGENCY_MANAGER",
    "enum.incident_validator_role.AGENCY_MANAGER.name",
    "enum.incident_validator_role.AGENCY_MANAGER.description"
  ),
  ADMIN(
    "ADMIN",
    "enum.incident_validator_role.ADMIN.name",
    "enum.incident_validator_role.ADMIN.description"
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
      .map(IncidentValidatorRole::getName)
      .toArray(String[]::new);
  }
}
