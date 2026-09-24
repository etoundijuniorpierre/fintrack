// Client inter-services : communique avec les services externes lies a user service client.

package com.fintrack.reporting.client.user;

import com.fintrack.reporting.client.user.dto.AgencyClientResponse;
import com.fintrack.reporting.client.user.dto.ServiceClientResponse;
import com.fintrack.reporting.client.user.dto.UserClientResponse;
import com.fintrack.reporting.model.dto.response.UserSummaryResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

// Service qui resout les informations utilisateur via le client Feign, avec mise en cache
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClientService {

  private final UserServiceClient userServiceClient;

  // Recupere et met en cache le resume d'un utilisateur a partir de son identifiant
  @Cacheable(
    value = "users",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  public UserSummaryResponse resolveUser(UUID id) {
    if (id == null) return null;
    log.debug("Résolution de l'utilisateur {} depuis user-service", id);
    UserClientResponse r = userServiceClient.getUserById(id);
    if (r == null) return null;
    return UserSummaryResponse.builder()
      .id(r.getId())
      .username(r.getUsername())
      .firstName(r.getFirstName())
      .lastName(r.getLastName())
      .email(r.getEmail())
      .build();
  }

  // Charge les sujets utilisateurs actifs en un seul appel inter-service.
  public List<UserClientResponse> getReportSubjects() {
    List<UserClientResponse> users = userServiceClient.getReportSubjects();
    return users != null ? users : List.of();
  }

  // Charge les agences actives en un seul appel inter-service.
  public List<AgencyClientResponse> getAgencies() {
    List<AgencyClientResponse> agencies = userServiceClient.getAgencies();
    return agencies != null ? agencies : List.of();
  }

  // Charge les services actifs en un seul appel inter-service.
  public List<ServiceClientResponse> getServices() {
    List<ServiceClientResponse> services = userServiceClient.getServices();
    return services != null ? services : List.of();
  }
}
