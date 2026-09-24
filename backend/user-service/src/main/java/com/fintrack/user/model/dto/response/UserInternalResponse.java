// DTO : transporte les donnees liees a user internal entre les couches.

package com.fintrack.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

// DTO de reponse contenant les details internes d'un compte utilisateur.

@Data
public class UserInternalResponse {

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
}
