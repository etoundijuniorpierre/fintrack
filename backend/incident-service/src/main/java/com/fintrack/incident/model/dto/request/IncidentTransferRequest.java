// DTO : transporte les donnees liees a incident transfer entre les couches.

package com.fintrack.incident.model.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

// DTO de requete pour transferer un incident a un autre service ou agence.

@Data
public class IncidentTransferRequest {

  private UUID targetServiceId;

  private UUID targetAgencyId;

  private UUID newTypeId;

  @Size(max = 1000, message = "{validation.size.max}")
  private String reason;

  @Size(max = 1000, message = "{validation.size.max}")
  private String comment;

  // Exige une destination unique pour eviter les transferts ambigus.
  @JsonIgnore
  @AssertTrue(message = "{validation.incident.transfer_target_required}")
  public boolean isTargetSelectionValid() {
    return (targetServiceId == null) != (targetAgencyId == null);
  }
}
