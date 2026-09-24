// Tests frontend : verifie le comportement de generate rapport modal.test.

import { describe, expect, it } from "vitest";
import { getActualMetricsForSections } from "./reportMetrics";

describe("GenerateReportModal metrics", () => {
  it("keeps monthly user metrics and hides non-user rankings when a user filter is selected", () => {
    const sections = [
      "synthesis",
      "distributions",
      "performance",
      "trends",
    ];
    const metrics = getActualMetricsForSections(sections, "all", true);

    expect(metrics).toContain("monthlyAvgClosureHours");
    expect(metrics).not.toContain("topAgencies");
    expect(metrics).not.toContain("topServices");
    expect(metrics).not.toContain("topResolvers");
  });

  it("exposes service and resolver rankings when the report is not filtered by user", () => {
    const sections = [
      "synthesis",
      "distributions",
      "performance",
      "trends",
    ];
    const metrics = getActualMetricsForSections(sections, "all", false);

    expect(metrics).toContain("topServices");
    expect(metrics).toContain("topResolvers");
    expect(metrics).toContain("topAgencies");
  });

  it("includes the new distribution and performance metrics", () => {
    const metrics = getActualMetricsForSections(
      ["distributions", "performance"],
      "all",
      false,
    );

    expect(metrics).toContain("cohortOutcome");
    expect(metrics).toContain("distributionByStatus");
    expect(metrics).toContain("ageDistribution");
    expect(metrics).toContain("medianClosureHours");
    expect(metrics).toContain("p90ClosureHours");
    expect(metrics).toContain("medianResolutionHours");
    expect(metrics).toContain("p90ResolutionHours");
    expect(metrics).toContain("slaDenominator");
    expect(metrics).toContain("transferDenominator");
    expect(metrics).toContain("reopenDenominator");
    expect(metrics).toContain("workload");
    expect(metrics).not.toContain("efficiencyScorecard");
  });

  it("masks workload and scorecard under a user filter", () => {
    const metrics = getActualMetricsForSections(["performance"], "all", true);

    expect(metrics).not.toContain("workload");
    expect(metrics).not.toContain("efficiencyScorecard");
  });

  it("adds personal and breach metrics in the synthesis section", () => {
    const metrics = getActualMetricsForSections(["synthesis"], "own", false);

    expect(metrics).toContain("slaBreachNow");
    expect(metrics).toContain("createdByMe");
    expect(metrics).toContain("resolvedByMe");
  });
});
