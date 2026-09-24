// Tache planifiee : orchestre les rappels SLA. Chaque rappel s'execute dans SA PROPRE
// transaction (IncidentSlaReminderExecutor) : l'echec d'un incident n'annule jamais le lot.

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentSlaReminderJob {

  private final IncidentRepository incidentRepository;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;
  private final IncidentSlaReminderExecutor executor;

  @Value("${incident.sla.reminder.interval-hours:24}")
  private long slaReminderIntervalHours;

  @Value("${incident.sla.reminder.blocked-interval-days:7}")
  private long blockedReminderIntervalDays;

  // S'execute toutes les heures pour verifier les SLA depasses.
  @Scheduled(cron = "0 0 * * * *")
  public void processBreachedSlaReminders() {
    long slaReminderIntervalHours = Math.max(
      1L,
      reportingSystemConfigClientService.getThresholdLong(
        "slaReminderIntervalHours",
        this.slaReminderIntervalHours
      )
    );
    LocalDateTime currentDate = LocalDateTime.now();

    // --- 1. Standard Reminders (active non-blocked breached incidents, default 24h) ---
    LocalDateTime standardReminderCutoff = currentDate.minusHours(slaReminderIntervalHours);
    // Derive de la reference : un incident sorti du circuit (annule compris) ne
    // recoit plus rien. S'y ajoutent les etats de blocage, relances par leur propre
    // circuit juste en dessous.
    List<IncidentStatus> standardExcludedStatuses = Stream.concat(
      IncidentStatus.SLA_CLOCK_STOPPED_STATUSES.stream(),
      Stream.concat(
        IncidentStatus.AWAITING_VALIDATION_STATUSES.stream(),
        Stream.of(
          IncidentStatus.BLOCKED,
          IncidentStatus.UNRESOLVED_PROLONGED_WAIT
        )
      )
    ).toList();

    List<Incident> standardBreachedIncidents =
      incidentRepository.findBreachedSlaWithoutRecentReminder(
        standardExcludedStatuses,
        currentDate,
        standardReminderCutoff
      );

    int standardCount = processAndSendReminders(standardBreachedIncidents);

    // --- 2. Blocked Reminders (BLOCKED incidents only, intervalle configurable, defaut 7 jours) ---
    long blockedReminderIntervalDays = Math.max(
      1L,
      reportingSystemConfigClientService.getThresholdLong(
        "blockedReminderIntervalDays",
        this.blockedReminderIntervalDays
      )
    );
    LocalDateTime blockedReminderCutoff = currentDate.minusDays(
      blockedReminderIntervalDays
    );
    // Ce rappel ne vise QUE les incidents bloques : on exclut le complement plutot
    // que de reenumerer les autres statuts a la main — cette liste avait deja oublie
    // ANNULE et TRAITE, qui recevaient donc des rappels de blocage.
    List<IncidentStatus> blockedExcludedStatuses = List.copyOf(
      EnumSet.complementOf(EnumSet.of(IncidentStatus.BLOCKED))
    );

    List<Incident> blockedBreachedIncidents =
      incidentRepository.findBreachedSlaWithoutRecentReminder(
        blockedExcludedStatuses,
        currentDate,
        blockedReminderCutoff
      );

    int blockedCount = processAndSendReminders(blockedBreachedIncidents);

    // Heartbeat systematique : distingue "aucun SLA a rappeler" de "job qui ne tourne pas",
    // et rend visible un lot ou tout echoue (candidats > 0 mais rappels = 0).
    log.info(
      "Job de rappel SLA terminé. Standard : {} candidat(s) / {} rappel(s). Bloqué : {} candidat(s) / {} rappel(s).",
      standardBreachedIncidents.size(),
      standardCount,
      blockedBreachedIncidents.size(),
      blockedCount
    );
  }

  private int processAndSendReminders(List<Incident> incidents) {
    int count = 0;
    for (Incident incident : incidents) {
      try {
        // Transaction dediee : un echec ici est isole et n'emporte pas le reste du lot.
        if (executor.sendReminder(incident.getId())) {
          count++;
        }
      } catch (Exception e) {
        log.error(
          "Échec d'envoi de rappel SLA pour incident ID: {}",
          incident.getId(),
          e
        );
      }
    }
    return count;
  }
}
