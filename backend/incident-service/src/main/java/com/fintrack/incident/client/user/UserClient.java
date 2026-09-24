// Client inter-services : communique avec les services externes lies a user.

package com.fintrack.incident.client.user;

import com.fintrack.incident.client.user.dto.AgencyClientResponse;
import com.fintrack.incident.client.user.dto.ServiceClientResponse;
import com.fintrack.incident.client.user.dto.UserClientResponse;
import com.fintrack.incident.client.user.fallback.UserClientFallback;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Client Feign vers le service utilisateur pour recuperer utilisateurs, agences et services
@FeignClient(
  name = "user-service",
  url = "${user.service.url}",
  fallback = UserClientFallback.class
)
// Definit le contrat user attendu par les autres couches.
public interface UserClient {
  // Verifie l'etat de sante du service utilisateur
  @GetMapping("/actuator/health")
  Map<String, Object> getHealth();

  // Recupere un utilisateur par son identifiant
  @GetMapping("/api/v1/userService/internal/users/{id}")
  UserClientResponse getUserById(@PathVariable UUID id);

  // Recupere plusieurs utilisateurs en masse
  @org.springframework.web.bind.annotation.PostMapping(
    "/api/v1/userService/internal/bulk/users"
  )
  List<UserClientResponse> getUsersByIds(
    @org.springframework.web.bind.annotation.RequestBody java.util.Set<UUID> ids
  );

  // Recupere plusieurs agences en masse
  @org.springframework.web.bind.annotation.PostMapping(
    "/api/v1/userService/internal/bulk/agencies"
  )
  List<AgencyClientResponse> getAgenciesByIds(
    @org.springframework.web.bind.annotation.RequestBody java.util.Set<UUID> ids
  );

  // Recupere plusieurs departements en masse
  @org.springframework.web.bind.annotation.PostMapping(
    "/api/v1/userService/internal/bulk/departments"
  )
  List<ServiceClientResponse> getServicesByIds(
    @org.springframework.web.bind.annotation.RequestBody java.util.Set<UUID> ids
  );

  // Recupere une agence par son identifiant
  @GetMapping("/api/v1/userService/internal/agencies/{id}")
  AgencyClientResponse getAgencyById(@PathVariable UUID id);

  // Recupere un service (departement) par son identifiant
  @GetMapping("/api/v1/userService/internal/departments/{id}")
  ServiceClientResponse getServiceById(@PathVariable UUID id);

  // Recupere les indicateurs globaux sur les utilisateurs
  // Recupere le responsable d'un service donne
  @GetMapping("/api/v1/userService/internal/departments/{id}/head")
  UserClientResponse getServiceHead(@PathVariable UUID id);

  // Recupere le chef d'une agence donnee
  @GetMapping("/api/v1/userService/internal/agencies/{id}/head")
  UserClientResponse getAgencyHead(@PathVariable UUID id);

  // Recupere la liste des administrateurs
  @GetMapping("/api/v1/userService/internal/users/admins")
  List<UserClientResponse> getAdmins();

  // Recupere les IDs des utilisateurs inactifs (filtre qualite "assigne a un inactif")
  @GetMapping("/api/v1/userService/internal/users/inactive-ids")
  List<UUID> getInactiveUserIds();
}
