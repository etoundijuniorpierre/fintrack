// Constructeur de rapport : regroupe une population d'incidents par famille de
// statut metier et signale ceux en retard. Rapport volontairement simple.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.model.constant.IncidentStatusSets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

// Produit la synthese "situation par statut" 
@Component
public class IncidentStatusReportBuilder {

  // Familles metier, dans l'ordre d'affichage du rapport
  private enum Category {
    NEW(Set.of("OPEN", "PENDING_VALIDATION", "VALIDATED", "REOPENED")),
    PENDING_DIRECTION(Set.of("DRAFT")),
    IN_PROGRESS(Set.of("ASSIGNED", "IN_PROGRESS", "TRANSFERRED")),
    BLOCKED(Set.of("BLOCKED", "UNRESOLVED_PROLONGED_WAIT")),
    RESOLVED(Set.of("TREATED", "RESOLVED")),
    CLOSED(Set.of("CLOSED")),
    REJECTED(Set.of("REJECTED", "CANCELLED"));

    private final Set<String> statuses;

    Category(Set<String> statuses) {
      this.statuses = statuses;
    }
  }

  // Les deux services doivent dire la meme chose de "en retard", sinon le rapport et le
  // tableau de bord affichent deux chiffres pour la meme question.
  private static final Set<String> ACTIVE_STATUSES =
    IncidentStatusSets.CLOCK_RUNNING;

  // Enrichit chaque incident du drapeau "overdue" (encore ouvert et echeance passee),
  // sans le rattacher a une famille : sert aux listes de flux (traites/resolus/clotures).
  public List<Map<String, Object>> enrich(List<Map<String, Object>> incidents) {
    LocalDateTime now = LocalDateTime.now();
    List<Map<String, Object>> enriched = new ArrayList<>(
      incidents != null ? incidents.size() : 0
    );
    if (incidents != null) {
      for (Map<String, Object> incident : incidents) {
        String status = text(incident.get("status")).toUpperCase(Locale.ROOT);
        Map<String, Object> copy = new LinkedHashMap<>(incident);
        copy.put("overdue", isOverdue(status, incident.get("dueDate"), now));
        enriched.add(copy);
      }
    }
    return enriched;
  }

  // Construit le contenu a partir de la population deja chargee.
  public Map<String, Object> build(List<Map<String, Object>> incidents) {
    List<Map<String, Object>> enrichedIncidents = enrich(incidents);

    Map<Category, List<Map<String, Object>>> buckets = new LinkedHashMap<>();
    for (Category category : Category.values()) {
      buckets.put(category, new ArrayList<>());
    }

    long overdueCount = 0;
    for (Map<String, Object> enriched : enrichedIncidents) {
      String status = text(enriched.get("status")).toUpperCase(Locale.ROOT);
      buckets.get(categoryOf(status)).add(enriched);
      if (Boolean.TRUE.equals(enriched.get("overdue"))) overdueCount++;
    }

    int total = enrichedIncidents.size();
    List<Map<String, Object>> categorySections = new ArrayList<>();
    for (Category category : Category.values()) {
      List<Map<String, Object>> bucket = buckets.get(category);
      Map<String, Object> section = new LinkedHashMap<>();
      section.put("key", category.name());
      section.put("count", bucket.size());
      section.put(
        "share",
        total == 0 ? 0.0 : Math.round(bucket.size() * 1000.0 / total) / 10.0
      );
      section.put("incidents", bucket);
      categorySections.add(section);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("reportContentType", "INCIDENT_STATUS_OVERVIEW");
    result.put("totalIncidents", total);
    result.put("overdueCount", overdueCount);
    result.put("categorySections", categorySections);
    result.put("incidentsList", enrichedIncidents);
    return result;
  }

  // Rattache un statut a sa famille ; un statut inconnu reste comptabilise
  // (rattache aux nouveaux) pour ne jamais perdre un incident du total.
  private Category categoryOf(String status) {
    for (Category category : Category.values()) {
      if (category.statuses.contains(status)) return category;
    }
    return Category.NEW;
  }

  // Un incident est en retard s'il est encore ouvert et que son echeance est passee.
  private boolean isOverdue(String status, Object dueDate, LocalDateTime now) {
    if (!ACTIVE_STATUSES.contains(status)) return false;
    LocalDateTime due = parseDateTime(dueDate);
    return due != null && due.isBefore(now);
  }

  // Lecture tolerante de l'echeance (LocalDateTime, offset ou date seule).
  private LocalDateTime parseDateTime(Object value) {
    if (value == null) return null;
    String text = value.toString().trim();
    if (text.isEmpty()) return null;
    try {
      return LocalDateTime.parse(text);
    } catch (DateTimeParseException ignored) {
      // format suivant
    }
    try {
      return OffsetDateTime.parse(text).toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      // format suivant
    }
    try {
      return LocalDate.parse(text).atStartOfDay();
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  private String text(Object value) {
    return value != null ? String.valueOf(value).trim() : "";
  }
}
