// DTO : transporte les donnees liees a super admin user client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a super-administration utilisateur client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminUserClientResponse {

  private UUID id;
  private String username;
  private String email;

  @JsonAlias({ "isActive", "active" })
  private boolean isActive;

  private List<UserRoleClientResponse> roles;
  private UUID agencyId;
  private UUID serviceId;
  private AgencyClientResponse agency;
  private ServiceClientResponse service;
}
