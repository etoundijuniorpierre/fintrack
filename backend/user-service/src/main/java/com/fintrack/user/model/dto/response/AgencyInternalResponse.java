// DTO : transporte les donnees liees a agency internal entre les couches.

package com.fintrack.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;

// DTO de reponse contenant les donnees internes d'une agence.

@Data
public class AgencyInternalResponse {

  private UUID id;
  private String name;
  private String code;

  @JsonProperty("isActive")
  private boolean isActive;
}
