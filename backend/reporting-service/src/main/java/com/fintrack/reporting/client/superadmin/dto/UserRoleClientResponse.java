// DTO : transporte les donnees liees a user role client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a utilisateur role client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleClientResponse {

  private String name;
  private String displayName;
  private String description;

  @JsonAlias({ "system", "isSystem" })
  private boolean system;

  private List<UserPermissionClientResponse> permissions;
}
