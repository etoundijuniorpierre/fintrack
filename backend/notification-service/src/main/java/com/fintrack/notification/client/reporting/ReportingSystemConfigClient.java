// Client inter-services : communique avec les services externes lies a reporting system config.

package com.fintrack.notification.client.reporting;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

// Client Feign pour lire les seuils metier configures cote reporting-service
// (source de verite pour, ex., le nombre max de tentatives d'envoi).
@FeignClient(name = "reporting-service", url = "${reporting.service.url}")
// Definit le contrat reporting system config attendu par les autres couches.
public interface ReportingSystemConfigClient {
  // Fournit seuils a la couche appelante.

  @GetMapping("/api/v1/reportingService/internal/system-config/thresholds")
  Map<String, Object> getThresholds();
}
