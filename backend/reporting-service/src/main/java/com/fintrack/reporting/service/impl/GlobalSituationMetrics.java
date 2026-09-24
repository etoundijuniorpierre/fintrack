// Utilitaire : assemble la "situation globale" d'un rapport "situation par statut".
// Combine le flux de la periode (tableau de bord) et le backlog courant deja mis en
// forme par familles/retards (IncidentStatusReportBuilder), afin que les compteurs
// soient adosses a la liste reelle des incidents encore ouverts.

package com.fintrack.reporting.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GlobalSituationMetrics {

  private GlobalSituationMetrics() {}

  static final List<String> BACKLOG_STATUSES = List.of(
    "OPEN",
    "PENDING_VALIDATION",
    "VALIDATED",
    "TRANSFERRED",
    "ASSIGNED",
    "IN_PROGRESS",
    "BLOCKED",
    "REOPENED",
    "TREATED",
    "RESOLVED",
    "UNRESOLVED_PROLONGED_WAIT"
  );

  // Assemble la situation globale : familles + liste du backlog (backlogBuild) enrichies
  // des compteurs de flux de la periode, des listes qui les adossent et des totaux de
  // backlog/retard. Les trois listes rendent chaque compteur de flux auditable.
  static Map<String, Object> merge(
    Map<String, Object> dashboard,
    Map<String, Object> backlogBuild,
    Map<String, List<Map<String, Object>>> periodActivity
  ) {
    Map<String, Object> situation = new LinkedHashMap<>();
    if (backlogBuild != null) {
      situation.putAll(backlogBuild); // categorySections, incidentsList, totalIncidents, overdueCount...
    }
    // Flux de la periode (sorties du backlog).
    situation.put("treatedInPeriod", asLong(dashboard, "treatedIncidents"));
    situation.put("resolvedInPeriod", asLong(dashboard, "resolvedIncidents"));
    situation.put("closedInPeriod", asLong(dashboard, "closedIncidents"));
    // Listes adossant chaque compteur de flux (populations distinctes du backlog).
    situation.put("treatedList", flowList(periodActivity, "treated"));
    situation.put("resolvedList", flowList(periodActivity, "resolved"));
    situation.put("closedList", flowList(periodActivity, "closed"));
    // Photographie courante : reste a traiter et retard, adosses a la liste ci-dessus.
    situation.put("untreatedBacklog", asLong(situation, "totalIncidents"));
    situation.put("overdueBacklog", asLong(situation, "overdueCount"));
    return situation;
  }

  private static List<Map<String, Object>> flowList(
    Map<String, List<Map<String, Object>>> periodActivity,
    String key
  ) {
    if (periodActivity == null) {
      return List.of();
    }
    List<Map<String, Object>> list = periodActivity.get(key);
    return list != null ? list : List.of();
  }

  private static long asLong(Map<String, Object> metrics, String key) {
    Object value = metrics != null ? metrics.get(key) : null;
    return value instanceof Number number ? number.longValue() : 0L;
  }
}
