// DTO : transporte les donnees liees a user summary entre les couches.

package com.fintrack.document.model.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO resume d'un utilisateur, alimente via l'appel au service utilisateur.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

  private UUID id;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
}
