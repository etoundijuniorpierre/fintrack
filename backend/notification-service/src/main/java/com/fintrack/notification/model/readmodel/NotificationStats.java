// Composant backend : porte la logique liee a notification stats.

package com.fintrack.notification.model.readmodel;

import com.fintrack.notification.model.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne des statistiques de notifications calculees par le service.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStats {

  private long total;
  private long sent;
  private long failed;
  private long pending;
  private Map<String, Long> byStatus;
  private Map<String, Long> byType;
  private Map<String, Map<String, Long>> byTypeAndStatus;
  private long invalidRecipientCount;
  private long sentWithoutTraceCount;
  private List<Notification> failedSample;
  private List<Notification> invalidRecipientsSample;
  private List<Notification> sentWithoutTraceSample;
  private Instant computedAt;
}
