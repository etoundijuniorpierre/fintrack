// DTO : transporte les donnees liees a audit stats entre les couches.

package com.fintrack.audit.model.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Agregat des audit logs calcule en MongoDB ($group). Couvre la fenetre demandee.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditStatsResponse {

  private long total;
  private Map<String, Long> byAction;
  private Map<String, Long> byStatus;
  private long sensitiveCount;
  private List<AuditSample> recentLogs;
  private List<AuditSample> recentSensitive;
  private List<AuditSample> permissionChangeHistory;
  private List<RepeatedAction> repeatedSensitiveActions;
  private Map<String, Long> failuresByResourceType;
  // heatmap activite des 7 derniers jours.
  // Cle externe = code jour Mongo (1=Dim ... 7=Sam) ; cle interne = heure 0..23 ;
  // valeur = nombre d'evenements dans le bucket.
  private Map<Integer, Map<Integer, Long>> heatmapLast7Days;
  private Instant computedAt;
}
