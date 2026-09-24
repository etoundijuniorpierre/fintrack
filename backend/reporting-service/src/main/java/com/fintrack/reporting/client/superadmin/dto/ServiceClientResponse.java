// DTO : transporte les donnees liees a service client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a service client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceClientResponse {

  private UUID id;
  private String name;
  private String description;

  @JsonAlias({ "isActive", "active" })
  private Boolean active;

  private UserSummaryClientResponse headOfService;
  private List<UserSummaryClientResponse> members;
}
