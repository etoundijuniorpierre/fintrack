// Client inter-services : communique avec les services externes lies a super admin notification client.

package com.fintrack.reporting.client.superadmin.fallback;

import com.fintrack.reporting.client.superadmin.SuperAdminNotificationClient;
import com.fintrack.reporting.client.superadmin.dto.NotificationClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationsPageClientResponse;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli quand notification-service est indisponible : valeurs neutres traitees
// en mode degrade par les appelants Super Admin (au lieu de NoFallbackAvailableException).
@Slf4j
@Component
public class SuperAdminNotificationClientFallback
  implements SuperAdminNotificationClient
{

  // Fournit notifications a la couche appelante.

  @Override
  public NotificationsPageClientResponse getNotifications(
    int page,
    int size,
    String sort
  ) {
    log.warn(
      "Repli : notification-service indisponible — getNotifications dégradé (liste vide)"
    );
    return null;
  }

  // Fournit all notifications a la couche appelante.

  @Override
  public List<NotificationClientResponse> getAllNotifications() {
    log.warn(
      "Repli : notification-service indisponible — getAllNotifications dégradé (liste vide)"
    );
    return List.of();
  }

  // Fournit statistiques a la couche appelante.

  @Override
  public NotificationStatsClientResponse getStats(
    String from,
    String to,
    int sampleSize
  ) {
    log.warn(
      "Repli : notification-service indisponible — statistiques de notification dégradées"
    );
    return null;
  }

  // Fournit notification by id a la couche appelante.

  @Override
  public NotificationClientResponse getNotificationById(String id) {
    log.warn(
      "Repli : notification-service indisponible — getNotificationById({}) dégradé",
      id
    );
    return null;
  }

  // Traite en masse purge.

  @Override
  public Map<String, Object> bulkPurge(Map<String, Object> request) {
    log.warn("Repli : notification-service indisponible — purge non exécutée");
    return Map.of();
  }
}
