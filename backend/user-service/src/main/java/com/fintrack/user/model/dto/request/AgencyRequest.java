// DTO : transporte les donnees liees a agency entre les couches.

package com.fintrack.user.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;

// DTO de requete pour enregistrer une agence.

@Data
public class AgencyRequest {

  @NotBlank(message = "{agency.name.required}")
  private String name;

  @NotBlank(message = "{agency.city.required}")
  private String city;

  private String address;

  @JsonProperty("isActive")
  private boolean isActive = true;

  private UUID headUserId;
}
