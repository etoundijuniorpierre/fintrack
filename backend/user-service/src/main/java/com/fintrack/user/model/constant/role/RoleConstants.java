// Constantes metier : centralise les valeurs stables liees a role constants.

package com.fintrack.user.model.constant.role;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Constantes definissant les roles systeme par defaut.

@Getter
@RequiredArgsConstructor
public enum RoleConstants {
  AGENT("AGENT", "enum.role.AGENT.name", "role.AGENT.description"),
  CHEF_AGENCE(
    "CHEF_AGENCE",
    "enum.role.CHEF_AGENCE.name",
    "role.CHEF_AGENCE.description"
  ),
  CHEF_SERVICE(
    "CHEF_SERVICE",
    "enum.role.CHEF_SERVICE.name",
    "role.CHEF_SERVICE.description"
  ),
  ADMIN("ADMIN", "enum.role.ADMIN.name", "role.ADMIN.description"),
  SUPER_ADMIN(
    "SUPER_ADMIN",
    "enum.role.SUPER_ADMIN.name",
    "role.SUPER_ADMIN.description"
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
      .map(RoleConstants::getName)
      .toArray(String[]::new);
  }
}
