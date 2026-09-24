// Client inter-services : communique avec les services externes lies a user service client.

package com.fintrack.reporting.client.user.fallback;

import com.fintrack.reporting.client.user.UserServiceClient;
import com.fintrack.reporting.client.user.dto.AgencyClientResponse;
import com.fintrack.reporting.client.user.dto.ServiceClientResponse;
import com.fintrack.reporting.client.user.dto.UserClientResponse;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli utilise lorsque le user-service est indisponible.
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

  // Retourne un utilisateur minimal "unknown" en cas d'echec de l'appel au user-service
  @Override
  public UserClientResponse getUserById(UUID id) {
    log.warn("Repli : user-service indisponible pour getUserById({})", id);
    UserClientResponse fallback = new UserClientResponse();
    fallback.setId(id);
    fallback.setUsername("unknown");
    return fallback;
  }

  @Override
  public List<UserClientResponse> getReportSubjects() {
    log.warn("Repli : user-service indisponible pour les sujets de rapports");
    return List.of();
  }

  @Override
  public List<AgencyClientResponse> getAgencies() {
    log.warn("Repli : user-service indisponible pour les agences de rapports");
    return List.of();
  }

  @Override
  public List<ServiceClientResponse> getServices() {
    log.warn("Repli : user-service indisponible pour les services de rapports");
    return List.of();
  }
}
