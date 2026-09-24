// DTO : transporte les donnees liees a notification entre les couches.

package com.fintrack.notification.model.dto.response;

import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.constant.NotificationType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les details d'une notification.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

  private String id;
  private NotificationType type;
  private String recipient;
  private IncidentSummaryResponse incidentId;
  private String subject;
  private String content;
  private String subjectEn;
  private String contentEn;
  private NotificationStatus status;
  private LocalDateTime sentAt;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UUID modifiedBy;
  private Map<String, Object> templateParams;
}
