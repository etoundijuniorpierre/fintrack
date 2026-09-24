// Tache planifiee : relance les demandes d'actualite restees sans reponse, puis les
// escalade aux administrateurs. Chaque relance s'execute dans SA PROPRE transaction
// (IncidentConfirmationReminderExecutor) : l'echec d'un incident n'annule jamais le lot.

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
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
public class IncidentConfirmationReminderJob {

  private final IncidentRepository incidentRepository;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;
  private final IncidentConfirmationReminderExecutor executor;

  // Meme cadence que la relance d'un incident bloque : dans les deux cas un incident est
  // immobilise et on rappelle a intervalle fixe celui qui peut le debloquer.
  @Value("${incident.sla.reminder.blocked-interval-days:7}")
  private long blockedReminderIntervalDays;

  @Scheduled(cron = "0 45 * * * *") // Runs at minute 45 of every hour
  public void processPendingConfirmations() {
    long reminderDays = Math.max(
      1L,
      reportingSystemConfigClientService.getThresholdLong(
        "blockedReminderIntervalDays",
        blockedReminderIntervalDays
      )
    );

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime cutoff = now.minusDays(reminderDays);
    List<Incident> pending =
      incidentRepository.findPendingConfirmationsWithoutRecentReminder(
        IncidentStatus.UNRESOLVED_PROLONGED_WAIT,
        cutoff,
        cutoff
      );

    int reminded = 0;
    for (Incident incident : pending) {
      try {
        // Transaction dediee : un echec ici est isole et n'emporte pas le reste du lot.
        if (executor.sendReminder(incident.getId(), now)) {
          reminded++;
        }
      } catch (Exception e) {
        log.error(
          "Échec de la relance de confirmation d'actualité pour l'incident {}",
          incident.getId(),
          e
        );
      }
    }

    // Heartbeat systematique : distingue "rien a relancer" de "job qui ne tourne pas",
    // et rend visible un lot ou tout echoue (candidats > 0 mais relances = 0).
    log.info(
      "Relances de confirmation d'actualité terminées. {} candidat(s) / {} relance(s).",
      pending.size(),
      reminded
    );
  }
}
