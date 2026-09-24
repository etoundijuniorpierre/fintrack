// DTO : transporte les donnees liees a notification stats client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a notification statistiques client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsClientResponse {

  private long total;
  private long sent;
  private long failed;
  private long pending;
  private Map<String, Long> byStatus;
  private Map<String, Long> byType;
  private long invalidRecipientCount;
  private long sentWithoutTraceCount;
  private List<NotificationClientResponse> failedSample;
  private List<NotificationClientResponse> invalidRecipientsSample;
  private List<NotificationClientResponse> sentWithoutTraceSample;
  private Map<String, Map<String, Long>> byTypeAndStatus;
}
