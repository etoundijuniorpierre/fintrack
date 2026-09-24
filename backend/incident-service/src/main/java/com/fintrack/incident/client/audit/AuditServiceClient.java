// Client inter-services : communique avec les services externes lies a audit service.

package com.fintrack.incident.client.audit;

import com.fintrack.incident.client.audit.dto.AuditLogClientRequest;
import com.fintrack.incident.client.audit.fallback.AuditServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Client Feign vers le service d'audit pour envoyer les journaux d'audit
@FeignClient(
  name = "audit-service",
  url = "${audit.service.url}",
  fallback = AuditServiceClientFallback.class
)
// Definit le contrat audit service attendu par les autres couches.
public interface AuditServiceClient {
  // Cet endpoint envoie un journal d'audit au service d'audit
  @PostMapping("/api/v1/auditService/audit-logs")
  void recordAuditLog(@RequestBody AuditLogClientRequest request);
}
