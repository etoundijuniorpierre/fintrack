// DTO : transporte les donnees liees a notification client entre les couches.

package com.fintrack.incident.client.notification.dto;

import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

// DTO de requete envoye au service de notification pour creer une notification
@Data
@Builder
public class NotificationClientRequest {

  /** EMAIL ou INTERNAL */
  private String type;
  private String recipient;
  private String subject;
  private String content;
  // Variante anglaise ; le service de notification stocke les deux langues.
  private String subjectEn;
  private String contentEn;
  private UUID incidentId;
  private String idempotencyKey;
  private Map<String, Object> templateParams;
}
