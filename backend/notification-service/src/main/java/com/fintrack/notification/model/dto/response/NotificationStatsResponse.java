// DTO : transporte les donnees liees a notification stats entre les couches.

package com.fintrack.notification.model.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Agregat des notifications calcule en MongoDB ($group). Couvre la fenetre demandee.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsResponse {

  private long total;
  private long sent;
  private long failed;
  private long pending;
  private Map<String, Long> byStatus;
  private Map<String, Long> byType;
  // Decoupage canal x statut : {EMAIL: {SENT: 80, FAILED: 15, PENDING: 5}, INTERNAL: {...}}.
  // Permet au front de presenter le suivi separement pour les canaux EMAIL et INTERNAL,
  // car leurs modes d'echec et leurs SLA sont differents.
  private Map<String, Map<String, Long>> byTypeAndStatus;
  private long invalidRecipientCount;
  private long sentWithoutTraceCount;
  private List<NotificationSample> failedSample;
  private List<NotificationSample> invalidRecipientsSample;
  private List<NotificationSample> sentWithoutTraceSample;
  private Instant computedAt;
}
