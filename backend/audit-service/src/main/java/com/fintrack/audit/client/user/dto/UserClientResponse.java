// DTO : transporte les donnees liees a user client entre les couches.

package com.fintrack.audit.client.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;

// DTO recevant les donnees d'un utilisateur renvoyees par le user-service.
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
}
