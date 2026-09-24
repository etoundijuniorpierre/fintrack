// Client inter-services : communique avec les services externes lies a incident service.

package com.fintrack.reporting.client.incident;

import com.fintrack.reporting.client.incident.dto.IncidentDashboardMetricsClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentTypeConfigClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentsPageClientResponse;
import com.fintrack.reporting.client.incident.dto.PeriodActivityClientResponse;
import com.fintrack.reporting.client.incident.fallback.IncidentServiceClientFallbackFactory;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Client Feign vers incident-service pour recuperer les metriques consolidees.
@FeignClient(
  name = "incident-service",
  url = "${incident.service.url}",
  fallbackFactory = IncidentServiceClientFallbackFactory.class
)
// Definit le contrat incident service attendu par les autres couches.
public interface IncidentServiceClient {
  @GetMapping("/api/v1/incidentService/dashboard/metrics")
  // Fournit dashboard indicateurs au cas d usage appelant.
  IncidentDashboardMetricsClientResponse getDashboardMetrics(
    @RequestParam("view") String view,
    @RequestParam(value = "agencyId", required = false) String agencyId,
    @RequestParam(value = "serviceId", required = false) String serviceId,
    @RequestParam(value = "period", required = false) String period
  );

  // Variante non typee pour la generation de rapports : ReportServiceImpl selectionne
  // dynamiquement les metriques par cle, il a besoin du jeu complet (et non de la
  // projection a 5 champs consommee par la gouvernance Super Admin).
  @GetMapping("/api/v1/incidentService/dashboard/metrics")
  Map<String, Object> getDashboardMetricsRaw(
    @RequestParam("view") String view,
    @RequestParam(value = "agencyId", required = false) String agencyId,
    @RequestParam(value = "serviceId", required = false) String serviceId,
    @RequestParam(value = "targetUserId", required = false) String targetUserId,
    @RequestParam(value = "period", required = false) String period,
    @RequestParam(value = "dateFrom", required = false) String dateFrom,
    @RequestParam(value = "dateTo", required = false) String dateTo
  );

  // Listes de flux (traites/resolus/clotures) adossant les compteurs de la periode.
  @GetMapping("/api/v1/incidentService/dashboard/period-activity")
  PeriodActivityClientResponse getPeriodActivity(
    @RequestParam("view") String view,
    @RequestParam(value = "agencyId", required = false) String agencyId,
    @RequestParam(value = "serviceId", required = false) String serviceId,
    @RequestParam(value = "targetUserId", required = false) String targetUserId,
    @RequestParam(value = "period", required = false) String period,
    @RequestParam(value = "dateFrom", required = false) String dateFrom,
    @RequestParam(value = "dateTo", required = false) String dateTo
  );

  // Fournit incident type configs a la couche appelante.

  @GetMapping("/api/v1/incidentService/incident-type-configs/all")
  List<IncidentTypeConfigClientResponse> getIncidentTypeConfigs();

  @GetMapping("/api/v1/incidentService/incidents")
  // Fournit les incidents au cas d usage appelant.
  IncidentsPageClientResponse getIncidents(
    @RequestParam("view") String view,
    @RequestParam(value = "status", required = false) List<String> statuses,
    @RequestParam(value = "type", required = false) List<String> types,
    @RequestParam(
      value = "criticality",
      required = false
    ) List<String> criticalities,
    @RequestParam(value = "assignedTo", required = false) String assignedTo,
    @RequestParam(value = "createdBy", required = false) String createdBy,
    @RequestParam(
      value = "subjectUserId",
      required = false
    ) String subjectUserId,
    @RequestParam(value = "agencyId", required = false) String agencyId,
    @RequestParam(value = "serviceId", required = false) String serviceId,
    @RequestParam(
      value = "subjectUserIds",
      required = false
    ) List<String> subjectUserIds,
    @RequestParam(value = "agencyIds", required = false) List<String> agencyIds,
    @RequestParam(
      value = "serviceIds",
      required = false
    ) List<String> serviceIds,
    @RequestParam(value = "startDate", required = false) String startDate,
    @RequestParam(value = "endDate", required = false) String endDate,
    @RequestParam(value = "page", defaultValue = "0") int page,
    @RequestParam(value = "size", defaultValue = "1000") int size
  );
}
