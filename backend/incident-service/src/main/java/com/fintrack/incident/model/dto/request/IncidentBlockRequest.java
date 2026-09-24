// DTO : transporte les donnees liees a incident block entre les couches.

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete transportant les parametres de incident blocage.

@Data
public class IncidentBlockRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 5000, message = "{validation.size.max}")
  private String reason;

  @Size(max = 1000, message = "{validation.size.max}")
  private String comment;
}
