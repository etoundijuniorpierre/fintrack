// Client inter-services : communique avec les services externes lies a user client.

package com.fintrack.incident.client.user.fallback;

import com.fintrack.incident.client.user.UserClient;
import com.fintrack.incident.client.user.dto.AgencyClientResponse;
import com.fintrack.incident.client.user.dto.ServiceClientResponse;
import com.fintrack.incident.client.user.dto.UserClientResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli utilise quand le service utilisateur est indisponible : renvoie des valeurs inconnues par defaut.
@Slf4j
@Component
public class UserClientFallback implements UserClient {

  // Fournit sante a la couche appelante.

  @Override
  public Map<String, Object> getHealth() {
    return Map.of("status", "UNKNOWN");
  }

  // Fournit utilisateur by id a la couche appelante.

  @Override
  public UserClientResponse getUserById(UUID id) {
    UserClientResponse fallback = new UserClientResponse();
    fallback.setId(id);
    fallback.setUsername("unknown");
    fallback.setRoles(Set.of());
    fallback.setPermissions(Set.of());
    return fallback;
  }

  @Override
  public List<UserClientResponse> getUsersByIds(Set<UUID> ids) {
    if (ids == null) return List.of();
    return ids.stream().map(this::getUserById).toList();
  }

  @Override
  public List<AgencyClientResponse> getAgenciesByIds(Set<UUID> ids) {
    if (ids == null) return List.of();
    return ids.stream().map(this::getAgencyById).toList();
  }

  @Override
  public List<ServiceClientResponse> getServicesByIds(Set<UUID> ids) {
    if (ids == null) return List.of();
    return ids.stream().map(this::getServiceById).toList();
  }

  // Fournit agence by id a la couche appelante.

  @Override
  public AgencyClientResponse getAgencyById(UUID id) {
    AgencyClientResponse fallback = new AgencyClientResponse();
    fallback.setId(id);
    fallback.setName("unknown");
    return fallback;
  }

  // Fournit service by id a la couche appelante.

  @Override
  public ServiceClientResponse getServiceById(UUID id) {
    ServiceClientResponse fallback = new ServiceClientResponse();
    fallback.setId(id);
    fallback.setName("unknown");
    return fallback;
  }

  // Fournit service head a la couche appelante.

  @Override
  public UserClientResponse getServiceHead(UUID id) {
    return null;
  }

  // Fournit agence head a la couche appelante.

  @Override
  public UserClientResponse getAgencyHead(UUID id) {
    return null;
  }

  // Fournit admins a la couche appelante.

  @Override
  public List<UserClientResponse> getAdmins() {
    log.warn(
      "Repli : user-service indisponible — administrateurs non resolus, ils ne seront pas notifies"
    );
    return List.of();
  }

  // Retourne un filtre vide lorsque les utilisateurs inactifs ne peuvent pas etre resolus.
  @Override
  public List<UUID> getInactiveUserIds() {
    return List.of();
  }
}
