// Contrat metier : expose les operations du domaine notification WebSocket.

package com.fintrack.notification.service;

import com.fintrack.notification.model.entity.Notification;
import com.fintrack.notification.model.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

// Service de push WebSocket : envoie les notifications en temps reel
// au topic STOMP personnalise de chaque destinataire.

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

  private final SimpMessagingTemplate messagingTemplate;
  private final NotificationMapper notificationMapper;

  /**
   * Pousse une notification vers le topic STOMP du destinataire.
   * Les clients abonnés à /topic/notifications/{recipient} recevront le message.
   */
  // Diffuse to recipient.
  public void pushToRecipient(Notification notification) {
    String destination = "/topic/notifications/" + notification.getRecipient();
    try {
      messagingTemplate.convertAndSend(
        destination,
        notificationMapper.toResponse(notification)
      );
      log.debug(
        "Envoi WebSocket vers {}: notification {}",
        destination,
        notification.getId()
      );
    } catch (Exception e) {
      log.warn(
        "Échec de notification WebSocket à {}: {}",
        destination,
        e.getMessage()
      );
    }
  }
}
