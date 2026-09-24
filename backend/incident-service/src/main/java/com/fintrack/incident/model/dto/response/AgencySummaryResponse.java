// DTO : transporte les donnees liees a agency summary entre les couches.

package com.fintrack.incident.model.dto.response;

import java.util.UUID;
import lombok.Data;

// DTO de reponse resumant les informations d'une agence.

@Data
public class AgencySummaryResponse {

  private UUID id;
  private String name;
  private String code;
}
