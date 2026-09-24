// Relance a intervalle regulier les incidents critiques encore ouverts.

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentCriticalReminderJob {

  private final IncidentRepository incidentRepository;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;
  private final IncidentCriticalReminderExecutor executor;

  // Intervalle entre deux relances, et delai avant la premiere.
  @Value("${incident.critical.reminder-interval-hours:4}")
  private long criticalReminderIntervalHours;

  @Scheduled(cron = "0 15 * * * *") // Runs at minute 15 of every hour
  public void processCriticalReminders() {
    long intervalHours = Math.max(
      1L,
      reportingSystemConfigClientService.getThresholdLong(
        "criticalReminderIntervalHours",
        criticalReminderIntervalHours
      )
    );

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime cutoff = now.minusHours(intervalHours);
    List<Incident> candidates =
      incidentRepository.findCriticalOpenWithoutRecentReminder(
        Criticality.CRITICAL,
        IncidentStatus.OPEN_STATUSES,
        cutoff,
        cutoff
      );

    int reminded = 0;
    for (Incident incident : candidates) {
      try {
        // Transaction dediee : un echec ici est isole et n'emporte pas le reste du lot.
        if (executor.sendReminder(incident.getId(), now)) {
          reminded++;
        }
      } catch (Exception e) {
        log.error(
          "Échec de la relance de l'incident critique {}",
          incident.getId(),
          e
        );
      }
    }

    // Heartbeat : distingue « rien a relancer » de « job qui ne tourne pas ».
    log.info(
      "Relances d'incidents critiques terminées. {} candidat(s) / {} relance(s).",
      candidates.size(),
      reminded
    );
  }
}
