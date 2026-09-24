// DTO : transporte les donnees liees a notifications overview entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fintrack.reporting.client.superadmin.dto.NotificationClientResponse;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de notifications vue d ensemble.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsOverviewResponse {

  private long total;
  private Map<String, Long> byStatus;
  private long sent;
  private long failed;
  private long pending;
  private List<EnrichedNotificationResponse> failedNotifications;
  private List<NotificationClientResponse> invalidRecipients;
  private long invalidRecipientCount;
  private Map<String, Long> volumeByType;
  private Map<String, ChannelStatsResponse> byChannel;
  private List<String> alerts;
}
