// DTO : transporte les donnees liees a incident validate entre les couches.

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

// DTO de requete pour la validation d'un incident.

@Data
public class IncidentValidateRequest {

  @Size(max = 1000, message = "{validation.size.max}")
  private String comment;

  private UUID targetServiceId;

  private UUID targetUserId;
}
