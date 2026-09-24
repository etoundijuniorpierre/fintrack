// DTO : transporte les donnees liees a incident history entre les couches.

package com.fintrack.incident.model.dto.response;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.dto.BaseDto;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

// DTO de reponse detaillant une etape de l'historique d'un incident.

@Data
@EqualsAndHashCode(callSuper = true)
public class IncidentHistoryResponse extends BaseDto {

  private UserSummaryResponse user;
  private ActionType action;
  private String oldValue;
  private String newValue;
  private String comment;
  private UUID incidentId;
  private String incidentTitle;
}
