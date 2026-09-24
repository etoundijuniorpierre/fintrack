// Client inter-services : communique avec les services externes lies a super admin audit.

package com.fintrack.reporting.client.superadmin;

import com.fintrack.reporting.client.superadmin.dto.AuditLogsPageClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AuditStatsClientResponse;
import com.fintrack.reporting.client.superadmin.fallback.SuperAdminAuditClientFallback;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
  name = "audit-service",
  contextId = "superAdminAuditClient",
  url = "${audit.service.url}",
  fallback = SuperAdminAuditClientFallback.class
)
// Definit le contrat super admin audit attendu par les autres couches.
public interface SuperAdminAuditClient {
  @GetMapping("/api/v1/auditService/audit-logs")
  // Fournit audit logs au cas d usage appelant.
  AuditLogsPageClientResponse getAuditLogs(
    @RequestParam("page") int page,
    @RequestParam("size") int size,
    @RequestParam("sort") String sort,
    @RequestParam(value = "action", required = false) String action,
    @RequestParam(value = "status", required = false) String status,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to
  );

  @GetMapping("/api/v1/auditService/audit-logs/stats")
  // Fournit statistiques au cas d usage appelant.
  AuditStatsClientResponse getStats(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "sampleSize", defaultValue = "20") int sampleSize,
    @RequestParam(
      value = "repeatedThreshold",
      defaultValue = "3"
    ) int repeatedThreshold
  );

  @RequestMapping(
    method = RequestMethod.DELETE,
    value = "/api/v1/auditService/audit-logs/bulk"
  )
  // Traite en masse purge.
  Map<String, Object> bulkPurge(@RequestBody Map<String, Object> request);
}
