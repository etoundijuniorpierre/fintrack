// Client inter-services : communique avec les services externes lies a notification client.

package com.fintrack.reporting.client.notification;

import com.fintrack.reporting.model.mapper.NotificationRequestMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Adapter sortant vers notification-service.
@Service
@RequiredArgsConstructor
public class NotificationClientService {

  private final NotificationClient notificationClient;
  private final NotificationRequestMapper notificationRequestMapper;

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

  // Diffuse une notification interne (tableau de bord du destinataire).
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
