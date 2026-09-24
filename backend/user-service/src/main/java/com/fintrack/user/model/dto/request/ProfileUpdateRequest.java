// DTO : transporte les donnees liees a profile update entre les couches.

package com.fintrack.user.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de requete pour mettre a jour le profil personnel.

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {

  @NotBlank
  private String username;

  private String email;

  private String phoneNumber;

  private UUID avatarDocumentId;
}
