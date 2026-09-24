// DTO : transporte les donnees liees a auth entre les couches.

package com.fintrack.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO retourne apres authentification contenant le token JWT.

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

  private String token;

  @Builder.Default
  private String type = "Bearer";

  private UUID id;
  private UUID agencyId;
  private UUID serviceId;
  private Set<UUID> managedServiceIds;
  private UUID managedAgencyId;
  private String username;
  private Set<String> roles;
  private Set<String> permissions;

  @JsonProperty("isFirstLogin")
  private boolean firstLogin;

  @JsonProperty("isActive")
  private boolean active;
}
