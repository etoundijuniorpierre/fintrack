// DTO : transporte les donnees liees a audit section entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.reporting.client.superadmin.dto.AuditLogClientResponse;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de audit section.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditSectionResponse {

  private Map<String, Long> byAction;
  private Map<String, Long> byStatus;
  private List<AuditLogClientResponse> recentLogs;
  // Insights securite que la page Audit generique n'offre pas.
  private long sensitiveCount;
  private List<AuditLogClientResponse> recentSensitive;
  private List<AuditLogClientResponse> permissionChangeHistory;
  private List<Map<String, Object>> repeatedSensitiveActions;
  private String exportEndpoint;
  private List<String> exportFilters;

  @JsonProperty("_meta")
  private SectionMetadataResponse metadata;
}
