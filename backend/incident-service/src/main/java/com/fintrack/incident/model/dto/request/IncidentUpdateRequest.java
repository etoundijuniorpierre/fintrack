// DTO : transporte les donnees liees a incident update entre les couches.

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

// DTO de requete pour mettre a jour les details d'un incident.

@Data
public class IncidentUpdateRequest {

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

  @PastOrPresent(message = "{validation.incident.date_not_future}")
  private LocalDate incidentDate;

  @PastOrPresent(message = "{validation.incident.date_not_future}")
  private LocalDate observationDate;

  private IncidentCause cause;

  @Size(max = 2000, message = "{validation.size.max}")
  private String causeDetail;

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
