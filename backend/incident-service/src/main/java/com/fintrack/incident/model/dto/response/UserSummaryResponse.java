// DTO : transporte les donnees liees a user summary entre les couches.

package com.fintrack.incident.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

// DTO de reponse contenant le resume des informations d'un utilisateur.

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSummaryResponse {

  private UUID id;
  private String username;
  private String firstName;
  private String lastName;
  private String email;

  @JsonProperty("isActive")
  private boolean isActive;

  private Set<String> roles;
  private Set<String> permissions;
}
