// DTO : transporte les donnees liees a system config section entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de configuration systeme section.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigSectionResponse {

  private SystemThresholdsResponse businessThresholds;

  @JsonProperty("_meta")
  private SectionMetadataResponse metadata;
}
