// DTO : transporte les donnees liees a la validation de resolution (TREATED -> RESOLVED).

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete : le valideur confirme la resolution d'un incident traite.

@Data
public class IncidentResolveRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 2000, message = "{validation.size.max}")
  private String resolutionNote;
}
