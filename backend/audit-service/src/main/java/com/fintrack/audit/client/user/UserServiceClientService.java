// Client inter-services : communique avec les services externes lies a user service client.

package com.fintrack.audit.client.user;

import com.fintrack.audit.client.user.dto.UserClientResponse;
import com.fintrack.audit.model.dto.response.UserSummaryResponse;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

// Resout les informations d'un utilisateur via user-service pour enrichir
// l'affichage des journaux d'audit ; resultat mis en cache par identifiant.
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClientService {

  private final UserServiceClient userServiceClient;

  // On ne cache pas les resultats partiels (sans nom complet) pour pouvoir
  // retenter la resolution si le user-service redevient disponible ou si les
  // champs nominatifs sont completes ulterieurement.
  @Cacheable(
    value = "users",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null || (#result.firstName == null && #result.lastName == null)"
  )
  // Resout utilisateur a partir du contexte disponible.
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

  // Resout username a partir du contexte disponible.

  public String resolveUsername(UUID id) {
    UserSummaryResponse user = resolveUser(id);
    return user != null ? user.getUsername() : null;
  }

  // Resout plusieurs utilisateurs en une requete afin d'eviter les appels Feign par ligne.
  public Map<UUID, UserSummaryResponse> resolveUsers(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    return userServiceClient
      .getUsersByIds(ids)
      .stream()
      .map(this::toSummary)
      .filter(user -> user.getId() != null)
      .collect(
        Collectors.toMap(
          UserSummaryResponse::getId,
          Function.identity(),
          (left, right) -> left
        )
      );
  }

  // Adapte le DTO interne user-service au resume d'audit.
  private UserSummaryResponse toSummary(UserClientResponse response) {
    return UserSummaryResponse.builder()
      .id(response.getId())
      .username(response.getUsername())
      .firstName(response.getFirstName())
      .lastName(response.getLastName())
      .email(response.getEmail())
      .build();
  }
}
