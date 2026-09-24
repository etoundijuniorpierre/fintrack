// Centralise les sections et metriques disponibles pour la generation de rapport.
export const REPORT_SECTIONS = [
  "synthesis",
  "distributions",
  "performance",
  "trends",
] as const;

// Centralise la logique d'interface liee a rapport section.
export type ReportSection = (typeof REPORT_SECTIONS)[number];

// Associe chaque section aux metriques attendues par le backend.
// and handles scope logic
// Les tendances suivent desormais le perimetre demande : toutes les sections sont offertes.
export const getReportMetricsForScope = (): string[] => [...REPORT_SECTIONS];

// Determine actual indicateurs for sections e partir du contexte fourni.
export const getActualMetricsForSections = (
  sections: string[],
  scope?: string,
  hasUserFilter = false,
): string[] => {
  const metrics = new Set<string>();

  // Incidents list is mandatory in Lot 6
  metrics.add("incidentsList");

  if (sections.includes("synthesis")) {
    metrics.add("activeIncidents");
    metrics.add("blockedIncidents");
    metrics.add("rejectedIncidents");
    metrics.add("cancelledIncidents");
    metrics.add("inflow");
    metrics.add("outflow");
    metrics.add("netBacklog");
    metrics.add("slaBreachNow");
    if (scope === "own" || hasUserFilter) {
      metrics.add("assignedToMe");
      metrics.add("transferredByMe");
      metrics.add("closedByMe");
      metrics.add("createdByMe");
      metrics.add("resolvedByMe");
    }
  }

  if (sections.includes("distributions")) {
    metrics.add("distributionByType");
    metrics.add("distributionByCriticality");
    metrics.add("distributionByStatus");
    metrics.add("ageDistribution");
    metrics.add("cohortOutcome");
    metrics.add("closureHoursByType");
    metrics.add("closureHoursByCriticality");
  }

  if (sections.includes("performance")) {
    metrics.add("avgClosureHours");
    metrics.add("medianClosureHours");
    metrics.add("p90ClosureHours");
    metrics.add("avgNetClosureHours");
    metrics.add("medianNetClosureHours");
    metrics.add("closureSampleSize");
    metrics.add("resolutionSampleSize");
    metrics.add("avgResolutionHours");
    metrics.add("medianResolutionHours");
    metrics.add("p90ResolutionHours");
    metrics.add("transferRate");
    metrics.add("transferDenominator");
    metrics.add("slaComplianceRate");
    metrics.add("slaDenominator");
    metrics.add("resolutionReopenRate");
    metrics.add("reopenDenominator");
    metrics.add("avgTimeToFirstResponse");

    if (!hasUserFilter && scope !== "own") {
      metrics.add("workload");
    }
    if (
      !hasUserFilter &&
      (scope === "agency" || scope === "service")
    ) {
      metrics.add("efficiencyScorecard");
    }

    if (!hasUserFilter) {
      if (
        scope === "agency" ||
        scope === "byAgency" ||
        scope === "all" ||
        scope === "global"
      ) {
        metrics.add("topServices");
        metrics.add("topResolvers");
      }
      if (scope === "all" || scope === "global") {
        metrics.add("topAgencies");
      }
      if (scope === "service" || scope === "byService") {
        metrics.add("topResolvers");
      }
    }
  }

  if (sections.includes("trends")) {
    metrics.add("monthlyClosures");
    metrics.add("monthlyAvgClosureHours");
  }

  return Array.from(metrics);
};

// Retourne les metriques techniques correspondant au modele de contenu choisi.
export const getMetricsForReportContent = (
  contentType:
    | "OPERATIONAL"
    | "INCIDENT_TYPE_ANALYSIS"
    | "INCIDENT_STATUS_OVERVIEW",
  sections: string[],
  scope?: string,
  hasUserFilter = false,
): string[] => {
  if (contentType === "INCIDENT_TYPE_ANALYSIS") {
    return ["incidentsList", "distributionByType"];
  }
  if (contentType === "INCIDENT_STATUS_OVERVIEW") {
    return ["incidentsList"];
  }
  return getActualMetricsForSections(sections, scope, hasUserFilter);
};
