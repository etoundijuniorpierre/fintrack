// DTO : transporte les donnees liees a enriched notification entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de enriched notification.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedNotificationResponse {

  private String id;
  private String type;
  private String recipient;
  private String subject;
  private String subjectEn;
  private long retryCount;
  private String nextRetry;
  private String status;
  private String createdAt;
  private String diagnosis;
  private String recommendedAction;
  private long maxRetry;
}
