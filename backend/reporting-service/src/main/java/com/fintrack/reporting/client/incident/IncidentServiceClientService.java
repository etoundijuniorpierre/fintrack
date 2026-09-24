// Client inter-services : communique avec les services externes lies a incident service client.

package com.fintrack.reporting.client.incident;

import com.fintrack.reporting.client.incident.dto.IncidentClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentDashboardMetricsClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentTypeConfigClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentsPageClientResponse;
import com.fintrack.reporting.client.incident.dto.PeriodActivityClientResponse;
import com.fintrack.reporting.model.readmodel.EscalationIncident;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Service applicatif leger autour du client incident-service.
@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceClientService {

  private final IncidentServiceClient incidentServiceClient;
  private final ObjectMapper objectMapper;
  private final MessageSource messageSource;

  private String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  // Taille de page lors de la recuperation d'incidents pour les agregats Super Admin (reglage infra).
  @Value("${fintrack.superadmin.incident-fetch-size:1000}")
  private int incidentFetchSize;

  // Fournit tableau de bord metrics a la couche appelante.

  public IncidentDashboardMetricsClientResponse getDashboardMetrics(
    String view,
    String agencyId,
    String serviceId
  ) {
    try {
      return getDashboardMetricsOrThrow(view, agencyId, serviceId);
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger les metriques d incident pour report scope {}",
        normalizeView(view),
        ex
      );
      return new IncidentDashboardMetricsClientResponse(
        0,
        0,
        0.0,
        0.0,
        0.0,
        List.of(),
        List.of()
      );
    }
  }

  // Fournit tableau de bord metrics or throw a la couche appelante.

  public IncidentDashboardMetricsClientResponse getDashboardMetricsOrThrow(
    String view,
    String agencyId,
    String serviceId
  ) {
    String safeView = normalizeView(view);
    IncidentDashboardMetricsClientResponse metrics =
      incidentServiceClient.getDashboardMetrics(
        safeView,
        agencyId,
        serviceId,
        "ALL"
      );
    return metrics != null
      ? metrics
      : new IncidentDashboardMetricsClientResponse(
          0,
          0,
          0.0,
          0.0,
          0.0,
          List.of(),
          List.of()
        );
  }

  // Fournit les incidents au cas d usage appelant.
  public List<IncidentClientResponse> getIncidents(
    String view,
    List<String> statuses,
    List<String> types,
    List<String> criticalities,
    String assignedTo,
    String createdBy,
    String subjectUserId,
    String agencyId,
    String serviceId,
    String startDate,
    String endDate
  ) {
    try {
      return getIncidentsOrThrow(
        view,
        statuses,
        types,
        criticalities,
        assignedTo,
        createdBy,
        subjectUserId,
        agencyId,
        serviceId,
        startDate,
        endDate
      );
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger les incidents pour report scope {}",
        normalizeView(view),
        ex
      );
      return new ArrayList<>();
    }
  }

  // Surcharge mono-valeur : delegue en laissant les filtres multi-valeurs vides.
  public List<IncidentClientResponse> getIncidentsOrThrow(
    String view,
    List<String> statuses,
    List<String> types,
    List<String> criticalities,
    String assignedTo,
    String createdBy,
    String subjectUserId,
    String agencyId,
    String serviceId,
    String startDate,
    String endDate
  ) {
    return getIncidentsOrThrow(
      view,
      statuses,
      types,
      criticalities,
      assignedTo,
      createdBy,
      subjectUserId,
      agencyId,
      serviceId,
      null,
      null,
      null,
      startDate,
      endDate
    );
  }

  // Fournit les incidents ou leve l'erreur metier correspondante.
  public List<IncidentClientResponse> getIncidentsOrThrow(
    String view,
    List<String> statuses,
    List<String> types,
    List<String> criticalities,
    String assignedTo,
    String createdBy,
    String subjectUserId,
    String agencyId,
    String serviceId,
    List<String> subjectUserIds,
    List<String> agencyIds,
    List<String> serviceIds,
    String startDate,
    String endDate
  ) {
    String safeView = normalizeView(view);
    List<IncidentClientResponse> incidents = new ArrayList<>();
    int page = 0;
    int totalPages;
    do {
      IncidentsPageClientResponse response = incidentServiceClient.getIncidents(
        safeView,
        statuses,
        types,
        criticalities,
        assignedTo,
        createdBy,
        subjectUserId,
        agencyId,
        serviceId,
        subjectUserIds,
        agencyIds,
        serviceIds,
        startDate,
        endDate,
        page,
        incidentFetchSize
      );
      if (response == null) {
        throw new IllegalStateException(
          t("reporting.error.incident_service_no_response")
        );
      }
      if (response.getContent() == null) {
        break;
      }
      incidents.addAll(response.getContent());
      totalPages = response.getTotalPages();
      page++;
    } while (page < totalPages);
    return incidents;
  }

  // Surcharge mono-valeur : delegue en laissant les filtres multi-valeurs vides.
  public List<Map<String, Object>> getIncidentsAsMaps(
    String view,
    List<String> statuses,
    List<String> types,
    List<String> criticalities,
    String assignedTo,
    String createdBy,
    String subjectUserId,
    String agencyId,
    String serviceId,
    String startDate,
    String endDate
  ) {
    return getIncidentsAsMaps(
      view,
      statuses,
      types,
      criticalities,
      assignedTo,
      createdBy,
      subjectUserId,
      agencyId,
      serviceId,
      null,
      null,
      null,
      startDate,
      endDate
    );
  }

  // Fournit les incidents sous forme de dictionnaires pour la generation de rapport.
  public List<Map<String, Object>> getIncidentsAsMaps(
    String view,
    List<String> statuses,
    List<String> types,
    List<String> criticalities,
    String assignedTo,
    String createdBy,
    String subjectUserId,
    String agencyId,
    String serviceId,
    List<String> subjectUserIds,
    List<String> agencyIds,
    List<String> serviceIds,
    String startDate,
    String endDate
  ) {
    return objectMapper.convertValue(
      getIncidentsOrThrow(
        view,
        statuses,
        types,
        criticalities,
        assignedTo,
        createdBy,
        subjectUserId,
        agencyId,
        serviceId,
        subjectUserIds,
        agencyIds,
        serviceIds,
        startDate,
        endDate
      ),
      new TypeReference<>() {}
    );
  }

  // Fournit escalation incidents a la couche appelante.

  public List<EscalationIncident> getEscalationIncidents() {
    return getIncidents(
      "all",
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    )
      .stream()
      .map(this::toEscalationIncident)
      .toList();
  }

  // Fournit tableau de bord metrics raw a la couche appelante.

  public Map<String, Object> getDashboardMetricsRaw(
    String view,
    String agencyId,
    String serviceId,
    String targetUserId,
    LocalDateTime periodStart,
    LocalDateTime periodEnd
  ) {
    String safeView = normalizeView(view);
    // Periode du rapport transmise au tableau de bord en mode CUSTOM ; sinon le
    // service incident retombe sur sa fenetre par defaut (30 jours) et ignore la periode choisie.
    boolean customRange = periodStart != null && periodEnd != null;
    String period = customRange ? "CUSTOM" : null;
    String dateFrom = customRange ? periodStart.toString() : null;
    String dateTo = customRange ? periodEnd.toString() : null;
    try {
      Map<String, Object> metrics =
        incidentServiceClient.getDashboardMetricsRaw(
          safeView,
          agencyId,
          serviceId,
          targetUserId,
          period,
          dateFrom,
          dateTo
        );
      return metrics != null ? metrics : Map.of();
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger les metriques brutes d incident pour report scope {}",
        safeView,
        ex
      );
      return Map.of();
    }
  }

  // Listes de flux (traites/resolus/clotures) adossant les compteurs de la periode,
  // dans la meme portee et la meme fenetre que le tableau de bord. Cle -> liste de maps.
  public Map<String, List<Map<String, Object>>> getPeriodActivityAsMaps(
    String view,
    String agencyId,
    String serviceId,
    String targetUserId,
    LocalDateTime periodStart,
    LocalDateTime periodEnd
  ) {
    String safeView = normalizeView(view);
    boolean customRange = periodStart != null && periodEnd != null;
    String period = customRange ? "CUSTOM" : null;
    String dateFrom = customRange ? periodStart.toString() : null;
    String dateTo = customRange ? periodEnd.toString() : null;
    Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
    try {
      PeriodActivityClientResponse response =
        incidentServiceClient.getPeriodActivity(
          safeView,
          agencyId,
          serviceId,
          targetUserId,
          period,
          dateFrom,
          dateTo
        );
      result.put(
        "treated",
        toMaps(response != null ? response.getTreated() : null)
      );
      result.put(
        "resolved",
        toMaps(response != null ? response.getResolved() : null)
      );
      result.put(
        "closed",
        toMaps(response != null ? response.getClosed() : null)
      );
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger l activite de periode pour report scope {}",
        safeView,
        ex
      );
      result.put("treated", new ArrayList<>());
      result.put("resolved", new ArrayList<>());
      result.put("closed", new ArrayList<>());
    }
    return result;
  }

  // Convertit une liste d'incidents client en liste de dictionnaires pour le rapport.
  private List<Map<String, Object>> toMaps(
    List<IncidentClientResponse> incidents
  ) {
    if (incidents == null || incidents.isEmpty()) {
      return new ArrayList<>();
    }
    return objectMapper.convertValue(
      incidents,
      new TypeReference<List<Map<String, Object>>>() {}
    );
  }

  // Fournit incident type configs a la couche appelante.

  public List<IncidentTypeConfigClientResponse> getIncidentTypeConfigs() {
    try {
      return getIncidentTypeConfigsOrThrow();
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de charger les configurations de types d incident",
        ex
      );
      return List.of();
    }
  }

  // Fournit incident type configs or throw a la couche appelante.

  public List<IncidentTypeConfigClientResponse> getIncidentTypeConfigsOrThrow() {
    List<IncidentTypeConfigClientResponse> configs =
      incidentServiceClient.getIncidentTypeConfigs();
    return configs == null ? List.of() : configs;
  }

  // Normalise les valeurs du domaine incident service client avant traitement.

  private String normalizeView(String view) {
    if (view == null || view.isBlank()) {
      return "own";
    }
    return switch (view.toLowerCase()) {
      case "global", "byglobal" -> "all";
      case "byagency" -> "agency";
      case "byservice" -> "service";
      default -> view;
    };
  }

  // Convertit les donnees du domaine incident service client entre les modeles utilises.

  private EscalationIncident toEscalationIncident(
    IncidentClientResponse incident
  ) {
    return EscalationIncident.builder()
      .reference(incident.getReference())
      .title(incident.getTitle())
      .criticality(incident.getCriticality())
      .status(incident.getStatus())
      .transferCount(
        incident.getTransferCount() != null ? incident.getTransferCount() : 0
      )
      .createdAt(
        incident.getCreatedAt() != null
          ? incident.getCreatedAt().toString()
          : null
      )
      .build();
  }

}
