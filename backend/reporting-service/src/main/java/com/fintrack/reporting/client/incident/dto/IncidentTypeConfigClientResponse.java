// DTO : transporte les donnees liees a incident type config client entre les couches.

package com.fintrack.reporting.client.incident.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a inincident type configuration client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncidentTypeConfigClientResponse {

  private String id;
  private String name;
  private String displayName;
  private String description;

  @JsonAlias({ "isActive", "active" })
  private Boolean active;

  private Integer slaHours;
}
