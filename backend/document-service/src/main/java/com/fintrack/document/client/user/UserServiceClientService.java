// Client inter-services : communique avec les services externes lies a user service client.

package com.fintrack.document.client.user;

import com.fintrack.document.client.user.dto.UserClientResponse;
import com.fintrack.document.model.dto.response.UserSummaryResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

// Service de resolution des utilisateurs via le client Feign, avec mise en cache des resultats.
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceClientService {

  private final UserServiceClient userServiceClient;

  // Recupere le resume d'un utilisateur ; renvoie un utilisateur "unknown" si le service est indisponible.
  @Cacheable(
    value = "users",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  public UserSummaryResponse resolveUser(UUID id) {
    if (id == null) return null;
    log.debug("Résolution de l'utilisateur {} depuis user-service", id);
    UserClientResponse r;
    try {
      r = userServiceClient.getUserById(id);
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de résoudre l'auteur du téléversement {} depuis user-service",
        id,
        ex
      );
      return UserSummaryResponse.builder().id(id).username("unknown").build();
    }
    if (r == null) return null;
    return UserSummaryResponse.builder()
      .id(r.getId())
      .username(r.getUsername() != null ? r.getUsername() : "unknown")
      .firstName(r.getFirstName())
      .lastName(r.getLastName())
      .email(r.getEmail())
      .build();
  }
}
