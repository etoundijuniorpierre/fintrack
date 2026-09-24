// DTO : transporte les donnees liees a agency client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a agence client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgencyClientResponse {

  private UUID id;
  private String name;
  private String code;

  @JsonAlias({ "isActive", "active" })
  private Boolean active;

  private UserSummaryClientResponse headOfAgency;
  private List<UserSummaryClientResponse> members;
}
