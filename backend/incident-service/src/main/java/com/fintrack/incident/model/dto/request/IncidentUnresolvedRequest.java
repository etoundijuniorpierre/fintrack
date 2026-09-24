// DTO : transporte le motif de renvoi en traitement (TREATED -> IN_PROGRESS).

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete : le valideur juge un incident traite comme non resolu.

@Data
public class IncidentUnresolvedRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 2000, message = "{validation.size.max}")
  private String reason;
}
