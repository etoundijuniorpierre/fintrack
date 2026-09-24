// DTO : transporte les donnees liees a permissions overview entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fintrack.reporting.client.superadmin.dto.UserPermissionClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserRoleClientResponse;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de permissions vue d ensemble.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionsOverviewResponse {

  private List<UserRoleClientResponse> roles;
  private List<UserPermissionClientResponse> permissions;
  private List<String> criticalPermissions;
  private List<String> reservedPermissions;
  private List<UserRoleClientResponse> systemRoles;
  private Map<String, List<String>> rolePermissionMatrix;
  private boolean changeAuditRequired;
  private boolean changeAuditObserved;
  private List<String> superAdminOnlyControls;
}
