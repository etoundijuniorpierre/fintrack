// DTO : transporte les donnees liees a incident entre les couches.

package com.fintrack.incident.model.dto.request;

import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentCause;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

// DTO de requete pour la creation ou modification d'un incident.

@Data
public class IncidentRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(min = 3, max = 200, message = "{validation.size.max}")
  private String title;

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 5000, message = "{validation.size.max}")
  private String description;

  @NotNull(message = "{validation.not_blank}")
  private UUID typeId;

  @NotNull(message = "{validation.not_blank}")
  private Criticality criticality;

  /** Date de survenance de l'événement (peut être antérieure à la création). */
  @PastOrPresent(message = "{validation.incident.date_not_future}")
  private LocalDate incidentDate;

  /** Date de constatation (optionnelle, par défaut = date de création). */
  @PastOrPresent(message = "{validation.incident.date_not_future}")
  private LocalDate observationDate;

  /** Service cible choisi par l'utilisateur (ignoré si le type d'incident impose un service par défaut). */
  private UUID targetServiceId;

  /** Cause principale de l'incident (optionnelle à la création). */
  private IncidentCause cause;

  /** Précision de la cause : personne responsable ou description détaillée. */
  @Size(max = 2000, message = "{validation.size.max}")
  private String causeDetail;

  /** Si vrai, l'incident est immédiatement assigné à l'utilisateur courant. */
  private boolean assignToSelf = false;

  /**
   * Identifiant de l'agence à laquelle rattacher l'incident.
   * Réservé aux profils globaux (SUPER_ADMIN / ADMIN sans agence propre) :
   * un agent ou un chef d'agence ne peut créer que pour son agence et ce
   * champ est ignoré pour eux. Si null, on retombe sur l'agence de
   * l'utilisateur courant.
   */
  private UUID agencyId;

  // Garantit que l'evenement ne peut pas survenir apres sa constatation.
  @JsonIgnore
  @AssertTrue(message = "{validation.incident.date_order}")
  public boolean isDateOrderValid() {
    return (
      incidentDate == null ||
      observationDate == null ||
      !incidentDate.isAfter(observationDate)
    );
  }
}
