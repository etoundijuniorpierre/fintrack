// Tache planifiee : execute les traitements automatiques lies a late incidents daily report job.

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Porte la responsabilite applicative liee a late inincidents daily rapport job.

@Slf4j
@Component
@RequiredArgsConstructor
public class LateIncidentsDailyReportJob {

  private final IncidentRepository incidentRepository;
  private final NotificationClientService notificationClientService;

  // S'execute tous les jours a 18h00
  @Scheduled(cron = "0 0 18 * * *")
  @Transactional(readOnly = true)
  public void sendLateIncidentsDailyReport() {
    log.info(
      "Démarrage de job de rapport quotidien pour incidents en retard..."
    );

    // Meme definition du retard que le tableau de bord et les relances SLA : sorti du
    // circuit, horloge arretee, ou decision de validation attendue. Sans ANNULE, un
    // incident annule figurait chaque soir dans le rapport, indefiniment ; sans les
    // attentes de validation, ce rapport declarait en retard des incidents que tout
    // le reste de l'application compte comme a l'heure.
    List<IncidentStatus> excludedStatuses = List.copyOf(
      IncidentStatus.NOT_LATE_STATUSES
    );

    // On utilise la meme methode que le SLA reminder pour recuperer les incidents en retard,
    // mais on pourrait aussi utiliser une methode findBySlaBreached() plus generique si on veut
    // tous les incidents en retard peu importe s'ils ont recu un rappel ou non.
    // Ici on prend arbitrairement les incidents dont la date limite est passee.
    LocalDateTime reminderCutoff = LocalDateTime.now();
    LocalDateTime currentDate = LocalDateTime.now();

    List<Incident> lateIncidents =
      incidentRepository.findBreachedSlaWithoutRecentReminder(
        excludedStatuses,
        currentDate,
        reminderCutoff
      );

    if (lateIncidents != null && !lateIncidents.isEmpty()) {
      try {
        notificationClientService.notifyLateIncidentsDailyReport(lateIncidents);
        log.info(
          "Rapport quotidien des incidents en retard envoyé avec {} incident(s).",
          lateIncidents.size()
        );
      } catch (Exception e) {
        log.error("Échec d'envoi de incidents en retard daily report", e);
      }
    } else {
      log.info(
        "Aucun incident en retard trouvé aujourd'hui. Rapport quotidien ignoré."
      );
    }
  }
}
