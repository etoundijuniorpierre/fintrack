// Constantes metier : centralise les valeurs stables liees a notification permission.

package com.fintrack.user.model.constant.notification;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Permissions liees a la consultation et a l'administration des notifications.
@Getter
@RequiredArgsConstructor
public enum NotificationPermission {
  NOTIFICATION_VIEW_OWN("NOTIFICATION_VIEW_OWN"),
  NOTIFICATION_VIEW_ALL("NOTIFICATION_VIEW_ALL"),
  NOTIFICATION_MANAGE("NOTIFICATION_MANAGE");

  private final String name;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(NotificationPermission::getName)
      .toArray(String[]::new);
  }
}
