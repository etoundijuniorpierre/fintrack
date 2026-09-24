// configurables du workflow (traitement, resolution, cloture, reouverture).

package com.fintrack.incident.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IncidentActorRole implements LocalizableEnum {
  SOURCE_AGENCY_MANAGER(
    "SOURCE_AGENCY_MANAGER",
    "enum.incident_actor_role.SOURCE_AGENCY_MANAGER.name",
    "enum.incident_actor_role.SOURCE_AGENCY_MANAGER.description"
  ),
  CREATOR(
    "CREATOR",
    "enum.incident_actor_role.CREATOR.name",
    "enum.incident_actor_role.CREATOR.description"
  ),
  ASSIGNEE(
    "ASSIGNEE",
    "enum.incident_actor_role.ASSIGNEE.name",
    "enum.incident_actor_role.ASSIGNEE.description"
  ),
  CHEF_SERVICE(
    "CHEF_SERVICE",
    "enum.incident_actor_role.CHEF_SERVICE.name",
    "enum.incident_actor_role.CHEF_SERVICE.description"
  );

  /** Identifiant technique stable (stocke en base, utilise dans le code). */
  private final String name;
  /** Cle i18n pour le libelle affiche a l'utilisateur. */
  private final String nameKey;
  /** Cle i18n pour la description affichee a l'utilisateur. */
  private final String descriptionKey;

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(IncidentActorRole::getName)
      .toArray(String[]::new);
  }

  public static IncidentActorRole fromName(String name) {
    return Arrays.stream(values())
      .filter(role -> role.name.equals(name))
      .findFirst()
      .orElse(null);
  }
}
