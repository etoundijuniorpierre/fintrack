// Client inter-services : communique avec les services externes lies a audit service.

package com.fintrack.reporting.client.audit;

import com.fintrack.reporting.client.audit.dto.AuditLogClientRequest;
import com.fintrack.reporting.client.audit.fallback.AuditServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
  name = "audit-service",
  url = "${audit.service.url}",
  // Client Feign pour communiquer avec le microservice d'audit.

  fallback = AuditServiceClientFallback.class
)
// Definit le contrat audit service attendu par les autres couches.
public interface AuditServiceClient {
  // Point d'acces POST pour creer une ressource.
  @PostMapping("/api/v1/auditService/audit-logs")
  void recordAuditLog(@RequestBody AuditLogClientRequest request);
}
