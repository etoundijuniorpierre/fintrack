// DTO : transporte les donnees liees a data quality issue entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de qualite des donnees issue.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityIssueResponse {

  private String key;
  private String label;
  private long count;
  private String severity;
}
