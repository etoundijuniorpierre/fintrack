// Client inter-services : communique avec les services externes lies a notification client.

package com.fintrack.incident.client.notification.fallback;

import com.fintrack.incident.client.notification.NotificationClient;
import com.fintrack.incident.client.notification.dto.NotificationClientRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli utilise quand le service de notification est indisponible : la notification est seulement tracee localement.
@Slf4j
@Component
public class NotificationClientFallback implements NotificationClient {

  // En cas d'indisponibilite du service de notification, on se contente d'un avertissement sans bloquer le traitement
  @Override
  public void createNotification(NotificationClientRequest request) {
    log.warn(
      "Repli : notification-service indisponible — notification NON envoyée: destinataire={} sujet={}",
      request.getRecipient(),
      request.getSubject()
    );
  }
}
