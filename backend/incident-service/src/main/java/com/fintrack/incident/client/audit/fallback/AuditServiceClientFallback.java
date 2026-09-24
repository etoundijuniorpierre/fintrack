// Client inter-services : communique avec les services externes lies a audit service client.

package com.fintrack.incident.client.audit.fallback;

import com.fintrack.incident.client.audit.AuditServiceClient;
import com.fintrack.incident.client.audit.dto.AuditLogClientRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli utilise quand le service d'audit est indisponible : le journal est seulement trace localement.
@Slf4j
@Component
public class AuditServiceClientFallback implements AuditServiceClient {

  // En cas d'indisponibilite du service d'audit, on se contente d'un avertissement sans bloquer le traitement
  @Override
  public void recordAuditLog(AuditLogClientRequest request) {
    log.warn(
      "Repli : audit-service indisponible — journal d audit NON enregistré: action={} ressource={}/{}",
      request.getAction(),
      request.getResourceType(),
      request.getResourceId()
    );
  }
}
