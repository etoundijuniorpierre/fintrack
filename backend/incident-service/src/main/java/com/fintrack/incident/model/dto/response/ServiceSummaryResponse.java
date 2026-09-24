// DTO : transporte les donnees liees a service summary entre les couches.

package com.fintrack.incident.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.Data;

// DTO de reponse contenant le resume des informations d'un service.

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceSummaryResponse {

  private UUID id;
  private String name;
  private String description;
}
