// Composant backend : porte la logique liee a audit export row.

package com.fintrack.reporting.model.readmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Ligne interne d'export CSV audit.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditExportRow {

  private String timestamp;
  private String username;
  private String userId;
  private String action;
  private String status;
  private String resourceType;
  private String resourceId;
  private String ipAddress;
}
