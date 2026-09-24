// Mapper : convertit les donnees liees a notification request entre modeles.

package com.fintrack.reporting.model.mapper;

import com.fintrack.reporting.client.notification.BilingualText;
import com.fintrack.reporting.model.dto.request.notification.NotificationRequest;
import com.fintrack.reporting.model.dto.request.notification.NotificationType;
import java.util.Map;
import org.springframework.stereotype.Component;

// Mapper des requetes sortantes vers notification-service.
@Component
public class NotificationRequestMapper {

  // Convertit notification vers le modele attendu par la couche appelante.
  public NotificationRequest toEmailRequest(
    String recipient,
    BilingualText subject,
    BilingualText content,
    Map<String, Object> templateParams
  ) {
    return toEmailRequest(recipient, subject, content, templateParams, null);
  }

  // Convertit notification avec une cle d'idempotence fournie par l'emetteur.
  public NotificationRequest toEmailRequest(
    String recipient,
    BilingualText subject,
    BilingualText content,
    Map<String, Object> templateParams,
    String idempotencyKey
  ) {
    return build(
      NotificationType.EMAIL,
      recipient,
      subject,
      content,
      templateParams,
      idempotencyKey
    );
  }

  // Construit une requete de notification interne (tableau de bord).
  public NotificationRequest toInternalRequest(
    String recipient,
    BilingualText subject,
    BilingualText content,
    Map<String, Object> templateParams,
    String idempotencyKey
  ) {
    return build(
      NotificationType.INTERNAL,
      recipient,
      subject,
      content,
      templateParams,
      idempotencyKey
    );
  }

  // Assemble la requete en portant les deux variantes linguistiques.
  private NotificationRequest build(
    NotificationType type,
    String recipient,
    BilingualText subject,
    BilingualText content,
    Map<String, Object> templateParams,
    String idempotencyKey
  ) {
    return NotificationRequest.builder()
      .type(type)
      .recipient(recipient)
      .subject(subject != null ? subject.fr() : null)
      .subjectEn(subject != null ? subject.en() : null)
      .content(content != null ? content.fr() : null)
      .contentEn(content != null ? content.en() : null)
      .idempotencyKey(idempotencyKey)
      .templateParams(templateParams)
      .build();
  }
}
