// DTO : transporte les donnees liees a incident close entre les couches.

package com.fintrack.incident.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete pour cloturer un incident.

@Data
public class IncidentCloseRequest {

  /**
   * Description de clôture obligatoire : synthèse officielle attachée à l'incident fermé.
   * Sans cette information, la clôture est refusée.
   */
  @NotBlank(message = "{validation.incident.closure_description_required}")
  @Size(max = 5000, message = "{validation.size.max}")
  private String closureDescription;

  @Size(max = 1000, message = "{validation.size.max}")
  private String comment;
}
