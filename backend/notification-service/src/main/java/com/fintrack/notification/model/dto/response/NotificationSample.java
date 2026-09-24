// DTO : transporte les donnees liees a notification sample entre les couches.

package com.fintrack.notification.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de notification sample.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSample {

  private String id;
  private String type;
  private String status;
  private String recipient;
  private String subject;
  private String subjectEn;
  private String createdAt;
  private String sentAt;
  private String retryAt;
  private String nextRetry;
  private Integer retryCount;
  private String incidentId;
}
