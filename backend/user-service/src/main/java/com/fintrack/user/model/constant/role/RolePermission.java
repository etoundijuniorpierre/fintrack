// Constantes metier : centralise les valeurs stables liees a role permission.

package com.fintrack.user.model.constant.role;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration des permissions associees a l'administration des roles.

@Getter
@RequiredArgsConstructor
public enum RolePermission {
  ROLE_CREATE("ROLE_CREATE"),
  ROLE_UPDATE("ROLE_UPDATE"),
  ROLE_DELETE("ROLE_DELETE"),
  ROLE_ASSIGN("ROLE_ASSIGN");

  private final String name;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(RolePermission::getName)
      .toArray(String[]::new);
  }
}
