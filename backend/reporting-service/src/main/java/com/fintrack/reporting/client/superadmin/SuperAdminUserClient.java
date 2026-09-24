// Client inter-services : communique avec les services externes lies a super admin user.

package com.fintrack.reporting.client.superadmin;

import com.fintrack.reporting.client.superadmin.dto.AgencyClientResponse;
import com.fintrack.reporting.client.superadmin.dto.ServiceClientResponse;
import com.fintrack.reporting.client.superadmin.dto.SuperAdminUserClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserPermissionClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserRoleClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserStatsClientResponse;
import com.fintrack.reporting.client.superadmin.fallback.SuperAdminUserClientFallback;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
  name = "user-service",
  contextId = "superAdminUserClient",
  url = "${user.service.url}",
  fallback = SuperAdminUserClientFallback.class
)
// Definit le contrat super admin user attendu par les autres couches.
public interface SuperAdminUserClient {
  // Fournit users a la couche appelante.

  @GetMapping("/api/v1/userService/users/all")
  List<SuperAdminUserClientResponse> getUsers();

  @GetMapping("/api/v1/userService/users/stats")
  // Fournit utilisateur statistiques au cas d usage appelant.
  UserStatsClientResponse getUserStats(
    @RequestParam("period") String period,
    @RequestParam("view") String view
  );

  @GetMapping("/api/v1/userService/roles/all")
  // Fournit roles au cas d usage appelant.
  List<UserRoleClientResponse> getRoles();

  // Fournit agencies a la couche appelante.

  @GetMapping("/api/v1/userService/agencies/all")
  List<AgencyClientResponse> getAgencies();

  // Fournit services a la couche appelante.

  @GetMapping("/api/v1/userService/departments/all")
  List<ServiceClientResponse> getServices();

  // Fournit permissions a la couche appelante.

  @GetMapping("/api/v1/userService/permissions/all")
  List<UserPermissionClientResponse> getPermissions();

  // Fournit agence by id a la couche appelante.

  @GetMapping("/api/v1/userService/internal/agencies/{id}")
  AgencyClientResponse getAgencyById(@PathVariable("id") String agencyId);

  // Fournit department by id a la couche appelante.

  @GetMapping("/api/v1/userService/internal/departments/{id}")
  ServiceClientResponse getDepartmentById(
    @PathVariable("id") String departmentId
  );
}
