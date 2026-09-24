// DTO : transporte les donnees liees a change password entre les couches.

package com.fintrack.user.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete pour modifier le mot de passe d'un utilisateur.

@Data
public class ChangePasswordRequest {

  private String currentPassword;

  @NotBlank(message = "{user.password.required}")
  @Size(min = 8)
  private String newPassword;
}
