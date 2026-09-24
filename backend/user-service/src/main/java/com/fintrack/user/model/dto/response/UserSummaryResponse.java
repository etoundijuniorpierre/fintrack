// DTO : transporte les donnees liees a user summary entre les couches.

package com.fintrack.user.model.dto.response;

import java.util.UUID;
import lombok.Data;

// DTO de reponse contenant le resume des informations d'un utilisateur.

@Data
public class UserSummaryResponse {

  private UUID id;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
}
