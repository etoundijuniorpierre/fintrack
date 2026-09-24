// Client inter-services : communique avec les services externes lies a user service client.

package com.fintrack.document.client.user.fallback;

import com.fintrack.document.client.user.UserServiceClient;
import com.fintrack.document.client.user.dto.UserClientResponse;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli utilise quand user-service est indisponible : renvoie un utilisateur "inconnu".
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

  // Retourne un utilisateur marqueur "unknown" lorsque l'appel distant echoue.
  @Override
  public UserClientResponse getUserById(UUID id) {
    log.warn("Repli : user-service indisponible pour getUserById({})", id);
    UserClientResponse fallback = new UserClientResponse();
    fallback.setId(id);
    fallback.setUsername("unknown");
    return fallback;
  }
}
