// Client inter-services : communique avec les services externes lies a user service client.

package com.fintrack.audit.client.user.fallback;

import com.fintrack.audit.client.user.UserServiceClient;
import com.fintrack.audit.client.user.dto.UserClientResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli utilise quand user-service est indisponible : renvoie un utilisateur "inconnu".
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

  // Retourne un marqueur "unknown" portant l'ID demande lorsque l'appel distant echoue.
  // Conserver l'ID permet au mapper d'audit de retomber sur le snapshot username
  // stocke dans l'AuditLog plutot que d'afficher "null null" cote UI.
  @Override
  public UserClientResponse getUserById(UUID id) {
    log.warn("Repli : user-service indisponible pour getUserById({})", id);
    UserClientResponse fallback = new UserClientResponse();
    fallback.setId(id);
    fallback.setUsername("unknown");
    return fallback;
  }

  @Override
  // Retourne une liste vide lorsque la resolution groupee est indisponible.
  public List<UserClientResponse> getUsersByIds(Set<UUID> ids) {
    log.warn(
      "Repli : user-service indisponible pour getUsersByIds({})",
      ids != null ? ids.size() : 0
    );
    return List.of();
  }
}
