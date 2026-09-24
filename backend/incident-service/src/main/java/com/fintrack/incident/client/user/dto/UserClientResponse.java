// DTO : transporte les donnees liees a user client entre les couches.

package com.fintrack.incident.client.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

// DTO representant un utilisateur retourne par le service utilisateur
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserClientResponse {

  private UUID id;
  private String username;
  private String firstName;
  private String lastName;
  private String email;

  @JsonProperty("isActive")
  private boolean isActive;

  private Set<String> roles;
  private Set<String> permissions;
  private UUID agencyId;
  private UUID serviceId;
  private Set<UUID> managedServiceIds;
}
