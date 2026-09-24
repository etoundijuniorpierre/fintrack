// Client inter-services : communique avec les services externes lies a incident service.

package com.fintrack.notification.client.incident;

import com.fintrack.notification.client.incident.dto.IncidentClientResponse;
import com.fintrack.notification.client.incident.fallback.IncidentServiceClientFallback;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Client Feign pour appeler le microservice incident-service
@FeignClient(
  name = "incident-service",
  url = "${incident.service.url}",
  fallback = IncidentServiceClientFallback.class
)
// Definit le contrat incident service attendu par les autres couches.
public interface IncidentServiceClient {
  // Recupere un incident distant par son identifiant
  @GetMapping("/api/v1/incidentService/incidents/{id}")
  IncidentClientResponse getIncidentById(@PathVariable UUID id);
}
