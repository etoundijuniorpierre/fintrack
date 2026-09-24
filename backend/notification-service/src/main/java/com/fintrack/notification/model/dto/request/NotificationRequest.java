// DTO : transporte les donnees liees a notification entre les couches.

package com.fintrack.notification.model.dto.request;

import com.fintrack.notification.model.constant.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// DTO de requete pour planifier ou envoyer une notification.

@Getter
@Setter
public class NotificationRequest {

  @NotNull(message = "{validation.not_blank}")
  private NotificationType type;

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 255)
  private String recipient;

  @Size(max = 255)
  private String subject;

  private String content;

  @Size(max = 255)
  private String subjectEn;

  private String contentEn;

  private UUID incidentId;

  @Size(max = 512)
  private String idempotencyKey;

  private Map<String, Object> templateParams;
}
