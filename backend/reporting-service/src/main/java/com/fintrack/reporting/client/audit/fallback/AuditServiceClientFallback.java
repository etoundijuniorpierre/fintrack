// Client inter-services : communique avec les services externes lies a audit service client.

package com.fintrack.reporting.client.audit.fallback;

import com.fintrack.reporting.client.audit.AuditServiceClient;
import com.fintrack.reporting.client.audit.dto.AuditLogClientRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Service de repli en cas d'indisponibilite du service d'audit.

@Slf4j
@Component
public class AuditServiceClientFallback implements AuditServiceClient {

  @Override
  // Trace la tentative d'audit lorsque le service distant est indisponible.
  public void recordAuditLog(AuditLogClientRequest request) {
    log.warn(
      "Repli : audit-service indisponible — journal d audit NON enregistré: action={} ressource={}/{}",
      request.getAction(),
      request.getResourceType(),
      request.getResourceId()
    );
  }
}
