// DTO : transporte les donnees liees a incident reopen entre les couches.

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO de réouverture d'un incident clôturé ou rejeté.
 *
 * Le motif est obligatoire : rouvrir un incident est un acte métier qui
 * doit être justifiée pour traçabilité (audit + historique). Le commentaire
 * libre reste optionnel et permet d'ajouter des détails complémentaires
 * (numéro de ticket externe, lien vers une réclamation, etc.).
 */
@Data
// Modelise la responsabilite applicative liee a incident.
public class IncidentReopenRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(min = 3, max = 500, message = "{validation.size.max}")
  private String reason;

  @Size(max = 1000, message = "{validation.size.max}")
  private String comment;
}
