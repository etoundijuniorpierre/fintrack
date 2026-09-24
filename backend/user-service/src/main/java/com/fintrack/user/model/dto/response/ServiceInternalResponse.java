// DTO : transporte les donnees liees a service internal entre les couches.

package com.fintrack.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;

// DTO de reponse interne pour un service applicatif.

@Data
public class ServiceInternalResponse {

  private UUID id;
  private String name;
  private String description;

  @JsonProperty("isActive")
  private boolean isActive;

  private UserSummaryResponse headOfService;
}
