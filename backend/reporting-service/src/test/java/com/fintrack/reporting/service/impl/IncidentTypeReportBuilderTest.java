// Tests backend : verifie la repartition exhaustive des incidents par type.

package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Verifie les volumes, parts et listes produits pour le rapport thematique.
class IncidentTypeReportBuilderTest {

  private final IncidentTypeReportBuilder builder =
    new IncidentTypeReportBuilder();

  @Test
  void shouldGroupIncidentsByTypeAndComputeExactShares() {
    List<Map<String, Object>> incidents = List.of(
      incident("Caisse", "OPEN"),
      incident("Caisse", "CLOSED"),
      incident("Acces", "REJECTED")
    );

    Map<String, Object> metrics = builder.build(incidents);

    assertThat(metrics)
      .containsEntry("reportContentType", "INCIDENT_TYPE_ANALYSIS")
      .containsEntry("totalIncidents", 3)
      .containsEntry("representedTypeCount", 2)
      .containsEntry("dominantType", "Caisse")
      .containsEntry("dominantTypeShare", 66.7);
    assertThat((List<?>) metrics.get("typeSections")).hasSize(2);
    assertThat(metrics.get("incidentsList")).isSameAs(incidents);
  }

  @Test
  void shouldNotCountCancelledIncidentsAsActive() {
    // ANNULE est sorti du circuit au meme titre que CLOTURE et REJETE : l'oublier
    // le faisait compter comme un incident encore actif.
    List<Map<String, Object>> incidents = List.of(
      incident("Caisse", "OPEN"),
      incident("Caisse", "CANCELLED"),
      incident("Caisse", "CLOSED"),
      incident("Caisse", "REJECTED")
    );

    Map<String, Object> metrics = builder.build(incidents);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> sections = (List<
      Map<String, Object>
    >) metrics.get("typeSections");
    assertThat(sections).hasSize(1);
    assertThat(sections.get(0)).containsEntry("active", 1L);
  }

  @Test
  void shouldKeepMissingTypesInAnExplicitFallbackGroup() {
    Map<String, Object> metrics = builder.build(
      List.of(Map.of("status", "OPEN"))
    );

    assertThat(metrics.get("distributionByType").toString())
      .contains("Non renseigne=1");
  }

  @Test
  void shouldReturnAnEmptyButValidReportForNoIncident() {
    Map<String, Object> metrics = builder.build(List.of());

    assertThat(metrics)
      .containsEntry("totalIncidents", 0)
      .containsEntry("representedTypeCount", 0)
      .containsEntry("dominantType", "-")
      .containsEntry("dominantTypeShare", 0.0);
  }

  // Cree un incident minimal representatif du contrat recu par reporting-service.
  private Map<String, Object> incident(String type, String status) {
    return Map.of(
      "status",
      status,
      "type",
      Map.of("id", type.toUpperCase(), "displayName", type)
    );
  }
}
