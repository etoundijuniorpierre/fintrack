// Constantes metier : centralise les valeurs stables liees a notification type.

package com.fintrack.notification.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration listant les types de notifications geres.

@Getter
@RequiredArgsConstructor
public enum NotificationType implements LocalizableEnum {
  EMAIL(
    "EMAIL",
    "enum.notification_type.EMAIL.name",
    "enum.notification_type.EMAIL.description"
  ),
  INTERNAL(
    "INTERNAL",
    "enum.notification_type.INTERNAL.name",
    "enum.notification_type.INTERNAL.description"
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
      .map(NotificationType::getName)
      .toArray(String[]::new);
  }
}
