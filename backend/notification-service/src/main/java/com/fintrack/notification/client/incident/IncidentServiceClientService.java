// Client inter-services : communique avec les services externes lies a incident service client.

package com.fintrack.notification.client.incident;

import com.fintrack.notification.client.incident.dto.IncidentClientResponse;
import com.fintrack.notification.model.dto.response.IncidentSummaryResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
// Service applicatif qui resout et met en cache les incidents distants
public class IncidentServiceClientService {

  private final IncidentServiceClient incidentServiceClient;

  // Cette fonction recupere un incident distant et le convertit en resume, avec mise en cache
  @Cacheable(
    value = "incidents",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  public IncidentSummaryResponse resolveIncident(UUID id) {
    if (id == null) return null;
    log.debug("Résolution de l'incident {} depuis incident-service", id);
    IncidentClientResponse r;
    try {
      r = incidentServiceClient.getIncidentById(id);
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de résoudre l'incident {} depuis incident-service: {}",
        id,
        ex.getMessage()
      );
      return fallbackSummary(id);
    }
    if (r == null) return null;
    return IncidentSummaryResponse.builder()
      .id(r.getId())
      .title(r.getTitle())
      .status(r.getStatus())
      .build();
  }

  // Fournit le resume de repli d'un incident indisponible.

  private IncidentSummaryResponse fallbackSummary(UUID id) {
    return IncidentSummaryResponse.builder()
      .id(id)
      .title("unknown")
      .status("UNKNOWN")
      .build();
  }
}
