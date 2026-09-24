// Client inter-services : communique avec les services externes lies a super admin user client.

package com.fintrack.reporting.client.superadmin.fallback;

import com.fintrack.reporting.client.superadmin.SuperAdminUserClient;
import com.fintrack.reporting.client.superadmin.dto.AgencyClientResponse;
import com.fintrack.reporting.client.superadmin.dto.ServiceClientResponse;
import com.fintrack.reporting.client.superadmin.dto.SuperAdminUserClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserPermissionClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserRoleClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserStatsClientResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Repli quand user-service est indisponible : valeurs neutres traitees en mode
// degrade par les appelants Super Admin (au lieu de NoFallbackAvailableException).
@Slf4j
@Component
public class SuperAdminUserClientFallback implements SuperAdminUserClient {

  // Fournit users a la couche appelante.

  @Override
  public List<SuperAdminUserClientResponse> getUsers() {
    log.warn(
      "Repli : user-service indisponible — getUsers dégradé (liste vide)"
    );
    return List.of();
  }

  // Fournit utilisateur statistiques a la couche appelante.

  @Override
  public UserStatsClientResponse getUserStats(String period, String view) {
    log.warn(
      "Repli : user-service indisponible — statistiques utilisateur dégradées"
    );
    return null;
  }

  // Fournit roles a la couche appelante.

  @Override
  public List<UserRoleClientResponse> getRoles() {
    log.warn(
      "Repli : user-service indisponible — getRoles dégradé (liste vide)"
    );
    return List.of();
  }

  // Fournit agencies a la couche appelante.

  @Override
  public List<AgencyClientResponse> getAgencies() {
    log.warn(
      "Repli : user-service indisponible — getAgencies dégradé (liste vide)"
    );
    return List.of();
  }

  // Fournit services a la couche appelante.

  @Override
  public List<ServiceClientResponse> getServices() {
    log.warn(
      "Repli : user-service indisponible — getServices dégradé (liste vide)"
    );
    return List.of();
  }

  // Fournit permissions a la couche appelante.

  @Override
  public List<UserPermissionClientResponse> getPermissions() {
    log.warn(
      "Repli : user-service indisponible — getPermissions dégradé (liste vide)"
    );
    return List.of();
  }

  // Fournit agence by id a la couche appelante.

  @Override
  public AgencyClientResponse getAgencyById(String agencyId) {
    log.warn(
      "Repli : user-service indisponible — getAgencyById({}) dégradé",
      agencyId
    );
    return null;
  }

  // Fournit department by id a la couche appelante.

  @Override
  public ServiceClientResponse getDepartmentById(String departmentId) {
    log.warn(
      "Repli : user-service indisponible — getDepartmentById({}) dégradé",
      departmentId
    );
    return null;
  }
}
