// Client inter-services : communique avec les services externes lies a user service.

package com.fintrack.reporting.client.user;

import com.fintrack.reporting.client.user.dto.AgencyClientResponse;
import com.fintrack.reporting.client.user.dto.ServiceClientResponse;
import com.fintrack.reporting.client.user.dto.UserClientResponse;
import com.fintrack.reporting.client.user.fallback.UserServiceClientFallback;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Client Feign pour appeler le user-service et recuperer les informations des utilisateurs
@FeignClient(
  name = "user-service",
  url = "${user.service.url}",
  fallback = UserServiceClientFallback.class
)
// Definit le contrat user service attendu par les autres couches.
public interface UserServiceClient {
  // Recupere un utilisateur par son identifiant via l'API interne du user-service
  @GetMapping("/api/v1/userService/internal/users/{id}")
  UserClientResponse getUserById(@PathVariable UUID id);

  @GetMapping("/api/v1/userService/internal/users/report-subjects")
  List<UserClientResponse> getReportSubjects();

  @GetMapping("/api/v1/userService/internal/agencies")
  List<AgencyClientResponse> getAgencies();

  @GetMapping("/api/v1/userService/internal/departments")
  List<ServiceClientResponse> getServices();
}
