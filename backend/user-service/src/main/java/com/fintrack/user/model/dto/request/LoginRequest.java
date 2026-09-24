// DTO : transporte les donnees liees a login entre les couches.

package com.fintrack.user.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// DTO de requete d'authentification contenant l'email et le mot de passe.

@Data
public class LoginRequest {

  @NotBlank
  private String username;

  @NotBlank
  private String password;
}
