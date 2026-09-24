// Client inter-services : communique avec les services externes lies a reporting system config.

package com.fintrack.user.client.reporting;

import com.fintrack.user.client.reporting.fallback.ReportingSystemConfigClientFallback;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

// Client Feign vers le reporting-service pour lire les seuils systeme (Super Admin Parametres systeme).
@FeignClient(
  name = "reporting-service",
  url = "${reporting.service.url}",
  fallback = ReportingSystemConfigClientFallback.class
)
// Definit le contrat reporting system config attendu par les autres couches.
public interface ReportingSystemConfigClient {
  // Fournit seuils a la couche appelante.

  @GetMapping("/api/v1/reportingService/internal/system-config/thresholds")
  Map<String, Object> getThresholds();
}
