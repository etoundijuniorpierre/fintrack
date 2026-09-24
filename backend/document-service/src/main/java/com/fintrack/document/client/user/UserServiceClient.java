// Client inter-services : communique avec les services externes lies a user service.

package com.fintrack.document.client.user;

import com.fintrack.document.client.user.dto.UserClientResponse;
import com.fintrack.document.client.user.fallback.UserServiceClientFallback;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Client Feign vers user-service pour consulter les utilisateurs distants.
@FeignClient(
  name = "user-service",
  url = "${user.service.url}",
  fallback = UserServiceClientFallback.class
)
// Definit le contrat user service attendu par les autres couches.
public interface UserServiceClient {
  // Recupere un utilisateur par son identifiant aupres de user-service.
  @GetMapping("/api/v1/userService/internal/users/{id}")
  UserClientResponse getUserById(@PathVariable UUID id);
}
