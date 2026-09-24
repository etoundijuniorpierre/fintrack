// DTO : transporte les donnees liees a service entre les couches.

package com.fintrack.user.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;

// DTO de requete pour enregistrer un service interne.

@Data
public class ServiceRequest {

  @NotBlank(message = "{service.name.required}")
  private String name;

  private String description;

  @JsonProperty("isActive")
  private boolean isActive = true;

  private UUID headUserId;
}
