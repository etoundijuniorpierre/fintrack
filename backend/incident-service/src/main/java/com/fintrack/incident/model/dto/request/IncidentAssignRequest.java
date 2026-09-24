// DTO : transporte les donnees liees a incident assign entre les couches.

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

// DTO de requete pour affecter un incident a un utilisateur.

@Data
public class IncidentAssignRequest {

  @NotNull(message = "{validation.not_blank}")
  private UUID assignedTo;

  @Size(max = 1000, message = "{validation.size.max}")
  private String comment;
}
