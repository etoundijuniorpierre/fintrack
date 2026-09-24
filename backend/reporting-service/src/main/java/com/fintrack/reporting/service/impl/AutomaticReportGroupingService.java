// Service de rapports automatiques : construit une vue distincte pour chaque entite demandee.
package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.client.incident.IncidentServiceClientService;
import com.fintrack.reporting.client.user.UserServiceClient;
import com.fintrack.reporting.client.user.UserServiceClientService;
import com.fintrack.reporting.client.user.dto.AgencyClientResponse;
import com.fintrack.reporting.client.user.dto.ServiceClientResponse;
import com.fintrack.reporting.client.user.dto.UserClientResponse;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.constant.ReportContentType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Regroupe un jeu d'incidents charge une seule fois par agence, service ou utilisateur.
@Service
@RequiredArgsConstructor
public class AutomaticReportGroupingService {

  private static final Set<String> TERMINAL_STATUSES = Set.of(
    "CLOSED",
    "REJECTED"
  );

  private final IncidentServiceClientService incidentServiceClientService;
  private final UserServiceClientService userServiceClientService;
  private final UserServiceClient userServiceClient;
  private final ObjectMapper objectMapper;
  private final MessageSource messageSource;
  private final IncidentTypeReportBuilder incidentTypeReportBuilder;
  private final IncidentStatusReportBuilder incidentStatusReportBuilder;

  // Enrichit un rapport automatique avec ses sous-rapports individuels.
  public void populate(GeneratedReport report, String groupBy) {
    UserClientResponse creator = userServiceClient.getUserById(
      report.getCreatedBy()
    );
    AccessScope access = resolveAccessScope(creator);
    List<Map<String, Object>> incidents =
      incidentServiceClientService.getIncidentsAsMaps(
        access.view(),
        List.of(),
        List.of(),
        List.of(),
        null,
        access.createdBy(),
        null,
        access.agencyId(),
        access.serviceId(),
        report.getPeriodStart() != null
          ? report.getPeriodStart().toLocalDate().toString()
          : null,
        report.getPeriodEnd() != null
          ? report.getPeriodEnd().toLocalDate().toString()
          : null
      );
    Set<String> requestedMetrics = extractRequestedMetrics(report);

    if ("own".equalsIgnoreCase(groupBy) || "all".equalsIgnoreCase(groupBy)) {
      Map<String, Object> single = contentMetrics(
        report,
        incidents,
        requestedMetrics
      );
      if (isStatusOverview(report)) {
        // Situation globale a la portee du rapport (agence/service/utilisateur du createur).
        single.put(
          "globalSituation",
          fetchGlobalSituation(
            access.view(),
            access.agencyId(),
            access.serviceId(),
            access.createdBy(),
            report
          )
        );
      }
      report.setMetrics(writeJson(single));
      return;
    }

    // Sous-ensemble explicitement choisi dans la modale (multi-selection) ; vide
    // => on retombe sur "toutes les entites autorisees" (comportement historique).
    Set<UUID> selected = switch (groupBy.toLowerCase(Locale.ROOT)) {
      case "agency" -> extractSelectedIds(report, "agencyIds");
      case "service" -> extractSelectedIds(report, "serviceIds");
      case "user" -> extractSelectedIds(report, "subjectUserIds");
      default -> Set.of();
    };

    List<Map<String, Object>> groups = switch (
      groupBy.toLowerCase(Locale.ROOT)
    ) {
      case "agency" -> groupByAgency(
        incidents,
        creator,
        access.global(),
        selected
      );
      case "service" -> groupByService(incidents, creator, access, selected);
      case "user" -> groupByUser(incidents, creator, access, selected);
      default -> throw new IllegalArgumentException(
        t("reporting.error.unsupported_automatic_grouping") + groupBy
      );
    };

    ReportContentType contentType = ReportContentType.orDefault(
      report.getContentType()
    );
    if (isThematicContent(contentType)) {
      groups = groups
        .stream()
        .filter(group -> {
          Object rawMetrics = group.get("metrics");
          if (!(rawMetrics instanceof Map<?, ?> metrics)) return false;
          Object rows = metrics.get("incidentsList");
          if (!(rows instanceof List<?> list) || list.isEmpty()) return false;
          @SuppressWarnings("unchecked")
          List<Map<String, Object>> incidentRows =
            (List<Map<String, Object>>) list;
          Map<String, Object> groupMetrics = buildThematic(
            contentType,
            incidentRows
          );
          if (contentType == ReportContentType.INCIDENT_STATUS_OVERVIEW) {
            // Situation globale propre a l'entite du groupe (agence/service/utilisateur).
            addGroupGlobalSituation(
              groupMetrics,
              groupBy,
              String.valueOf(group.get("id")),
              report
            );
          }
          group.put("metrics", groupMetrics);
          return true;
        })
        .toList();
    } else {
      groups = groups
        .stream()
        .map(group -> selectGroupMetrics(group, requestedMetrics))
        .toList();
    }

    Map<String, Object> metrics = new LinkedHashMap<>();
    metrics.put("groupBy", groupBy.toLowerCase(Locale.ROOT));
    metrics.put("groupCount", groups.size());
    metrics.put("groupedReports", groups);
    Set<String> unavailableMetrics = unavailableMetrics(requestedMetrics);
    if (!unavailableMetrics.isEmpty()) {
      metrics.put("unavailableMetrics", unavailableMetrics);
    }
    report.setMetrics(writeJson(metrics));
  }

  // Produit une section par agence autorisee.
  private List<Map<String, Object>> groupByAgency(
    List<Map<String, Object>> incidents,
    UserClientResponse creator,
    boolean global,
    Set<UUID> selected
  ) {
    return userServiceClientService
      .getAgencies()
      .stream()
      .filter(AgencyClientResponse::isActive)
      .filter(agency -> selected.isEmpty() || selected.contains(agency.getId()))
      .filter(agency -> global || agency.getId().equals(creator.getAgencyId()))
      .sorted(
        Comparator.comparing(
          AgencyClientResponse::getName,
          String.CASE_INSENSITIVE_ORDER
        )
      )
      .map(agency ->
        group(
          agency.getId(),
          agency.getName(),
          incidents
            .stream()
            .filter(row ->
              agency
                .getId()
                .toString()
                .equals(nestedId(row.get("agency")))
            )
            .toList()
        )
      )
      .toList();
  }

  // Produit une section par service autorise.
  private List<Map<String, Object>> groupByService(
    List<Map<String, Object>> incidents,
    UserClientResponse creator,
    AccessScope access,
    Set<UUID> selected
  ) {
    List<ServiceClientResponse> services =
      userServiceClientService.getServices();
    Set<UUID> permittedServiceIds = permittedServiceIds(
      services,
      incidents,
      creator,
      access
    );
    List<Map<String, Object>> groups = new ArrayList<>(
      services
        .stream()
        .filter(ServiceClientResponse::isActive)
        .filter(service -> selected.isEmpty() || selected.contains(service.getId()))
        .filter(service -> permittedServiceIds.contains(service.getId()))
        .sorted(
          Comparator.comparing(
            ServiceClientResponse::getName,
            String.CASE_INSENSITIVE_ORDER
          )
        )
        .map(service ->
          group(
            service.getId(),
            service.getName(),
            incidents
              .stream()
              .filter(row -> belongsToService(row, service.getId()))
              .toList()
          )
        )
        .toList()
    );
    // Le groupe "sans service" ne s'affiche que si aucune selection explicite.
    if (selected.isEmpty()) {
      List<Map<String, Object>> withoutService = incidents
        .stream()
        .filter(this::hasNoService)
        .toList();
      if (!withoutService.isEmpty()) {
        groups.add(
          group(
            "UNASSIGNED_SERVICE",
            t("report.group.unassignedService"),
            withoutService
          )
        );
      }
    }
    return groups;
  }

  // Determine les services visibles sans supposer qu'un service appartient directement a une agence.
  private Set<UUID> permittedServiceIds(
    List<ServiceClientResponse> services,
    List<Map<String, Object>> incidents,
    UserClientResponse creator,
    AccessScope access
  ) {
    if (access.global()) {
      return services
        .stream()
        .map(ServiceClientResponse::getId)
        .collect(Collectors.toSet());
    }
    if (access.agencyId() != null) {
      Set<UUID> serviceIds = new LinkedHashSet<>();
      userServiceClientService
        .getReportSubjects()
        .stream()
        .filter(UserClientResponse::isActive)
        .filter(
          user ->
            user.getAgencyId() != null &&
            access.agencyId().equals(user.getAgencyId().toString())
        )
        .map(UserClientResponse::getServiceId)
        .filter(Objects::nonNull)
        .forEach(serviceIds::add);
      incidents.forEach(row -> {
        addUuid(serviceIds, text(row.get("creatorServiceId")));
        addUuid(serviceIds, nestedId(row.get("transferredToService")));
      });
      return serviceIds;
    }
    return creator != null && creator.getServiceId() != null
      ? Set.of(creator.getServiceId())
      : Set.of();
  }

  // Ajoute un identifiant valide a un ensemble de dimensions.
  private void addUuid(Set<UUID> values, String candidate) {
    if (candidate.isBlank()) return;
    try {
      values.add(UUID.fromString(candidate));
    } catch (IllegalArgumentException ignored) {
      // Une reference invalide ne doit pas creer une section de rapport fantome.
    }
  }

  // Produit une section par utilisateur autorise.
  private List<Map<String, Object>> groupByUser(
    List<Map<String, Object>> incidents,
    UserClientResponse creator,
    AccessScope access,
    Set<UUID> selected
  ) {
    return userServiceClientService
      .getReportSubjects()
      .stream()
      .filter(UserClientResponse::isActive)
      .filter(user -> selected.isEmpty() || selected.contains(user.getId()))
      .filter(user -> canIncludeUser(user, creator, access))
      .sorted(
        Comparator.comparing(this::userLabel, String.CASE_INSENSITIVE_ORDER)
      )
      .map(user ->
        group(
          user.getId(),
          userLabel(user),
          incidents
            .stream()
            .filter(row -> belongsToUser(row, user.getId()))
            .toList()
        )
      )
      .toList();
  }

  // Assemble une section nommee avec ses propres indicateurs et incidents.
  private Map<String, Object> group(
    UUID id,
    String label,
    List<Map<String, Object>> incidents
  ) {
    return group(id.toString(), label, incidents);
  }

  // Assemble une section identifiee par une cle fonctionnelle non UUID.
  private Map<String, Object> group(
    String id,
    String label,
    List<Map<String, Object>> incidents
  ) {
    Map<String, Object> group = new LinkedHashMap<>();
    group.put("id", id);
    group.put("label", label);
    group.put("metrics", summarize(incidents));
    return group;
  }

  // Calcule les indicateurs independants de chaque groupe.
  private Map<String, Object> summarize(List<Map<String, Object>> incidents) {
    Map<String, Object> metrics = new LinkedHashMap<>();
    long closed = countStatus(incidents, "CLOSED");
    long rejected = countStatus(incidents, "REJECTED");
    metrics.put("totalIncidents", incidents.size());
    metrics.put(
      "activeIncidents",
      incidents
        .stream()
        .filter(
          row ->
            !TERMINAL_STATUSES.contains(
              text(row.get("status")).toUpperCase(Locale.ROOT)
            )
        )
        .count()
    );
    metrics.put("closedIncidents", closed);
    metrics.put("rejectedIncidents", rejected);
    metrics.put("avgClosureHours", averageHours(incidents, "closedAt"));
    metrics.put("avgResolutionHours", averageHours(incidents, "resolvedAt"));
    metrics.put("distributionByType", groupCount(incidents, "type"));
    metrics.put(
      "distributionByCriticality",
      groupCount(incidents, "criticality")
    );
    metrics.put("distributionByStatus", groupCount(incidents, "status"));
    metrics.put("monthlyClosures", monthlyClosures(incidents));
    metrics.put("topResolvers", topResolvers(incidents));
    metrics.put("incidentsList", incidents);
    return metrics;
  }

  // Selectionne le constructeur de contenu correspondant au modele demande.
  private Map<String, Object> contentMetrics(
    GeneratedReport report,
    List<Map<String, Object>> incidents,
    Set<String> requestedMetrics
  ) {
    ReportContentType contentType = ReportContentType.orDefault(
      report.getContentType()
    );
    return isThematicContent(contentType)
      ? buildThematic(contentType, incidents)
      : selectMetrics(summarize(incidents), requestedMetrics);
  }

  // Contenus thematiques analyse par statut ou par types
  private boolean isThematicContent(ReportContentType contentType) {
    return (
      contentType == ReportContentType.INCIDENT_TYPE_ANALYSIS ||
      contentType == ReportContentType.INCIDENT_STATUS_OVERVIEW
    );
  }

  // Construit le contenu thematique correspondant au modele demande.
  private Map<String, Object> buildThematic(
    ReportContentType contentType,
    List<Map<String, Object>> incidents
  ) {
    return contentType == ReportContentType.INCIDENT_STATUS_OVERVIEW
      ? incidentStatusReportBuilder.build(incidents)
      : incidentTypeReportBuilder.build(incidents);
  }

  // Indique si le rapport suit le modele "situation par statut".
  private boolean isStatusOverview(GeneratedReport report) {
    return (
      ReportContentType.orDefault(report.getContentType()) ==
      ReportContentType.INCIDENT_STATUS_OVERVIEW
    );
  }

  // Ajoute au groupe la situation globale scopee sur son entite (id non UUID ignore).
  private void addGroupGlobalSituation(
    Map<String, Object> metrics,
    String groupBy,
    String id,
    GeneratedReport report
  ) {
    UUID entityId = tryParseUuid(id);
    if (entityId == null) {
      return; // groupe fonctionnel (ex. "sans service") : pas de portee entite.
    }
    Map<String, Object> situation = switch (groupBy.toLowerCase(Locale.ROOT)) {
      case "agency" -> fetchGlobalSituation("agency", id, null, null, report);
      case "service" -> fetchGlobalSituation("service", null, id, null, report);
      case "user" -> fetchGlobalSituation("own", null, null, id, report);
      default -> null;
    };
    if (situation != null) {
      metrics.put("globalSituation", situation);
    }
  }

  // Recupere la situation globale (flux periode + backlog liste) pour une portee donnee.
  private Map<String, Object> fetchGlobalSituation(
    String view,
    String agencyId,
    String serviceId,
    String subjectUserId,
    GeneratedReport report
  ) {
    Map<String, Object> dashboard =
      incidentServiceClientService.getDashboardMetricsRaw(
        view,
        agencyId,
        serviceId,
        subjectUserId,
        report.getPeriodStart(),
        report.getPeriodEnd()
      );
    // Backlog courant (instantane) mis en forme par familles/retards.
    List<Map<String, Object>> backlog =
      incidentServiceClientService.getIncidentsAsMaps(
        view,
        GlobalSituationMetrics.BACKLOG_STATUSES,
        List.of(),
        List.of(),
        null,
        null,
        subjectUserId,
        agencyId,
        serviceId,
        null,
        null
      );
    // Listes de flux (traites/resolus/clotures) adossant les compteurs, enrichies "en retard".
    Map<String, List<Map<String, Object>>> periodActivity = new LinkedHashMap<>();
    incidentServiceClientService
      .getPeriodActivityAsMaps(
        view,
        agencyId,
        serviceId,
        subjectUserId,
        report.getPeriodStart(),
        report.getPeriodEnd()
      )
      .forEach((key, list) ->
        periodActivity.put(key, incidentStatusReportBuilder.enrich(list))
      );
    return GlobalSituationMetrics.merge(
      dashboard,
      incidentStatusReportBuilder.build(backlog),
      periodActivity
    );
  }

  private UUID tryParseUuid(String value) {
    try {
      return value != null ? UUID.fromString(value) : null;
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  // Limite une section groupee aux indicateurs effectivement demandes.
  private Map<String, Object> selectGroupMetrics(
    Map<String, Object> group,
    Set<String> requestedMetrics
  ) {
    Object rawMetrics = group.get("metrics");
    if (!(rawMetrics instanceof Map<?, ?> values)) return group;
    Map<String, Object> available = new LinkedHashMap<>();
    values.forEach((key, value) -> available.put(String.valueOf(key), value));
    Map<String, Object> selectedGroup = new LinkedHashMap<>(group);
    selectedGroup.put("metrics", selectMetrics(available, requestedMetrics));
    return selectedGroup;
  }

  // Conserve la liste des incidents et les seuls indicateurs selectionnes.
  private Map<String, Object> selectMetrics(
    Map<String, Object> available,
    Set<String> requestedMetrics
  ) {
    if (requestedMetrics.isEmpty()) return available;
    Map<String, Object> selected = new LinkedHashMap<>();
    requestedMetrics.forEach(metric -> {
      if (available.containsKey(metric)) {
        selected.put(metric, available.get(metric));
      }
    });
    if (available.containsKey("incidentsList")) {
      selected.put("incidentsList", available.get("incidentsList"));
    }
    return selected;
  }

  // Identifie les indicateurs non calculables dans les regroupements locaux.
  private Set<String> unavailableMetrics(Set<String> requestedMetrics) {
    if (requestedMetrics.isEmpty()) return Set.of();
    Set<String> unavailable = new LinkedHashSet<>(requestedMetrics);
    unavailable.removeAll(summarize(List.of()).keySet());
    unavailable.remove("incidentsList");
    return unavailable;
  }

  // Extrait les indicateurs demandes depuis le payload persiste du rapport.
  private Set<String> extractRequestedMetrics(GeneratedReport report) {
    if (report.getFilters() == null || report.getFilters().isBlank()) {
      return Set.of();
    }
    try {
      Map<String, Object> payload = objectMapper.readValue(
        report.getFilters(),
        new TypeReference<>() {}
      );
      Object rawMetrics = payload.get("requestedMetrics");
      if (!(rawMetrics instanceof Iterable<?> values)) return Set.of();
      Set<String> metrics = new LinkedHashSet<>();
      values.forEach(value -> {
        if (value instanceof String metric && !metric.isBlank()) {
          metrics.add(metric);
        }
      });
      return Set.copyOf(metrics);
    } catch (Exception ex) {
      return Set.of();
    }
  }

  // Determine le perimetre maximal autorise au createur de la planification.
  private AccessScope resolveAccessScope(UserClientResponse creator) {
    Set<String> permissions =
      creator != null && creator.getPermissions() != null
        ? creator
            .getPermissions()
            .stream()
            .map(value -> value.toUpperCase(Locale.ROOT))
            .collect(Collectors.toSet())
        : Set.of();
    if (permissions.contains("REPORT_GENERATE_ALL_SCOPES")) {
      return new AccessScope("all", null, null, null, true);
    }
    if (
      permissions.contains("REPORT_VIEW_AGENCY") &&
      creator.getAgencyId() != null
    ) {
      return new AccessScope(
        "agency",
        creator.getAgencyId().toString(),
        null,
        null,
        false
      );
    }
    if (
      permissions.contains("REPORT_VIEW_SERVICE") &&
      creator.getServiceId() != null
    ) {
      return new AccessScope(
        "service",
        null,
        creator.getServiceId().toString(),
        null,
        false
      );
    }
    return new AccessScope(
      "all",
      null,
      null,
      creator != null && creator.getId() != null
        ? creator.getId().toString()
        : reportCreatorFallback(),
      false
    );
  }

  // Fournit une valeur impossible quand le createur ne peut pas etre resolu.
  private String reportCreatorFallback() {
    return "00000000-0000-0000-0000-000000000000";
  }

  // Verifie qu'un utilisateur appartient au perimetre autorise.
  private boolean canIncludeUser(
    UserClientResponse user,
    UserClientResponse creator,
    AccessScope access
  ) {
    if (access.global()) return true;
    if (access.agencyId() != null) return (
      user.getAgencyId() != null &&
      access.agencyId().equals(user.getAgencyId().toString())
    );
    if (access.serviceId() != null) return (
      user.getServiceId() != null &&
      access.serviceId().equals(user.getServiceId().toString())
    );
    return (
      creator != null &&
      creator.getId() != null &&
      creator.getId().equals(user.getId())
    );
  }

  // Verifie l'appartenance d'un incident a un service source ou destinataire.
  private boolean belongsToService(Map<String, Object> row, UUID serviceId) {
    String expected = serviceId.toString();
    return (
      expected.equals(text(row.get("creatorServiceId"))) ||
      expected.equals(nestedId(row.get("transferredToService")))
    );
  }

  // Identifie les incidents qui ne peuvent etre rattaches a aucun service.
  private boolean hasNoService(Map<String, Object> row) {
    return (
      text(row.get("creatorServiceId")).isBlank() &&
      nestedId(row.get("transferredToService")).isBlank()
    );
  }

  // Verifie l'appartenance d'un incident a un utilisateur createur ou assigne.
  private boolean belongsToUser(Map<String, Object> row, UUID userId) {
    String expected = userId.toString();
    return (
      expected.equals(nestedId(row.get("createdBy"))) ||
      expected.equals(nestedId(row.get("assignedTo"))) ||
      containsId(row.get("participantUserIds"), expected)
    );
  }

  // Recherche un identifiant dans une collection serialisee par le client incident.
  private boolean containsId(Object value, String expected) {
    if (!(value instanceof Iterable<?> values)) return false;
    for (Object item : values) {
      if (expected.equals(text(item))) return true;
    }
    return false;
  }

  // Compte les incidents d'un statut donne.
  private long countStatus(List<Map<String, Object>> incidents, String status) {
    return incidents
      .stream()
      .filter(row -> status.equalsIgnoreCase(text(row.get("status"))))
      .count();
  }

  // Calcule la moyenne des durees creation -> jalon demande (closedAt ou resolvedAt).
  private double averageHours(
    List<Map<String, Object>> incidents,
    String milestoneField
  ) {
    return (
      Math.round(
        incidents
          .stream()
          .mapToDouble(row ->
            durationHours(row.get("createdAt"), row.get(milestoneField))
          )
          .filter(value -> value >= 0)
          .average()
          .orElse(0) * 10.0
      ) / 10.0
    );
  }

  // Calcule une duree en heures entre deux dates ISO.
  private double durationHours(Object start, Object end) {
    LocalDateTime from = parseDate(start);
    LocalDateTime to = parseDate(end);
    return from == null || to == null || to.isBefore(from)
      ? -1
      : Duration.between(from, to).toMinutes() / 60.0;
  }

  // Parse une date d'incident ISO locale ou UTC.
  private LocalDateTime parseDate(Object value) {
    if (value == null) return null;
    try {
      return LocalDateTime.parse(value.toString().replace("Z", ""));
    } catch (RuntimeException ex) {
      return null;
    }
  }

  // Compte les occurrences d'une dimension textuelle.
  private Map<String, Long> groupCount(
    List<Map<String, Object>> incidents,
    String key
  ) {
    Map<String, Long> counts = new LinkedHashMap<>();
    incidents.forEach(row -> counts.merge(label(row.get(key)), 1L, Long::sum));
    return counts;
  }

  // Regroupe les clotures par mois.
  private Map<String, Long> monthlyClosures(
    List<Map<String, Object>> incidents
  ) {
    Map<String, Long> counts = new LinkedHashMap<>();
    incidents.forEach(row -> {
      LocalDateTime closedAt = parseDate(row.get("closedAt"));
      if (
        closedAt != null && "CLOSED".equalsIgnoreCase(text(row.get("status")))
      ) {
        counts.merge(
          "%d-%02d".formatted(closedAt.getYear(), closedAt.getMonthValue()),
          1L,
          Long::sum
        );
      }
    });
    return counts;
  }

  // Classe les derniers resolveurs des incidents clotures du groupe.
  private List<Map<String, Object>> topResolvers(
    List<Map<String, Object>> incidents
  ) {
    Map<String, Long> counts = new LinkedHashMap<>();
    incidents
      .stream()
      .filter(row ->
        "CLOSED".equalsIgnoreCase(text(row.get("status")))
      )
      .forEach(row ->
        counts.merge(resolverLabel(row), 1L, Long::sum)
      );
    return counts
      .entrySet()
      .stream()
      .sorted(
        Map.Entry
          .<String, Long>comparingByValue()
          .reversed()
          .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER)
      )
      .limit(10)
      .map(entry ->
        Map.<String, Object>of(
          "name",
          entry.getKey(),
          "count",
          entry.getValue()
        )
      )
      .toList();
  }

  // Utilise l'auteur de la resolution; l'affectation ne sert que pour les anciennes donnees.
  private String resolverLabel(Map<String, Object> incident) {
    Object historyValue = incident.get("history");
    if (historyValue instanceof List<?> history) {
      String resolver = history
        .stream()
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .filter(entry ->
          "STATUS_CHANGE".equalsIgnoreCase(text(entry.get("action"))) &&
          "RESOLVED".equalsIgnoreCase(text(entry.get("newValue")))
        )
        .max(
          Comparator.comparing(
            entry -> parseDate(entry.get("createdAt")),
            Comparator.nullsFirst(Comparator.naturalOrder())
          )
        )
        .map(entry -> personLabel(entry.get("user")))
        .orElse("");
      if (!resolver.isBlank() && !"N/A".equals(resolver)) {
        return resolver;
      }
    }
    return personLabel(incident.get("assignedTo"));
  }

  // Construit un nom complet, puis revient au libelle technique disponible.
  private String personLabel(Object value) {
    if (value instanceof Map<?, ?> map) {
      String fullName = String.join(
        " ",
        List.of(text(map.get("firstName")), text(map.get("lastName")))
      ).trim();
      if (!fullName.isBlank()) {
        return fullName;
      }
    }
    return label(value);
  }

  // Extrait un identifiant depuis un objet DTO converti en dictionnaire.
  private String nestedId(Object value) {
    return value instanceof Map<?, ?> map ? text(map.get("id")) : "";
  }

  // Extrait un libelle lisible depuis une valeur simple ou imbriquee.
  private String label(Object value) {
    if (value instanceof Map<?, ?> map) {
      for (String key : List.of("displayName", "name", "username", "id")) {
        String candidate = text(map.get(key));
        if (!candidate.isBlank()) return candidate;
      }
    }
    String candidate = text(value);
    return candidate.isBlank() ? "N/A" : candidate;
  }

  // Construit le nom complet affiche d'un utilisateur.
  private String userLabel(UserClientResponse user) {
    String fullName = String.join(
      " ",
      List.of(text(user.getFirstName()), text(user.getLastName()))
    ).trim();
    return fullName.isBlank()
      ? user.getUsername()
      : fullName + " (" + user.getUsername() + ")";
  }

  // Convertit une valeur nullable en texte stable.
  private String text(Object value) {
    return value == null ? "" : value.toString();
  }

  // Serialise les metriques groupees du rapport.
  private String writeJson(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalStateException(
        t("reporting.error.serialize_grouped_metrics"),
        ex
      );
    }
  }

  // Traduit un libelle fonctionnel dans la langue active du rapport.
  private String t(String key) {
    return messageSource.getMessage(
      key,
      null,
      key,
      LocaleContextHolder.getLocale()
    );
  }

  // Extrait les IDs multi-selection depuis les filtres JSON du rapport.
  @SuppressWarnings("unchecked")
  private Set<UUID> extractSelectedIds(GeneratedReport report, String key) {
    try {
      String rawFilters = report.getFilters();
      if (rawFilters == null || rawFilters.isBlank()) return Set.of();
      Map<String, Object> payload = objectMapper.readValue(
        rawFilters, new TypeReference<Map<String, Object>>() {}
      );
      // Les identifiants d'entites sont imbriques sous "filters" (comme dans
      // buildIncidentCriteria) ; repli sur la racine par tolerance.
      Map<?, ?> filters =
        payload.get("filters") instanceof Map<?, ?> nested ? nested : payload;
      Object value = filters.get(key);
      if (value instanceof List<?> list) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (Object item : list) {
          if (item != null) {
            try {
              ids.add(UUID.fromString(item.toString()));
            } catch (IllegalArgumentException ignored) {}
          }
        }
        return ids;
      }
    } catch (Exception ignored) {}
    return Set.of();
  }

  // Porte le perimetre maximal de lecture du createur de la planification.
  private record AccessScope(
    String view,
    String agencyId,
    String serviceId,
    String createdBy,
    boolean global
  ) {}
}
