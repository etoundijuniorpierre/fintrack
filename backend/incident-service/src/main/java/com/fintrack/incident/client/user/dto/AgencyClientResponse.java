// DTO : transporte les donnees liees a agency client entre les couches.

package com.fintrack.incident.client.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.Data;

// DTO representant une agence retournee par le service utilisateur
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgencyClientResponse {

  private UUID id;
  private String name;
  private String code;
  private boolean active;
}
