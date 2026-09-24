// DTO : transporte les donnees liees a incident client entre les couches.

package com.fintrack.notification.client.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.Data;

// DTO representant la reponse brute renvoyee par incident-service
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncidentClientResponse {

  private UUID id;
  private String title;
  private String status;
}
