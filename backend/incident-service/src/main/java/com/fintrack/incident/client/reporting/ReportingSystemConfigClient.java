// Client inter-services : communique avec les services externes lies a reporting system config.

package com.fintrack.incident.client.reporting;

import com.fintrack.incident.client.reporting.dto.EmailNotificationSettingsClient;
import com.fintrack.incident.client.reporting.fallback.ReportingSystemConfigClientFallback;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

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

  // Fournit les reglages e-mail par evenement a la couche appelante.
  @GetMapping(
    "/api/v1/reportingService/internal/system-config/email-notifications"
  )
  EmailNotificationSettingsClient getEmailNotifications();
}
