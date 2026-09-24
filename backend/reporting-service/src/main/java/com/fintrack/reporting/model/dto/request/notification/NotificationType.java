// DTO : transporte les donnees liees a notification type entre les couches.

package com.fintrack.reporting.model.dto.request.notification;

// Enumere les valeurs metier supportees pour notification type.

public enum NotificationType {
  EMAIL,
  PUSH,
  INTERNAL,
  SYSTEM,
}
