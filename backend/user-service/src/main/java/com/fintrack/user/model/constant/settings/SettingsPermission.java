// Constantes metier : centralise les valeurs stables liees a settings permission.

package com.fintrack.user.model.constant.settings;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Definition des permissions associees a la configuration systeme.

@Getter
@RequiredArgsConstructor
public enum SettingsPermission {
  SETTINGS_INCIDENT_TYPES("SETTINGS_INCIDENT_TYPES"),
  SETTINGS_SYSTEM("SETTINGS_SYSTEM");

  private final String name;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(SettingsPermission::getName)
      .toArray(String[]::new);
  }
}
