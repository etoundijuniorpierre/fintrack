// Client inter-services : communique avec les services externes lies a super admin notification client.

package com.fintrack.reporting.client.superadmin;

import com.fintrack.reporting.client.superadmin.dto.NotificationClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationsPageClientResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Adapter de lecture des notifications pour les vues Super Admin.
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminNotificationClientService {

  private final SuperAdminNotificationClient notificationClient;

  // Compte les elements du domaine super-administration notification client selon les criteres fournis.

  public long countFailedNotifications() {
    try {
      NotificationsPageClientResponse page =
        notificationClient.getNotifications(0, 200, "createdAt,desc");
      List<NotificationClientResponse> content =
        page == null ? null : page.getContent();
      if (content == null) {
        return 0;
      }
      return content
        .stream()
        .filter(item -> "FAILED".equalsIgnoreCase(item.getStatus()))
        .count();
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger les notifications en echec pour l'escalade : {}",
        ex.toString()
      );
      return 0;
    }
  }
}
