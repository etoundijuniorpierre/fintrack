// DTO : transporte les donnees liees a incident relevance entre les couches.

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete pour la confirmation d'actualite d'un incident en attente prolongee :
// demander l'actualite comme y repondre. L'infirmation, elle, passe par l'annulation
// et exige donc un motif (IncidentCancelRequest).

@Data
public class IncidentRelevanceRequest {

  @Size(max = 1000, message = "{validation.size.max}")
  private String comment;
}
