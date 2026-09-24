// DTO : transporte les donnees liees a incident summary entre les couches.

package com.fintrack.document.model.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO resume d'un incident, alimente via l'appel au service incident.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentSummaryResponse {

  private UUID id;
  private String title;
  private String status;
}
