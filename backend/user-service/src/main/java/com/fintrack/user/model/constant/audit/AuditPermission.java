// Constantes metier : centralise les valeurs stables liees a audit permission.

package com.fintrack.user.model.constant.audit;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Definition des permissions liees a la consultation des audits.

@Getter
@RequiredArgsConstructor
public enum AuditPermission {
  AUDIT_VIEW("AUDIT_VIEW");

  private final String name;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(AuditPermission::getName)
      .toArray(String[]::new);
  }
}
