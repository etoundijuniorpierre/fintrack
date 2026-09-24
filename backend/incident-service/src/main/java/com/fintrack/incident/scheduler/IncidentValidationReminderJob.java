// Tache planifiee : relance les validations attendues depuis trop longtemps — sans ce
// circuit, un incident jamais valide ne serait surveille par personne. Chaque relance
// s'execute dans SA PROPRE transaction (IncidentValidationReminderExecutor).

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
public class IncidentValidationReminderJob {

  private final IncidentRepository incidentRepository;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;
  private final IncidentValidationReminderExecutor executor;

  // Delai avant relance, et intervalle entre deux relances.
  @Value("${incident.validation.delay-hours:48}")
  private long validationDelayHours;

  // Interrupteur Super Admin, sans effet sur le reste du workflow.
  @Value("${incident.validation.reminder-enabled:1}")
  private long validationReminderEnabled;

  @Scheduled(cron = "0 15 * * * *") // Runs at minute 15 of every hour
  public void processOverdueValidations() {
    if (
      reportingSystemConfigClientService.getThresholdLong(
        "validationReminderEnabled",
        validationReminderEnabled
      ) !=
      1L
    ) {
      log.debug("Relance des validations désactivée par la configuration.");
      return;
    }

    long delayHours = Math.max(
      1L,
      reportingSystemConfigClientService.getThresholdLong(
        "validationDelayHours",
        validationDelayHours
      )
    );

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime cutoff = now.minusHours(delayHours);
    List<Incident> overdue =
      incidentRepository.findOverdueDecisionsWithoutRecentReminder(
        IncidentStatus.AWAITING_VALIDATION_STATUSES,
        cutoff,
        cutoff
      );

    int reminded = 0;
    for (Incident incident : overdue) {
      try {
        // Transaction dediee : un echec ici est isole et n'emporte pas le reste du lot.
        if (executor.sendReminder(incident.getId(), now)) {
          reminded++;
        }
      } catch (Exception e) {
        log.error(
          "Échec de la relance de validation pour l'incident {}",
          incident.getId(),
          e
        );
      }
    }

    // Heartbeat systematique : distingue "rien a relancer" de "job qui ne tourne pas",
    // et rend visible un lot ou tout echoue (candidats > 0 mais relances = 0).
    log.info(
      "Relances de validation terminées. {} candidat(s) / {} relance(s).",
      overdue.size(),
      reminded
    );
  }
}
