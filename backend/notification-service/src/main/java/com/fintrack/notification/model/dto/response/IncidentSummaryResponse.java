// DTO : transporte les donnees liees a incident summary entre les couches.

package com.fintrack.notification.model.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse contenant le resume des informations d'un incident.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentSummaryResponse {

  private UUID id;
  private String title;
  private String status;
  private String reference;
}
