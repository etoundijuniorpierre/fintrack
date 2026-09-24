// Client inter-services : communique avec les services externes lies a audit service client.

package com.fintrack.user.client.audit.fallback;

import com.fintrack.user.client.audit.AuditServiceClient;
import com.fintrack.user.client.audit.dto.AuditLogClientRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli du client Feign d'audit : invoque si l'audit-service est injoignable, le journal n'est alors pas enregistre.
@Slf4j
@Component
public class AuditServiceClientFallback implements AuditServiceClient {

  // Trace un avertissement lorsque le log d'audit n'a pas pu etre transmis.
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
