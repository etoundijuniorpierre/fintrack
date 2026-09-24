// DTO : transporte les donnees liees a super admin overview meta entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de super-administration vue d ensemble meta.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminOverviewMetaResponse {

  private String generatedAt;
  private Long durationMs;
  private List<String> degraded;
}
