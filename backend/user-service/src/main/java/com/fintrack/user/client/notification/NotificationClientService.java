// Client inter-services : communique avec les services externes lies a notification client.

package com.fintrack.user.client.notification;

import com.fintrack.user.model.mapper.NotificationRequestMapper;
import java.util.Set;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Adapter sortant vers notification-service.
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationClientService {

  private final NotificationClient notificationClient;
  private final NotificationRequestMapper notificationRequestMapper;

  // Retourne les identifiants des utilisateurs disposant d'une session WebSocket active.
  public Set<String> getOnlineUsernames() {
    try {
      var response = notificationClient.getPresence();
      return response == null || response.getOnline() == null
        ? Set.of()
        : Set.copyOf(response.getOnline());
    } catch (RuntimeException ex) {
      log.warn(
        "Presence indisponible ; le filtre connecte considere tout le monde hors ligne : {}",
        ex.toString()
      );
      return Set.of();
    }
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void sendEmail(
    String recipient,
    BilingualText subject,
    BilingualText content,
    Map<String, Object> templateParams
  ) {
    sendEmail(recipient, subject, content, templateParams, null);
  }

  // Diffuse un e-mail avec une cle d'idempotence optionnelle.
  public void sendEmail(
    String recipient,
    BilingualText subject,
    BilingualText content,
    Map<String, Object> templateParams,
    String idempotencyKey
  ) {
    notificationClient.createNotification(
      notificationRequestMapper.toEmailRequest(
        recipient,
        subject,
        content,
        templateParams,
        idempotencyKey
      )
    );
  }

  // Diffuse l'information du domaine notification client aux destinataires concernes.

  public void sendInternal(
    String recipient,
    BilingualText subject,
    BilingualText content,
    Map<String, Object> templateParams
  ) {
    sendInternal(recipient, subject, content, templateParams, null);
  }

  // Diffuse une notification interne avec une cle d'idempotence optionnelle.
  public void sendInternal(
    String recipient,
    BilingualText subject,
    BilingualText content,
    Map<String, Object> templateParams,
    String idempotencyKey
  ) {
    notificationClient.createNotification(
      notificationRequestMapper.toInternalRequest(
        recipient,
        subject,
        content,
        templateParams,
        idempotencyKey
      )
    );
  }
}
