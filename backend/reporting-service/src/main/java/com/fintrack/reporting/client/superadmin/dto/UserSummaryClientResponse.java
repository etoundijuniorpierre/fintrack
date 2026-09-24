// DTO : transporte les donnees liees a user summary client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a super admin user client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSummaryClientResponse {

  private UUID id;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
}
