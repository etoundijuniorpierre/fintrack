// Constantes metier : centralise les valeurs stables liees a user permission.

package com.fintrack.user.model.constant.user;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration des droits specifiques a la gestion des comptes utilisateurs.

@Getter
@RequiredArgsConstructor
public enum UserPermission {
  USER_CREATE_ALL_AGENT("USER_CREATE_ALL_AGENT", false),
  USER_CREATE_AGENT_AGENCY("USER_CREATE_AGENT_AGENCY", false),
  USER_CREATE_AGENT_SERVICE("USER_CREATE_AGENT_SERVICE", false),
  /** Permission historique conservee uniquement pour compatibilite de migration. */
  @Deprecated
  USER_CREATE_AGENT("USER_CREATE_AGENT", true),
  USER_CREATE_CHEF_AGENCE("USER_CREATE_CHEF_AGENCE", false),
  USER_CREATE_CHEF_SERVICE("USER_CREATE_CHEF_SERVICE", false),
  USER_CREATE_ADMIN("USER_CREATE_ADMIN", false),
  USER_UPDATE("USER_UPDATE", false),
  USER_UPDATE_AGENT("USER_UPDATE_AGENT", false),
  USER_DELETE("USER_DELETE", false),
  USER_VIEW_AGENCY("USER_VIEW_AGENCY", false),
  USER_VIEW_SERVICE("USER_VIEW_SERVICE", false),
  USER_VIEW_ALL("USER_VIEW_ALL", false),
  USER_MANAGE_PROFILE("USER_MANAGE_PROFILE", false);

  private final String name;
  private final boolean legacy;

  // Indique qu'une constante est conservee uniquement le temps de migrer les anciennes donnees.
  public boolean isLegacy() {
    return legacy;
  }

  // Fournit all names a la couche appelante.
  public static String[] getAllNames() {
    return Arrays.stream(values())
      .filter(permission -> !permission.isLegacy())
      .map(UserPermission::getName)
      .toArray(String[]::new);
  }
}
