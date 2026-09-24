// Constantes metier : centralise les valeurs stables liees a incident validator scope.

package com.fintrack.incident.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration definissant qui valide un incident selon le type :
//  - SOURCE_SERVICE_MANAGER : validation par le chef du service du createur (defaut).
//                             Le createur sans service, ou un service sans chef, retombe
//                             sur le chef d'agence. Le cas createur chef du service de traitement
//                             configure sur le type depend du reglage Super Admin.
//  - AGENCY_MANAGER         : validation reservee au chef d'agence.
//  - TARGET_SERVICE_MANAGER : validation reservee au responsable du service cible.
//  - ADMIN                  : validation reservee a un administrateur (aucun autre acteur).

@Getter
@RequiredArgsConstructor
public enum IncidentValidatorScope implements LocalizableEnum {
  SOURCE_SERVICE_MANAGER(
    "SOURCE_SERVICE_MANAGER",
    "enum.incident_validator_scope.SOURCE_SERVICE_MANAGER.name",
    "enum.incident_validator_scope.SOURCE_SERVICE_MANAGER.description"
  ),
  AGENCY_MANAGER(
    "AGENCY_MANAGER",
    "enum.incident_validator_scope.AGENCY_MANAGER.name",
    "enum.incident_validator_scope.AGENCY_MANAGER.description"
  ),
  TARGET_SERVICE_MANAGER(
    "TARGET_SERVICE_MANAGER",
    "enum.incident_validator_scope.TARGET_SERVICE_MANAGER.name",
    "enum.incident_validator_scope.TARGET_SERVICE_MANAGER.description"
  ),
  ADMIN(
    "ADMIN",
    "enum.incident_validator_scope.ADMIN.name",
    "enum.incident_validator_scope.ADMIN.description"
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
      .map(IncidentValidatorScope::getName)
      .toArray(String[]::new);
  }
}
