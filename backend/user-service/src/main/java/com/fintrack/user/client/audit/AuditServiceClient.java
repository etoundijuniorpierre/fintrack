// Client inter-services : communique avec les services externes lies a audit service.

package com.fintrack.user.client.audit;

import com.fintrack.user.client.audit.dto.AuditLogClientRequest;
import com.fintrack.user.client.audit.fallback.AuditServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Client Feign vers l'audit-service pour envoyer les journaux d'audit, avec repli si le service est indisponible.
@FeignClient(
  name = "audit-service",
  url = "${audit.service.url}",
  fallback = AuditServiceClientFallback.class
)
// Definit le contrat audit service attendu par les autres couches.
public interface AuditServiceClient {
  // Envoie un log d'audit a l'audit-service.
  @PostMapping("/api/v1/auditService/audit-logs")
  void recordAuditLog(@RequestBody AuditLogClientRequest request);
}
