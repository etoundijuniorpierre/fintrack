// Client inter-services : communique avec les services externes lies a incident service client.

package com.fintrack.notification.client.incident.fallback;

import com.fintrack.notification.client.incident.IncidentServiceClient;
import com.fintrack.notification.client.incident.dto.IncidentClientResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Implementation de repli utilisee quand incident-service est indisponible
@Slf4j
@Component
public class IncidentServiceClientFallback implements IncidentServiceClient {

  // Retourne un incident generique lorsque l'appel distant echoue
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
