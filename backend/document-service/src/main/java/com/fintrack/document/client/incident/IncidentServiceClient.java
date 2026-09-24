// Client inter-services : communique avec les services externes lies a incident service.

package com.fintrack.document.client.incident;

import com.fintrack.document.client.incident.dto.IncidentClientResponse;
import com.fintrack.document.client.incident.fallback.IncidentServiceClientFallback;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

// Client Feign vers incident-service pour consulter les incidents distants.
@FeignClient(
  name = "incident-service",
  url = "${incident.service.url}",
  fallback = IncidentServiceClientFallback.class
)
// Definit le contrat incident service attendu par les autres couches.
public interface IncidentServiceClient {
  // Recupere un incident par son identifiant aupres d'incident-service.
  @GetMapping("/api/v1/incidentService/incidents/{id}")
  IncidentClientResponse getIncidentById(@PathVariable UUID id);

  @GetMapping("/api/v1/incidentService/incidents/{id}/attachment-access")
  Boolean authorizeAttachment(@PathVariable UUID id, @RequestParam String category,
    @RequestParam(required = false) UUID commentId);
}
