// Mapper : convertit les donnees liees a system health builder entre modeles.

package com.fintrack.reporting.model.mapper.superadmin;

import com.fintrack.reporting.client.superadmin.dto.AuditLogClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AuditStatsClientResponse;
import com.fintrack.reporting.model.dto.response.superadmin.HealthRowResponse;
import com.fintrack.reporting.model.readmodel.superadmin.HealthProbe;
import com.fintrack.reporting.model.readmodel.superadmin.SuperAdminSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

// Assure les conversions du domaine system health builder.

@Component
public class SystemHealthBuilder {

  // Initialise le mapper avec ses dependances de construction.

  public SystemHealthBuilder() {}

  // Construit la representation attendue pour le domaine systeme sante.

  @SuppressWarnings("unchecked")
  public List<HealthRowResponse> build(SuperAdminSnapshot snapshot) {
    AuditStatsClientResponse auditStats = snapshot.getAuditStats();
    if (auditStats == null) {
      auditStats = new AuditStatsClientResponse(
        0,
        Map.of(),
        Map.of(),
        0,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of()
      );
    }
    Map<String, HealthProbe> probes =
      snapshot.getHealthProbes() != null
        ? snapshot.getHealthProbes()
        : Map.of();

    Map<String, Long> failuresByResourceType =
      auditStats.getFailuresByResourceType() != null
        ? auditStats.getFailuresByResourceType()
        : Map.of();
    List<AuditLogClientResponse> recentLogs =
      auditStats.getRecentLogs() != null
        ? auditStats.getRecentLogs()
        : List.of();
    List<HealthRowResponse> health = new ArrayList<>();

    probes.forEach((key, probeResult) -> {
      String baseUrl = probeResult.getBaseUrl();
      String status =
        probeResult.getStatus() != null ? probeResult.getStatus() : "UNKNOWN";
      String lastCheckedAt = probeResult.getLastCheckedAt();
      long responseTimeMs =
        probeResult.getResponseTimeMs() != null
          ? probeResult.getResponseTimeMs()
          : 0L;
      String endpoint = probeResult.getEndpoint();
      Map<String, Object> components =
        probeResult.getComponents() != null
          ? probeResult.getComponents()
          : Map.of();
      String error = probeResult.getError();

      long errorsForKey = failuresByResourceType
        .entrySet()
        .stream()
        .filter(
          entry ->
            entry.getKey() != null &&
            entry.getKey().toLowerCase(Locale.ROOT).contains(key)
        )
        .mapToLong(Map.Entry::getValue)
        .sum();

      health.add(
        new HealthRowResponse(
          key,
          labelForService(key),
          baseUrl,
          lastCheckedAt,
          status,
          responseTimeMs,
          endpoint,
          components,
          error,
          errorsForKey,
          serviceLogsForKey(recentLogs, key)
        )
      );
    });
    return health;
  }

  // Realise l'intention metier service logs for key.

  private List<AuditLogClientResponse> serviceLogsForKey(
    List<AuditLogClientResponse> recentLogs,
    String key
  ) {
    // Les actions du reporting sont prefixees REPORT_* (et non REPORTING_*) :
    // on cherche donc le jeton "report" pour la cle "reporting".
    String serviceToken = "reporting".equals(key)
      ? "report"
      : key.toLowerCase(Locale.ROOT);
    return recentLogs
      .stream()
      .filter(logRow -> {
        String resourceType = (
          logRow.getResourceType() != null ? logRow.getResourceType() : ""
        ).toLowerCase(Locale.ROOT);
        String action = (
          logRow.getAction() != null ? logRow.getAction() : ""
        ).toLowerCase(Locale.ROOT);
        String service = (
          logRow.getService() != null ? logRow.getService() : ""
        ).toLowerCase(Locale.ROOT);
        return (
          resourceType.contains(serviceToken) ||
          action.contains(serviceToken) ||
          service.contains(serviceToken)
        );
      })
      .limit(10)
      .toList();
  }

  // Realise l'intention metier label for service.

  private String labelForService(String key) {
    return switch (key) {
      case "user" -> "User service";
      case "incident" -> "Incident service";
      case "document" -> "Document service";
      case "notification" -> "Notification service";
      case "reporting" -> "Reporting service";
      case "audit" -> "Audit service";
      default -> key;
    };
  }
}
