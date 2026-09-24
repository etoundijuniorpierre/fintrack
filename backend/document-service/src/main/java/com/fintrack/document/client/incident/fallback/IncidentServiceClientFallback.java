// Client inter-services : communique avec les services externes lies a incident service client.

package com.fintrack.document.client.incident.fallback;

import com.fintrack.document.client.incident.IncidentServiceClient;
import com.fintrack.document.client.incident.dto.IncidentClientResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli utilise quand incident-service est indisponible : renvoie un incident "inconnu".
@Slf4j
@Component
public class IncidentServiceClientFallback implements IncidentServiceClient {
  @Override
  public Boolean authorizeAttachment(UUID id, String category, UUID commentId) {
    return false;
  }

  // Retourne un incident marqueur "UNKNOWN" lorsque l'appel distant echoue.
  @Override
  public IncidentClientResponse getIncidentById(UUID id) {
    log.warn(
      "Repli : incident-service indisponible pour getIncidentById({})",
      id
    );
    IncidentClientResponse fallback = new IncidentClientResponse();
    fallback.setId(id);
    fallback.setTitle("unknown");
    fallback.setStatus("UNKNOWN");
    return fallback;
  }
}
