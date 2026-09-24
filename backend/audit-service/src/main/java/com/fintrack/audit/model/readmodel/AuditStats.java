// Composant backend : porte la logique liee a audit stats.

package com.fintrack.audit.model.readmodel;

import com.fintrack.audit.model.entity.AuditLog;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne des statistiques d'audit calculees par le service.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditStats {

  private long total;
  private Map<String, Long> byAction;
  private Map<String, Long> byStatus;
  private long sensitiveCount;
  private List<AuditLog> recentLogs;
  private List<AuditLog> recentSensitive;
  private List<AuditLog> permissionChangeHistory;
  private List<RepeatedAuditAction> repeatedSensitiveActions;
  private Map<String, Long> failuresByResourceType;
  private Map<Integer, Map<Integer, Long>> heatmapLast7Days;
  private Instant computedAt;
}
