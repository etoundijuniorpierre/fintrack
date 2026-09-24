// Tests backend : verrouille les metriques disponibles selon le perimetre.

package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

// Verifie que le catalogue ne propose aucune metrique indisponible au backend.
class ReportMetricCatalogTest {

  @Test
  void shouldExposeAdvancedMetricsForAgencyReports() {
    Set<String> metrics = ReportMetricCatalog.allowedFor("agency", false);

    assertThat(metrics).contains(
      "avgClosureHours",
      "medianClosureHours",
      "p90ClosureHours",
      "medianResolutionHours",
      "p90ResolutionHours",
      "slaComplianceRate",
      "slaDenominator",
      "transferDenominator",
      "reopenDenominator",
      "workload",
      "efficiencyScorecard",
      "topServices",
      "closureHoursByType",
      "closureHoursByCriticality"
    );
    // Les tendances ne sont plus reservees aux vues globale et personnelle.
    assertThat(metrics).contains("monthlyClosures", "monthlyAvgClosureHours");
    assertThat(metrics).doesNotContain("topAgencies");
    assertThat(metrics).doesNotContain("recentActivities");
  }

  @Test
  void shouldKeepPersonalMetricsOutOfGlobalReports() {
    Set<String> metrics = ReportMetricCatalog.allowedFor("all", false);

    assertThat(metrics)
      .contains("topAgencies", "monthlyClosures")
      .doesNotContain("createdByMe", "resolvedByMe");
  }

  @Test
  void shouldExposePersonalMetricsForTargetedUserReports() {
    assertThat(ReportMetricCatalog.allowedFor("all", true)).contains(
      "assignedToMe",
      "createdByMe",
      "resolvedByMe",
      "monthlyAvgClosureHours"
    );
  }
}
