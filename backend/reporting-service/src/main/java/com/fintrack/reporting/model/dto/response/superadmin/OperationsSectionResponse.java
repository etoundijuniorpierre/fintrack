// DTO : transporte les donnees liees a operations section entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de operations section.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationsSectionResponse {

  private NotificationsOverviewResponse notifications;

  @JsonProperty("_meta")
  private SectionMetadataResponse metadata;
}
