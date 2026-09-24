// Constructeur de rapport : regroupe une population d'incidents par type.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.model.constant.IncidentStatusSets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

// Produit la synthese, les parts et les listes detaillees du rapport par type.
@Component
public class IncidentTypeReportBuilder {

  // Construit un contenu thematique coherent a partir de la liste deja chargee.
  public Map<String, Object> build(List<Map<String, Object>> incidents) {
    List<Map<String, Object>> safeIncidents = incidents != null
      ? incidents
      : List.of();
    Map<String, TypeBucket> buckets = new LinkedHashMap<>();
    safeIncidents.forEach(incident -> {
      TypeIdentity type = typeIdentity(incident.get("type"));
      buckets
        .computeIfAbsent(type.id(), ignored -> new TypeBucket(type))
        .incidents()
        .add(incident);
    });

    List<Map<String, Object>> typeSections = buckets
      .values()
      .stream()
      .sorted(
        Comparator.<TypeBucket>comparingInt(bucket -> bucket.incidents().size())
          .reversed()
          .thenComparing(bucket -> bucket.type().name(), String.CASE_INSENSITIVE_ORDER)
      )
      .map(bucket -> toSection(bucket, safeIncidents.size()))
      .toList();

    Map<String, Long> distribution = new LinkedHashMap<>();
    typeSections.forEach(section ->
      distribution.put(
        String.valueOf(section.get("typeName")),
        ((Number) section.get("count")).longValue()
      )
    );

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("reportContentType", "INCIDENT_TYPE_ANALYSIS");
    result.put("totalIncidents", safeIncidents.size());
    result.put("representedTypeCount", typeSections.size());
    result.put("dominantType", typeSections.isEmpty() ? "-" : typeSections.get(0).get("typeName"));
    result.put("dominantTypeShare", typeSections.isEmpty() ? 0.0 : typeSections.get(0).get("share"));
    result.put("distributionByType", distribution);
    result.put("typeSections", typeSections);
    result.put("incidentsList", safeIncidents);
    return result;
  }

  // Assemble les indicateurs et incidents d'un type donne.
  private Map<String, Object> toSection(TypeBucket bucket, int total) {
    List<Map<String, Object>> incidents = bucket.incidents();
    Map<String, Object> section = new LinkedHashMap<>();
    section.put("typeId", bucket.type().id());
    section.put("typeName", bucket.type().name());
    section.put("count", incidents.size());
    section.put("share", total == 0 ? 0.0 : Math.round(incidents.size() * 1000.0 / total) / 10.0);
    section.put("active", countActive(incidents));
    section.put("closed", countStatus(incidents, "CLOSED"));
    section.put("rejected", countStatus(incidents, "REJECTED"));
    section.put("incidents", incidents);
    return section;
  }

  // Compte les incidents encore dans le circuit. Derive de la reference : ANNULE
  // manquait a la liste ecrite ici et comptait donc comme actif.
  private long countActive(List<Map<String, Object>> incidents) {
    return incidents
      .stream()
      .filter(incident ->
        !IncidentStatusSets.TERMINAL.contains(
          text(incident.get("status")).toUpperCase(Locale.ROOT)
        )
      )
      .count();
  }

  // Compte les incidents d'un statut terminal donne.
  private long countStatus(List<Map<String, Object>> incidents, String status) {
    return incidents
      .stream()
      .filter(incident -> status.equalsIgnoreCase(text(incident.get("status"))))
      .count();
  }

  // Extrait un identifiant et un libelle stables depuis le DTO incident.
  private TypeIdentity typeIdentity(Object value) {
    if (value instanceof Map<?, ?> map) {
      String id = text(map.get("id"));
      String name = firstNonBlank(text(map.get("displayName")), text(map.get("name")), id);
      return new TypeIdentity(firstNonBlank(id, name, "UNKNOWN"), firstNonBlank(name, "Non renseigne"));
    }
    String name = text(value);
    return new TypeIdentity(firstNonBlank(name, "UNKNOWN"), firstNonBlank(name, "Non renseigne"));
  }

  // Retourne la premiere valeur textuelle exploitable.
  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) return value;
    }
    return "";
  }

  // Convertit une valeur brute en texte non nul.
  private String text(Object value) {
    return value != null ? String.valueOf(value).trim() : "";
  }

  private record TypeIdentity(String id, String name) {}

  private record TypeBucket(TypeIdentity type, List<Map<String, Object>> incidents) {
    private TypeBucket(TypeIdentity type) {
      this(type, new ArrayList<>());
    }
  }
}
