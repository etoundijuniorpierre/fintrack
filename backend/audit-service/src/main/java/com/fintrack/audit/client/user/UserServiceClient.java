// Client inter-services : communique avec les services externes lies a user service.

package com.fintrack.audit.client.user;

import com.fintrack.audit.client.user.dto.UserClientResponse;
import com.fintrack.audit.client.user.fallback.UserServiceClientFallback;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Client Feign pour appeler le microservice utilisateur.
@FeignClient(
  name = "user-service",
  url = "${user.service.url}",
  fallback = UserServiceClientFallback.class
)
// Definit le contrat user service attendu par les autres couches.
public interface UserServiceClient {
  // Recupere un utilisateur par son identifiant via l'endpoint interne du user-service.
  @GetMapping("/api/v1/userService/internal/users/{id}")
  UserClientResponse getUserById(@PathVariable UUID id);

  // Recupere plusieurs utilisateurs en une seule requete interne.
  @PostMapping("/api/v1/userService/internal/bulk/users")
  List<UserClientResponse> getUsersByIds(@RequestBody Set<UUID> ids);
}
