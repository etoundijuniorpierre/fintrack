// DTO : transporte les donnees liees a audit stats client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

// Transporte les donnees liees a audit statistiques client entre services.

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditStatsClientResponse {

  private long total;
  private Map<String, Long> byAction;
  private Map<String, Long> byStatus;
  private long sensitiveCount;
  private List<AuditLogClientResponse> recentLogs;
  private List<AuditLogClientResponse> recentSensitive;
  private List<AuditLogClientResponse> permissionChangeHistory;
  private List<Map<String, Object>> repeatedSensitiveActions;
  private Map<String, Long> failuresByResourceType;
}
