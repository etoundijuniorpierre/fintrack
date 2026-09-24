// DTO : transporte les donnees liees au traitement d'un incident (IN_PROGRESS -> TREATED).

package com.fintrack.incident.model.dto.request;

import com.fintrack.incident.model.constant.IncidentCause;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO de requete : le traitant marque un incident comme traite, en attente de validation.

@Data
public class IncidentTreatRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 2000, message = "{validation.size.max}")
  private String treatmentDescription;

  /**
   * Cause principale, saisie au moment du traitement. Devient obligatoire
   * (avec causeDetail) lorsque le type d'incident impose l'analyse de cause.
   */
  private IncidentCause cause;

  @Size(max = 2000, message = "{validation.size.max}")
  private String causeDetail;
}
