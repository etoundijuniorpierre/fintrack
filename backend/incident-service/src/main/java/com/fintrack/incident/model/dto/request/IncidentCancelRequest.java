// DTO : transporte les donnees liees a incident cancel entre les couches.

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete pour annuler un incident. Distinct du rejet : l'annulation retire
// l'incident du circuit, le rejet refuse sa qualification.

@Data
public class IncidentCancelRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 5000, message = "{validation.size.max}")
  private String reason;
}
