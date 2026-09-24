// DTO : transporte les donnees liees a section metadata entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de section metadonnees.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionMetadataResponse {

  private String section;
  private String generatedAt;
  private long buildTimeMs;
  private List<String> degraded;
}
