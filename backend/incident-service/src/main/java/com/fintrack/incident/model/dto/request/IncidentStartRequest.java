// DTO : transporte les donnees liees au demarrage du traitement d'un incident.

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete pour la prise en charge (demarrage) d'un incident.
@Data
public class IncidentStartRequest {

  @Size(max = 1000, message = "{validation.size.max}")
  private String comment;

  // Temps de résolution estimé (en heures) ; s'il est renseigné il pilote le SLA.
  @Min(value = 1, message = "{validation.incident.estimated_hours_positive}")
  private Integer estimatedResolutionHours;
}
