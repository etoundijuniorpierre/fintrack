// Mapper : convertit les donnees liees a permissions builder entre modeles.

package com.fintrack.reporting.model.mapper.superadmin;

import com.fintrack.reporting.client.superadmin.dto.AuditStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserPermissionClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserRoleClientResponse;
import com.fintrack.reporting.model.dto.response.superadmin.PermissionsOverviewResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Assure les conversions du domaine permissions builder.

public final class PermissionsBuilder {

  public static final List<String> RESERVED_PERMISSIONS = List.of(
    "REPORT_DELETE",
    "INCIDENT_VIEW_ALL",
    "USER_MANAGE",
    "USER_CREATE_ADMIN",
    "NOTIFICATION_MANAGE",
    "SETTINGS_SYSTEM",
    "ROLE_UPDATE",
    "ROLE_DELETE"
  );

  // Initialise le mapper avec ses dependances de construction.

  private PermissionsBuilder() {}

  // Construit la representation de permission attendue par le cas d'usage.
  public static PermissionsOverviewResponse build(
    List<UserRoleClientResponse> roles,
    List<UserPermissionClientResponse> permissions,
    AuditStatsClientResponse auditStats
  ) {
    if (auditStats == null) {
      auditStats = new AuditStatsClientResponse(
        0L,
        Map.of(),
        Map.of(),
        0L,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of()
      );
    }
    boolean changeAuditObserved =
      auditStats.getPermissionChangeHistory() != null &&
      !auditStats.getPermissionChangeHistory().isEmpty();

    List<UserRoleClientResponse> systemRoles = roles
      .stream()
      .filter(
        role ->
          role.isSystem() ||
          Set.of("SUPER_ADMIN", "ADMIN").contains(
            (role.getName() != null ? role.getName() : "").toUpperCase(
              Locale.ROOT
            )
          )
      )
      .toList();

    Map<String, List<String>> rolePermissionMatrix = roles
      .stream()
      .collect(
        Collectors.toMap(
          role -> role.getName() != null ? role.getName() : "UNKNOWN",
          role ->
            role.getPermissions() != null
              ? role
                  .getPermissions()
                  .stream()
                  .map(UserPermissionClientResponse::getName)
                  .toList()
              : List.of(),
          (left, right) -> left,
          LinkedHashMap::new
        )
      );

    return new PermissionsOverviewResponse(
      roles,
      permissions,
      RESERVED_PERMISSIONS,
      RESERVED_PERMISSIONS,
      systemRoles,
      rolePermissionMatrix,
      true,
      changeAuditObserved,
      List.of("role deletion", "system permission mutation", "global settings")
    );
  }
}
