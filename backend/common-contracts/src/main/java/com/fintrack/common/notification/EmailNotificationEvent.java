// Constantes metier : catalogue canonique des e-mails envoyes automatiquement,
// chacun activable/desactivable independamment (et, sauf Direction, dote d'une
// liste d'utilisateurs a exclure de la reception).
// Le nom de chaque valeur est la clef de configuration echangee entre services.

package com.fintrack.common.notification;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Evenements e-mail configurables cote Super Admin, lus en runtime par les
// services emetteurs (incident-service). L'ordre pilote l'affichage de l'UI.
@Getter
@RequiredArgsConstructor
public enum EmailNotificationEvent {
  // --- Cycle de vie de l'incident (interrupteur + liste d'exclusion) ---
  INCIDENT_SUBMITTED(EmailNotificationCategory.LIFECYCLE, true),
  INCIDENT_ASSIGNED(EmailNotificationCategory.LIFECYCLE, true),
  STATUS_CHANGED(EmailNotificationCategory.LIFECYCLE, true),
  TRANSFERRED_SERVICE(EmailNotificationCategory.LIFECYCLE, true),
  TRANSFERRED_AGENCY(EmailNotificationCategory.LIFECYCLE, true),
  QUALIFICATION_CHANGED(EmailNotificationCategory.LIFECYCLE, true),
  UNASSIGNED_SERVICE(EmailNotificationCategory.LIFECYCLE, true),
  UNASSIGNED_AGENCY(EmailNotificationCategory.LIFECYCLE, true),
  RELEVANCE_CONFIRMATION(EmailNotificationCategory.LIFECYCLE, true),
  // --- Planifies / automatiques (interrupteur + liste d'exclusion) ---
  // Cadence reglee par le seuil criticalReminderIntervalHours.
  CRITICAL_INCIDENT_REMINDER(EmailNotificationCategory.SCHEDULED, true),
  SLA_REMINDER(EmailNotificationCategory.SCHEDULED, true),
  VALIDATION_REMINDER(EmailNotificationCategory.SCHEDULED, true),
  PENDING_ACTION_REMINDER(EmailNotificationCategory.SCHEDULED, true, false),
  AUTO_STATUS_CHANGED(EmailNotificationCategory.SCHEDULED, true),
  LATE_INCIDENTS_DAILY_REPORT(EmailNotificationCategory.SCHEDULED, true),
  // --- Validation Direction (interrupteur seul, l'interne reste inchange) ---
  DIRECTION_SUBMITTED(EmailNotificationCategory.DIRECTION, false),
  DIRECTION_VALIDATED(EmailNotificationCategory.DIRECTION, false),
  DIRECTION_REJECTED(EmailNotificationCategory.DIRECTION, false);

  // Constructeur courant : evenement actif par defaut.
  EmailNotificationEvent(
    EmailNotificationCategory category,
    boolean supportsExclusion
  ) {
    this(category, supportsExclusion, true);
  }

  private final EmailNotificationCategory category;

  /** Vrai si l'evenement accepte une liste d'utilisateurs exclus de la reception. */
  private final boolean supportsExclusion;

  // Declare APRES supportsExclusion : @RequiredArgsConstructor suit l'ordre des champs,
  // et les deux booleens sont interchangeables a l'appel.
  /** Vrai si l'e-mail part sans reglage explicite du Super Admin. */
  private final boolean enabledByDefault;

  // Clef de stockage "enabled" pour cet evenement.
  public String enabledKey() {
    return "emailNotif." + name() + ".enabled";
  }

  // Clef de stockage de la liste d'exclusion (JSON) pour cet evenement.
  public String excludedKey() {
    return "emailNotif." + name() + ".excluded";
  }

  // Resout un evenement par son nom, ou null si inconnu.
  public static EmailNotificationEvent fromName(String name) {
    return Arrays.stream(values())
      .filter(event -> event.name().equals(name))
      .findFirst()
      .orElse(null);
  }
}
