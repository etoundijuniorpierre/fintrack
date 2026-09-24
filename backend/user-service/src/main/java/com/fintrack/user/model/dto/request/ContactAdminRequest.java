// DTO : transporte les donnees liees a contact admin entre les couches.

package com.fintrack.user.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// DTO de requete transportant les parametres de contact administration.

@Data
public class ContactAdminRequest {

  @NotBlank
  private String username;

  @NotBlank
  private String subject;

  @NotBlank
  private String message;
}
