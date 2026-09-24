// DTO : transporte les donnees liees a reporting section entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de reporting section.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportingSectionResponse {

  private long total;
  private long failed;
  private long pending;
  private long available;
  private List<ReportRowResponse> failedReports;
  private List<ReportRowResponse> pendingReports;
  private List<ReportRowResponse> availableWithoutFileReports;
  private Double averageGenerationTimeSeconds;
  private String generationInProgressDefinition;

  @JsonProperty("_meta")
  private SectionMetadataResponse metadata;
}
