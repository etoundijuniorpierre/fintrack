// Client inter-services : communique avec les services externes lies a super admin stats client.

package com.fintrack.reporting.client.superadmin;

import com.fintrack.reporting.client.superadmin.dto.AuditStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserStatsClientResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Adapter de chargement des statistiques inter-services pour Super Admin.
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminStatsClientService {

  // Les metriques notifications/emails du SA sont bornees a cette fenetre pour
  // ne pas etre trompeuses (cumul all-time) : un en attente eleve refleterait
  // la file de retry historique, pas un incident. Le KPI failedEmails24h devient
  // ainsi reellement sur 24h.
  private static final int NOTIFICATION_STATS_WINDOW_HOURS = 24;

  private final SuperAdminNotificationClient notificationClient;
  private final SuperAdminAuditClient auditClient;
  private final SuperAdminUserClient userClient;

  // Charge notification stats.

  public NotificationStatsClientResponse loadNotificationStats(int sampleSize) {
    try {
      return loadNotificationStatsOrThrow(sampleSize);
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger les statistiques de notification pour vue d ensemble Super Admin: {}",
        ex.toString()
      );
      return emptyNotificationStats();
    }
  }

  // Charge notification stats ou signale une erreur.

  public NotificationStatsClientResponse loadNotificationStatsOrThrow(
    int sampleSize
  ) {
    LocalDateTime now = LocalDateTime.now();
    NotificationStatsClientResponse stats = notificationClient.getStats(
      now.minusHours(NOTIFICATION_STATS_WINDOW_HOURS).toString(),
      now.toString(),
      sampleSize
    );
    return stats != null ? stats : emptyNotificationStats();
  }

  // Charge audit stats.

  public AuditStatsClientResponse loadAuditStats(int sampleSize) {
    try {
      return loadAuditStatsOrThrow(sampleSize);
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger les statistiques d audit pour vue d ensemble Super Admin: {}",
        ex.toString()
      );
      return emptyAuditStats();
    }
  }

  // Charge audit stats ou signale une erreur.

  public AuditStatsClientResponse loadAuditStatsOrThrow(int sampleSize) {
    AuditStatsClientResponse stats = auditClient.getStats(
      null,
      null,
      sampleSize,
      3
    );
    return stats != null ? stats : emptyAuditStats();
  }

  // Charge user stats.

  public UserStatsClientResponse loadUserStats() {
    try {
      return loadUserStatsOrThrow();
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger les statistiques utilisateur pour la vue d'ensemble Super Admin : {}",
        ex.toString()
      );
      return new UserStatsClientResponse();
    }
  }

  // Charge user stats ou signale une erreur.

  public UserStatsClientResponse loadUserStatsOrThrow() {
    UserStatsClientResponse stats = userClient.getUserStats("ALL", "all");
    return stats != null ? stats : new UserStatsClientResponse();
  }

  // Realise l'intention metier empty notification stats.

  private NotificationStatsClientResponse emptyNotificationStats() {
    return new NotificationStatsClientResponse(
      0,
      0,
      0,
      0,
      Map.of(),
      Map.of(),
      0,
      0,
      List.of(),
      List.of(),
      List.of(),
      Map.of()
    );
  }

  // Realise l'intention metier empty audit stats.

  private AuditStatsClientResponse emptyAuditStats() {
    return new AuditStatsClientResponse(
      0,
      Map.of(),
      Map.of(),
      0,
      List.of(),
      List.of(),
      List.of(),
      List.of(),
      Map.of()
    );
  }
}
