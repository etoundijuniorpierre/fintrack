package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

// Verifie l'ancrage du perimetre "own" garantissant la coherence liste/agregats du rapport.
class ReportServiceImplTest {

  private final UUID creator = UUID.randomUUID();

  @Test
  void shouldAnchorOwnScopeOnReportCreatorWhenNoUserFilter() {
    String resolved = ReportServiceImpl.resolveOwnScopeCreatedBy(
      "own",
      null,
      null,
      creator
    );

    assertThat(resolved).isEqualTo(creator.toString());
  }

  @Test
  void shouldKeepExplicitCreatedByOverOwnAnchor() {
    String explicit = UUID.randomUUID().toString();

    String resolved = ReportServiceImpl.resolveOwnScopeCreatedBy(
      "own",
      explicit,
      null,
      creator
    );

    assertThat(resolved).isEqualTo(explicit);
  }

  @Test
  void shouldNotAnchorCreatedByWhenAssignedToFilterIsPresent() {
    String resolved = ReportServiceImpl.resolveOwnScopeCreatedBy(
      "own",
      null,
      UUID.randomUUID().toString(),
      creator
    );

    assertThat(resolved).isNull();
  }

  @Test
  void shouldNotAnchorForNonOwnScopes() {
    assertThat(
      ReportServiceImpl.resolveOwnScopeCreatedBy("all", null, null, creator)
    ).isNull();
    assertThat(
      ReportServiceImpl.resolveOwnScopeCreatedBy("agency", null, null, creator)
    ).isNull();
    assertThat(
      ReportServiceImpl.resolveOwnScopeCreatedBy("service", null, null, creator)
    ).isNull();
  }

  @Test
  void shouldNotAnchorWhenReportHasNoCreator() {
    String resolved = ReportServiceImpl.resolveOwnScopeCreatedBy(
      "own",
      null,
      null,
      null
    );

    assertThat(resolved).isNull();
  }

  @Test
  void slugStripsAccentsAndPunctuationForFilenames() {
    assertThat(ReportServiceImpl.slug("Agence Centre")).isEqualTo(
      "agence-centre"
    );
    assertThat(ReportServiceImpl.slug("Comptabilité & Paie")).isEqualTo(
      "comptabilite-paie"
    );
    assertThat(ReportServiceImpl.slug("  ")).isEqualTo("rapport");
  }

  @Test
  void buildReportNameCombinesNatureScopeAndPeriod() {
    String name = ReportServiceImpl.buildReportName(
      "Situation des incidents",
      "Par agence",
      "du 01/07/2026 au 10/08/2026"
    );

    assertThat(name).isEqualTo(
      "Situation des incidents - Par agence - du 01/07/2026 au 10/08/2026"
    );
  }
}
