// Mapper : convertit les donnees liees a notifications builder entre modeles.

package com.fintrack.reporting.model.mapper.superadmin;

import com.fintrack.reporting.client.superadmin.dto.NotificationClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationStatsClientResponse;
import com.fintrack.reporting.model.dto.response.superadmin.ChannelStatsResponse;
import com.fintrack.reporting.model.dto.response.superadmin.EnrichedNotificationResponse;
import com.fintrack.reporting.model.dto.response.superadmin.NotificationsOverviewResponse;
import com.fintrack.reporting.service.SystemConfigService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Assure les conversions du domaine notifications builder.

@Component
public class NotificationsBuilder {

  private final SystemConfigService systemConfigService;

  // Initialise le mapper avec ses dependances de construction.

  @Autowired
  public NotificationsBuilder(SystemConfigService systemConfigService) {
    this.systemConfigService = systemConfigService;
  }

  // Construit la representation attendue pour le domaine notifications.

  public NotificationsOverviewResponse build(
    NotificationStatsClientResponse notificationStats
  ) {
    if (notificationStats == null) {
      notificationStats = new NotificationStatsClientResponse(
        0L,
        0L,
        0L,
        0L,
        Map.of(),
        Map.of(),
        0L,
        0L,
        List.of(),
        List.of(),
        List.of(),
        Map.of()
      );
    }
    long total = notificationStats.getTotal();
    long sent = notificationStats.getSent();
    long failed = notificationStats.getFailed();
    long pending = notificationStats.getPending();

    List<NotificationClientResponse> failedSample =
      notificationStats.getFailedSample() != null
        ? notificationStats.getFailedSample()
        : List.of();
    List<NotificationClientResponse> invalidSample =
      notificationStats.getInvalidRecipientsSample() != null
        ? notificationStats.getInvalidRecipientsSample()
        : List.of();
    List<EnrichedNotificationResponse> failedWithDiagnostics =
      withNotificationDiagnostics(failedSample, invalidSample);

    Map<String, Map<String, Long>> rawByTypeAndStatus =
      notificationStats.getByTypeAndStatus();
    Map<String, ChannelStatsResponse> byChannel = new LinkedHashMap<>();
    if (rawByTypeAndStatus != null) {
      for (Map.Entry<
        String,
        Map<String, Long>
      > entry : rawByTypeAndStatus.entrySet()) {
        String channel = entry.getKey() == null ? "UNKNOWN" : entry.getKey();
        Map<String, Long> statuses =
          entry.getValue() != null ? entry.getValue() : Map.of();
        long channelTotal = statuses
          .values()
          .stream()
          .mapToLong(Long::longValue)
          .sum();
        byChannel.put(
          channel,
          new ChannelStatsResponse(
            channelTotal,
            statuses.getOrDefault("SENT", 0L),
            statuses.getOrDefault("FAILED", 0L),
            statuses.getOrDefault("PENDING", 0L),
            statuses
          )
        );
      }
    }

    return new NotificationsOverviewResponse(
      total,
      notificationStats.getByStatus() != null
        ? notificationStats.getByStatus()
        : Map.of(),
      sent,
      failed,
      pending,
      failedWithDiagnostics,
      invalidSample,
      notificationStats.getInvalidRecipientCount(),
      notificationStats.getByType() != null
        ? notificationStats.getByType()
        : Map.of(),
      byChannel,
      failed > 0 ? List.of("FAILED_EMAILS_DETECTED") : List.of()
    );
  }

  // Realise l'intention metier with notification diagnostics.
  private List<EnrichedNotificationResponse> withNotificationDiagnostics(
    List<NotificationClientResponse> failedSample,
    List<NotificationClientResponse> invalidSample
  ) {
    Set<String> invalidRecipients = invalidSample
      .stream()
      .map(NotificationClientResponse::getRecipient)
      .filter(val -> val != null && !val.isBlank())
      .collect(Collectors.toSet());
    long maxRetry = systemConfigService.getThresholdLong(
      "notificationMaxRetryCount",
      5
    );

    return failedSample
      .stream()
      .map(row -> {
        String type = (row.getType() != null ? row.getType() : "").toUpperCase(
          Locale.ROOT
        );
        String recipient = row.getRecipient();
        long retryCount = row.getRetryCount();
        boolean invalidRecipient = invalidRecipients.contains(recipient);
        String nextRetry = row.getNextRetry();

        String diagnosis;
        String recommendedAction;
        if ("EMAIL".equals(type) && invalidRecipient) {
          diagnosis = "INVALID_RECIPIENT";
          recommendedAction = "CHECK_RECIPIENT";
        } else if (
          retryCount >= maxRetry && (nextRetry == null || nextRetry.isBlank())
        ) {
          diagnosis = "RETRY_LIMIT_REACHED";
          recommendedAction = "CHECK_TEMPLATE_OR_PROVIDER";
        } else if (nextRetry != null && !nextRetry.isBlank()) {
          diagnosis = "WAITING_AUTOMATIC_RETRY";
          recommendedAction = "MONITOR_NEXT_RETRY";
        } else {
          diagnosis = "DELIVERY_FAILED";
          recommendedAction = "CHECK_PROVIDER_OR_TEMPLATE";
        }

        return EnrichedNotificationResponse.builder()
          .id(row.getId())
          .type(row.getType())
          .recipient(row.getRecipient())
          .subject(row.getSubject())
          .subjectEn(row.getSubjectEn())
          .retryCount(row.getRetryCount())
          .nextRetry(row.getNextRetry())
          .status(row.getStatus())
          .createdAt(row.getCreatedAt())
          .diagnosis(diagnosis)
          .recommendedAction(recommendedAction)
          .maxRetry(maxRetry)
          .build();
      })
      .toList();
  }
}
