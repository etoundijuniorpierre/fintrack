// Catalogue des rapports : centralise les metriques autorisees selon le perimetre.

package com.fintrack.reporting.service.impl;

import java.util.LinkedHashSet;
import java.util.Set;

// Definit le contrat unique partage par la selection des rapports.
public final class ReportMetricCatalog {

  private static final Set<String> COMMON = Set.of(
    "totalIncidents",
    "activeIncidents",
    "closedIncidents",
    "rejectedIncidents",
    "cancelledIncidents",
    "blockedIncidents",
    "avgClosureHours",
    "medianClosureHours",
    "p90ClosureHours",
    "avgNetClosureHours",
    "medianNetClosureHours",
    "closureSampleSize",
    "resolutionSampleSize",
    "avgResolutionHours",
    "medianResolutionHours",
    "p90ResolutionHours",
    "transferRate",
    "transferDenominator",
    "slaComplianceRate",
    "slaDenominator",
    "resolutionReopenRate",
    "reopenDenominator",
    "avgTimeToFirstResponse",
    "inflow",
    "outflow",
    "netBacklog",
    "ageDistribution",
    "slaBreachNow",
    "cohortOutcome",
    "cohortCompletion",
    "distributionByType",
    "distributionByCriticality",
    "distributionByStatus",
    "closureHoursByType",
    "closureHoursByCriticality",
    "monthlyClosures",
    "monthlyAvgClosureHours"
  );
  private static final Set<String> PERSONAL = Set.of(
    "assignedToMe",
    "transferredByMe",
    "closedByMe",
    "createdByMe",
    "resolvedByMe"
  );

  private ReportMetricCatalog() {}

  // Retourne les metriques que le tableau de bord peut fournir pour ce perimetre.
  public static Set<String> allowedFor(String view, boolean hasUserFilter) {
    String normalized = view == null ? "own" : view.toLowerCase();
    Set<String> metrics = new LinkedHashSet<>(COMMON);
    if ("own".equals(normalized) || hasUserFilter) {
      metrics.addAll(PERSONAL);
    }
    if (!hasUserFilter && !"own".equals(normalized)) {
      metrics.add("workload");
      metrics.add("topResolvers");
    }
    if (!hasUserFilter && ("agency".equals(normalized) || "all".equals(normalized))) {
      metrics.add("topServices");
    }
    if (!hasUserFilter && "all".equals(normalized)) {
      metrics.add("topAgencies");
    }
    if (!hasUserFilter && ("agency".equals(normalized) || "service".equals(normalized))) {
      metrics.add("efficiencyScorecard");
    }
    return Set.copyOf(metrics);
  }
}
