// DTO : transporte les donnees liees a system health section entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de systeme sante section.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthSectionResponse {

  private List<HealthRowResponse> services;

  @JsonProperty("_meta")
  private SectionMetadataResponse metadata;
}
