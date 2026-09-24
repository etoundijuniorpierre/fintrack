// DTO : transporte les donnees liees a incident resume entre les couches.

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete transportant les parametres de incident reprise.

@Data
public class IncidentResumeRequest {

  @Size(max = 1000, message = "{validation.size.max}")
  private String comment;
}
