// DTO : transporte les donnees liees a incident comment entre les couches.

package com.fintrack.incident.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

// DTO de requete pour ajouter un commentaire a un incident.
@Data
public class IncidentCommentRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 2000, message = "{validation.size.max}")
  private String content;

  @JsonProperty("isInternal")
  private boolean isInternal;

  private UUID parentCommentId;
}
