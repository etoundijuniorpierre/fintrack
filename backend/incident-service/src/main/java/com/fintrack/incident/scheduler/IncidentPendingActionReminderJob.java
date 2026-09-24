// Tache planifiee : relance les actions attendues apres traitement (resolution puis
// cloture). Chaque relance s'execute dans SA PROPRE transaction
// (IncidentPendingActionReminderExecutor).

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
public class IncidentPendingActionReminderJob {

  private final IncidentRepository incidentRepository;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;
  private final IncidentPendingActionReminderExecutor executor;

  // Delai avant relance, et intervalle entre deux relances.
  @Value("${incident.pending-action.reminder-interval-hours:4}")
  private long pendingActionReminderIntervalHours;

  // Interrupteur du canal interne. L'e-mail a le sien, porte par l'evenement.
  @Value("${incident.pending-action.internal-enabled:1}")
  private long pendingActionInternalEnabled;

  @Scheduled(cron = "0 5 * * * *") // Runs at minute 5 of every hour
  public void processPendingActions() {
    long intervalHours = Math.max(
      1L,
      reportingSystemConfigClientService.getThresholdLong(
        "pendingActionReminderIntervalHours",
        pendingActionReminderIntervalHours
      )
    );
    boolean internalEnabled =
      reportingSystemConfigClientService.getThresholdLong(
        "pendingActionInternalEnabled",
        pendingActionInternalEnabled
      ) ==
      1L;

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime cutoff = now.minusHours(intervalHours);
    List<Incident> pending =
      incidentRepository.findOverdueDecisionsWithoutRecentReminder(
        IncidentStatus.AWAITING_ACTION_STATUSES,
        cutoff,
        cutoff
      );

    int reminded = 0;
    for (Incident incident : pending) {
      try {
        // Transaction dediee : un echec ici est isole et n'emporte pas le reste du lot.
        if (executor.sendReminder(incident.getId(), now, internalEnabled)) {
          reminded++;
        }
      } catch (Exception e) {
        log.error(
          "Échec de la relance d'action attendue pour l'incident {}",
          incident.getId(),
          e
        );
      }
    }

    // Heartbeat systematique : distingue "rien a relancer" de "job qui ne tourne pas",
    // et rend visible un lot ou tout echoue (candidats > 0 mais relances = 0).
    log.info(
      "Relances d'actions attendues terminées. {} candidat(s) / {} relance(s).",
      pending.size(),
      reminded
    );
  }
}
