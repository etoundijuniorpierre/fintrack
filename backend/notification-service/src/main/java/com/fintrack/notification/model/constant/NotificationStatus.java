// Constantes metier : centralise les valeurs stables liees a notification status.

package com.fintrack.notification.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Enumeration listant les differents etats d'une notification.

@Getter
@RequiredArgsConstructor
public enum NotificationStatus implements LocalizableEnum {
  PENDING(
    "PENDING",
    "enum.notification_status.PENDING.name",
    "enum.notification_status.PENDING.description"
  ),
  SENT(
    "SENT",
    "enum.notification_status.SENT.name",
    "enum.notification_status.SENT.description"
  ),
  FAILED(
    "FAILED",
    "enum.notification_status.FAILED.name",
    "enum.notification_status.FAILED.description"
  ),
  READ(
    "READ",
    "enum.notification_status.READ.name",
    "enum.notification_status.READ.description"
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
      .map(NotificationStatus::getName)
      .toArray(String[]::new);
  }
}
