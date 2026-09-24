// DTO : transporte les donnees liees a controls quality section entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de controles qualite section.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlsQualitySectionResponse {

  private PermissionsOverviewResponse permissions;
  private DataQualityOverviewResponse dataQuality;

  @JsonProperty("_meta")
  private SectionMetadataResponse metadata;
}
