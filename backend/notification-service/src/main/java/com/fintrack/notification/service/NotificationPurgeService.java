// Contrat metier : expose les operations du domaine notification purge.

package com.fintrack.notification.service;

import com.fintrack.notification.model.readmodel.NotificationPurgeResult;

// Purge manuelle des notifications anciennes.
public interface NotificationPurgeService {
  // Supprime ou invalide les donnees ciblees apres controle metier.

  NotificationPurgeResult purge(
    int olderThanDays,
    int preserveLastN,
    boolean dryRun
  );
}
