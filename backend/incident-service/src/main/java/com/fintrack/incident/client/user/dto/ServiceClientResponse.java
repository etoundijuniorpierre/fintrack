// DTO : transporte les donnees liees a service client entre les couches.

package com.fintrack.incident.client.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;

// DTO representant un service (departement) retourne par le service utilisateur
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceClientResponse {

  private UUID id;
  private String name;
  private String description;

  @JsonProperty("isActive")
  private boolean isActive;
}
