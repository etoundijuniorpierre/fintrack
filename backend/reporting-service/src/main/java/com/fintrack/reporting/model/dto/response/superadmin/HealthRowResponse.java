// DTO : transporte les donnees liees a health row entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import com.fintrack.reporting.client.superadmin.dto.AuditLogClientResponse;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de sante ligne.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRowResponse {

  private String key;
  private String label;
  private String baseUrl;
  private String lastCheckedAt;
  private String status;
  private long responseTimeMs;
  private String endpoint;
  private Map<String, Object> components;
  private String error;
  private long recentErrors;
  private List<AuditLogClientResponse> recentLogs;
}
