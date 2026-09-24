// Tests : selection des sections et derivation des metriques backend selon le scope.

import { describe, it, expect } from "vitest";
import {
  REPORT_SECTIONS,
  getReportMetricsForScope,
  getActualMetricsForSections,
  getMetricsForReportContent,
} from "./reportMetrics";

describe("getReportMetricsForScope", () => {
  it("should expose trends only where a monthly series exists", () => {
    expect(getReportMetricsForScope()).toEqual([...REPORT_SECTIONS]);
    // Les tendances ne sont plus reservees a une portee particuliere.
    expect(getReportMetricsForScope()).toContain("trends");
  });

  it("should return a fresh array copy, not the shared reference", () => {
    expect(getReportMetricsForScope()).not.toBe(REPORT_SECTIONS);
  });
});

describe("getActualMetricsForSections", () => {
  it("should keep the type analysis contract minimal and deterministic", () => {
    expect(
      getMetricsForReportContent(
        "INCIDENT_TYPE_ANALYSIS",
        [...REPORT_SECTIONS],
        "all",
      ),
    ).toEqual(["incidentsList", "distributionByType"]);
  });

  it("should always include incidentsList even with no section", () => {
    expect(getActualMetricsForSections([])).toEqual(["incidentsList"]);
  });

  it("should add synthesis metrics for the synthesis section", () => {
    const metrics = getActualMetricsForSections(["synthesis"]);
    expect(metrics).toEqual(
      expect.arrayContaining([
        "activeIncidents",
        "blockedIncidents",
        "inflow",
        "outflow",
        "netBacklog",
        "slaBreachNow",
      ]),
    );
    expect(metrics).not.toContain("totalIncidents");
    expect(metrics).not.toContain("closedIncidents");
  });

  it("should add personal metrics when scope is own", () => {
    const metrics = getActualMetricsForSections(["synthesis"], "own");
    expect(metrics).toEqual(
      expect.arrayContaining(["assignedToMe", "closedByMe", "createdByMe"]),
    );
  });

  it("should add personal metrics when a user filter is present", () => {
    const metrics = getActualMetricsForSections(["synthesis"], "agency", true);
    expect(metrics).toContain("assignedToMe");
  });

  it("should not add personal metrics for scope agency without a user filter", () => {
    const metrics = getActualMetricsForSections(["synthesis"], "agency");
    expect(metrics).not.toContain("assignedToMe");
  });

  it("should add distribution metrics for the distributions section", () => {
    const metrics = getActualMetricsForSections(["distributions"]);
    expect(metrics).toEqual(
      expect.arrayContaining([
        "distributionByType",
        "distributionByCriticality",
        "cohortOutcome",
      ]),
    );
  });

  it("should add workload and efficiencyScorecard for performance scope agency without user filter", () => {
    const metrics = getActualMetricsForSections(["performance"], "agency");
    expect(metrics).toEqual(
      expect.arrayContaining(["workload", "efficiencyScorecard"]),
    );
  });

  it("should not add workload when a user filter is present", () => {
    const metrics = getActualMetricsForSections(
      ["performance"],
      "agency",
      true,
    );
    expect(metrics).not.toContain("workload");
  });

  it("should add topAgencies only for scope all/global", () => {
    expect(getActualMetricsForSections(["performance"], "all")).toContain(
      "topAgencies",
    );
    expect(
      getActualMetricsForSections(["performance"], "agency"),
    ).not.toContain("topAgencies");
  });

  it("should add topResolvers for scope service", () => {
    expect(getActualMetricsForSections(["performance"], "service")).toContain(
      "topResolvers",
    );
  });

  it("should expose both trend metrics whatever the scope", () => {
    ["all", "own", "agency", "service"].forEach((scope) => {
      expect(getActualMetricsForSections(["trends"], scope)).toContain(
        "monthlyClosures",
      );
      expect(getActualMetricsForSections(["trends"], scope)).toContain(
        "monthlyAvgClosureHours",
      );
    });
  });

  it("should break the closure delay down by type and criticality", () => {
    const metrics = getActualMetricsForSections(["distributions"], "agency");
    expect(metrics).toContain("closureHoursByType");
    expect(metrics).toContain("closureHoursByCriticality");
  });

  it("should not expose the activity log as a report section", () => {
    expect(REPORT_SECTIONS).not.toContain("recentActivity");
    expect(
      getActualMetricsForSections(["recentActivity"]),
    ).not.toContain("recentActivities");
  });

  it("should not contain duplicates when several sections share metrics", () => {
    const metrics = getActualMetricsForSections([
      "synthesis",
      "distributions",
      "performance",
    ]);
    expect(new Set(metrics).size).toBe(metrics.length);
  });
});
