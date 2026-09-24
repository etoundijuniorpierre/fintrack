// Contrat metier : expose les operations du domaine notification statistiques.

package com.fintrack.notification.service;

import com.fintrack.notification.model.readmodel.NotificationStats;
import java.time.LocalDateTime;

// Calcul d'agregats cote MongoDB ($group) appele par le reporting-service.
public interface NotificationStatsService {
  // Calcule stats.

  NotificationStats computeStats(
    LocalDateTime from,
    LocalDateTime to,
    int sampleSize
  );
}
