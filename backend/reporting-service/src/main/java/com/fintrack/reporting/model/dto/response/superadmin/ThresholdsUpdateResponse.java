// DTO : transporte les donnees liees a thresholds update entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de seuils update.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdsUpdateResponse {

  private SystemThresholdsResponse thresholds;
}
