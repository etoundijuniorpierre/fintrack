// DTO : transporte les donnees liees a user summary entre les couches.

package com.fintrack.reporting.model.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO resume des informations d'un utilisateur resolu depuis le user-service
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
