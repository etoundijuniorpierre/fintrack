// Client inter-services : communique avec les services externes lies a audit log sender.

package com.fintrack.incident.client.audit;

import com.fintrack.incident.client.audit.dto.AuditLogClientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
// Emetteur asynchrone des journaux d'audit : isole dans son propre bean pour que
// l'annotation @Async passe bien par le proxy Spring (pas d'auto-invocation).
public class AuditLogSender {

  private final AuditServiceClient auditServiceClient;

  // Diffuse l'information du domaine audit journal sender aux destinataires concernes.

  @Async
  public void send(AuditLogClientRequest request) {
    try {
      auditServiceClient.recordAuditLog(request);
    } catch (Exception e) {
      log.error(
        "Échec d'envoi du log d'audit: action={} ressource={}/{} erreur={}",
        request.getAction(),
        request.getResourceType(),
        request.getResourceId(),
        e.getMessage()
      );
    }
  }
}
