// DTO : transporte les donnees liees a notification entre les couches.

package com.fintrack.reporting.model.dto.request.notification;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// DTO de requete transportant les parametres de notification.

@Getter
@Setter
@Builder
public class NotificationRequest {

  private NotificationType type;
  private String recipient;
  private String subject;
  private String content;
  private String subjectEn;
  private String contentEn;
  private String idempotencyKey;
  private Map<String, Object> templateParams;
}
