// DTO : transporte les donnees liees a notification client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a notification client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationClientResponse {

  private String id;
  private String type;
  private String recipient;
  private long retryCount;
  private String nextRetry;
  private String status;
  private String createdAt;
  private String subject;
  private String subjectEn;
}
