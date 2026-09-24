// Contrat metier : expose les operations du domaine audit statistiques.

package com.fintrack.audit.service;

import com.fintrack.audit.model.readmodel.AuditStats;
import java.time.LocalDateTime;

// Calcul d'agregats cote MongoDB ($group) appele par le reporting-service.
public interface AuditStatsService {
  // Calcule stats.

  AuditStats computeStats(
    LocalDateTime from,
    LocalDateTime to,
    int sampleSize,
    int repeatedThreshold
  );
}
