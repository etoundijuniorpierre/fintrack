// DTO : transporte les donnees liees a user entre les couches.

package com.fintrack.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.user.model.dto.BaseDto;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

// DTO de reponse exposant les informations publiques d'un utilisateur.
@Data
@EqualsAndHashCode(callSuper = true)
public class UserResponse extends BaseDto {

  private String username;
  private String email;
  private Long phoneNumber;
  private String firstName;
  private String lastName;

  @JsonProperty("isActive")
  private boolean isActive;

  private LocalDateTime lastLogin;

  @JsonProperty("isFirstLogin")
  private boolean isFirstLogin;

  private UUID avatarDocumentId;
  private AgencyResponse agency;
  private ServiceResponse service;
  private Set<UUID> managedServiceIds;
  private UUID managedAgencyId;
  private Set<RoleResponse> roles;
  private Set<PermissionResponse> permissions;
  private Set<PermissionResponse> revokedPermissions;
}
