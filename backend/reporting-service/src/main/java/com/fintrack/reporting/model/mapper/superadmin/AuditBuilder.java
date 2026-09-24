// Mapper : convertit les donnees liees a audit builder entre modeles.

package com.fintrack.reporting.model.mapper.superadmin;

import com.fintrack.reporting.client.superadmin.dto.AuditStatsClientResponse;
import com.fintrack.reporting.model.dto.response.superadmin.AuditSectionResponse;
import java.util.List;
import java.util.Map;

// Assure les conversions du domaine audit builder.

public final class AuditBuilder {

  // Initialise le mapper avec ses dependances de construction.

  private AuditBuilder() {}

  // Construit la representation attendue pour le domaine audit.

  public static AuditSectionResponse build(
    AuditStatsClientResponse auditStats
  ) {
    if (auditStats == null) {
      auditStats = new AuditStatsClientResponse(
        0L,
        Map.of(),
        Map.of(),
        0L,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of()
      );
    }
    return AuditSectionResponse.builder()
      .byAction(
        auditStats.getByAction() != null ? auditStats.getByAction() : Map.of()
      )
      .byStatus(
        auditStats.getByStatus() != null ? auditStats.getByStatus() : Map.of()
      )
      .recentLogs(
        auditStats.getRecentLogs() != null
          ? auditStats.getRecentLogs()
          : List.of()
      )
      .sensitiveCount(auditStats.getSensitiveCount())
      .recentSensitive(
        auditStats.getRecentSensitive() != null
          ? auditStats.getRecentSensitive()
          : List.of()
      )
      .permissionChangeHistory(
        auditStats.getPermissionChangeHistory() != null
          ? auditStats.getPermissionChangeHistory()
          : List.of()
      )
      .repeatedSensitiveActions(
        auditStats.getRepeatedSensitiveActions() != null
          ? auditStats.getRepeatedSensitiveActions()
          : List.of()
      )
      .exportEndpoint("/api/v1/reportingService/super-admin/audit/export")
      .exportFilters(List.of("action", "status", "from", "to", "limit"))
      .build();
  }
}
