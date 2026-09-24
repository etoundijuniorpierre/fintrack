// Tests unitaires : verifie le regroupement par famille de statut et les retards.

package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IncidentStatusReportBuilderTest {

  private final IncidentStatusReportBuilder builder =
    new IncidentStatusReportBuilder();

  private static final String PAST = "2020-01-01T00:00:00";
  private static final String FUTURE = "2999-01-01T00:00:00";

  @Test
  @DisplayName(
    "build - Groups incidents into status families and counts overdue ones"
  )
  void build_GroupsByFamilyAndCountsOverdue() {
    List<Map<String, Object>> incidents = new ArrayList<>();
    incidents.add(incident("OPEN", PAST)); // NEW + en retard
    incidents.add(incident("DRAFT", PAST)); // PENDING_DIRECTION, attente de validation
    incidents.add(incident("IN_PROGRESS", FUTURE)); // IN_PROGRESS, a temps
    incidents.add(incident("IN_PROGRESS", PAST)); // IN_PROGRESS + en retard
    incidents.add(incident("BLOCKED", PAST)); // BLOCKED + en retard
    incidents.add(incident("TREATED", PAST)); // RESOLVED, non actif -> pas en retard
    incidents.add(incident("RESOLVED", PAST)); // RESOLVED, non actif -> pas en retard
    incidents.add(incident("CLOSED", null)); // CLOSED
    incidents.add(incident("REJECTED", null)); // REJECTED
    incidents.add(incident("CANCELLED", null)); // REJECTED
    incidents.add(incident("UNRESOLVED_PROLONGED_WAIT", PAST)); // actif + en retard

    Map<String, Object> result = builder.build(incidents);

    assertThat(result.get("reportContentType")).isEqualTo(
      "INCIDENT_STATUS_OVERVIEW"
    );
    assertThat(result.get("totalIncidents")).isEqualTo(11);
    // OPEN, IN_PROGRESS, BLOCKED et l'attente prolongee sont echus et encore actifs.
    // TRAITE et RESOLU ont leur horloge arretee, DRAFT attend une validation, ANNULE et
    // REJETE sont sortis du circuit. Meme definition que
    // IncidentStatus.NOT_LATE_STATUSES cote incident-service.
    assertThat(result.get("overdueCount")).isEqualTo(4L);

    Map<String, Integer> counts = countsByKey(result);
    assertThat(counts).containsEntry("NEW", 1);
    assertThat(counts).containsEntry("PENDING_DIRECTION", 1);
    assertThat(counts).containsEntry("IN_PROGRESS", 2);
    // L'attente prolongee appartient a la famille BLOQUE dans ce rapport.
    assertThat(counts).containsEntry("BLOCKED", 2);
    assertThat(counts).containsEntry("RESOLVED", 2);
    assertThat(counts).containsEntry("CLOSED", 1);
    assertThat(counts).containsEntry("REJECTED", 2);

    assertThat(incidents).allSatisfy(incident ->
      assertThat(incident).doesNotContainKey("overdue")
    );
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> enriched = (List<Map<String, Object>>) result.get(
      "incidentsList"
    );
    assertThat(enriched).allSatisfy(incident ->
      assertThat(incident).containsKey("overdue")
    );
  }

  @Test
  @DisplayName("build - Keeps all seven families even when empty")
  void build_KeepsEmptyFamilies() {
    Map<String, Object> result = builder.build(List.of());

    assertThat(result.get("totalIncidents")).isEqualTo(0);
    assertThat(result.get("overdueCount")).isEqualTo(0L);
    assertThat((Collection<?>) result.get("categorySections")).hasSize(7);
    assertThat(countsByKey(result).values()).allMatch(count -> count == 0);
  }

  private Map<String, Object> incident(String status, String dueDate) {
    Map<String, Object> incident = new HashMap<>();
    incident.put("status", status);
    incident.put("dueDate", dueDate);
    return incident;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Integer> countsByKey(Map<String, Object> result) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (Object section : (Collection<?>) result.get("categorySections")) {
      Map<String, Object> map = (Map<String, Object>) section;
      counts.put(
        String.valueOf(map.get("key")),
        ((Number) map.get("count")).intValue()
      );
    }
    return counts;
  }
}
