// Client inter-services : trace la cause des replis vers incident-service.

package com.fintrack.reporting.client.incident.fallback;

import com.fintrack.reporting.client.incident.IncidentServiceClient;
import com.fintrack.reporting.client.incident.dto.IncidentDashboardMetricsClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentTypeConfigClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentsPageClientResponse;
import com.fintrack.reporting.client.incident.dto.PeriodActivityClientResponse;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

// Cree un repli observable sans transformer une indisponibilite en absence d'incidents.
@Slf4j
@Component
public class IncidentServiceClientFallbackFactory
  implements FallbackFactory<IncidentServiceClient> {

  // Construit le client de repli et conserve la cause technique dans les journaux.
  @Override
  public IncidentServiceClient create(Throwable cause) {
    log.warn("Appel a incident-service bascule en repli", cause);
    return new IncidentServiceClient() {
      // Retourne des metriques degradees lorsque leur absence est acceptable.
      @Override
      public IncidentDashboardMetricsClientResponse getDashboardMetrics(
        String view,
        String agencyId,
        String serviceId,
        String period
      ) {
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

      // Retourne un dictionnaire vide pour les vues de supervision degradables.
      @Override
      public Map<String, Object> getDashboardMetricsRaw(
        String view,
        String agencyId,
        String serviceId,
        String targetUserId,
        String period,
        String dateFrom,
        String dateTo
      ) {
        return Map.of();
      }

      // Listes de flux vides : le repli ne fabrique pas de population fictive.
      @Override
      public PeriodActivityClientResponse getPeriodActivity(
        String view,
        String agencyId,
        String serviceId,
        String targetUserId,
        String period,
        String dateFrom,
        String dateTo
      ) {
        return PeriodActivityClientResponse.builder()
          .treated(List.of())
          .resolved(List.of())
          .closed(List.of())
          .build();
      }

      // Retourne une collection vide pour les referentiels degradables.
      @Override
      public List<IncidentTypeConfigClientResponse> getIncidentTypeConfigs() {
        return List.of();
      }

      // Signale l'indisponibilite afin qu'un rapport ne soit jamais produit vide par erreur.
      @Override
      public IncidentsPageClientResponse getIncidents(
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
        String endDate,
        int page,
        int size
      ) {
        return null;
      }
    };
  }
}
