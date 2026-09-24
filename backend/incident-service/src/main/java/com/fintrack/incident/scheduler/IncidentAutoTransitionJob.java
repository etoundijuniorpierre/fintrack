// Tache planifiee : orchestre les transitions automatiques de statut d'incidents suite a SLA
// depasse ou blocage prolonge. Chaque transition s'execute dans SA PROPRE transaction
// (IncidentAutoTransitionExecutor) : l'echec d'un incident n'annule jamais le lot entier.

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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
public class IncidentAutoTransitionJob {

  private final IncidentRepository incidentRepository;
  private final ReportingSystemConfigClientService reportingSystemConfigClientService;
  private final IncidentAutoTransitionExecutor executor;

  // Nombre de jours ouvres (hors dimanche) de depassement du delai avant blocage automatique.
  @Value("${incident.auto-block.overdue-working-days:7}")
  private long autoBlockOverdueWorkingDays;

  // Nombre de jours de blocage continu avant passage en attente prolongee.
  @Value("${incident.prolonged-wait.days:30}")
  private long prolongedWaitDays;

  // Date a partir de laquelle un incident est eligible a l'attente prolongee, sur sa
  // date de declaration. Vide = aucune borne, tout le stock est eligible (comportement
  // d'origine). Renseignee, elle reserve le dispositif aux incidents declares ensuite :
  // la sortie d'attente prolongee passe desormais par une confirmation d'actualite de
  // l'entite source, et rien ne dit qu'on veuille imposer cette demarche a un backlog
  // ancien qui basculerait en bloc au premier passage du job.
  @Value("${incident.prolonged-wait.applies-from:}")
  private String prolongedWaitAppliesFrom;

  // Le blocage systeme (SLA) s'applique a tout statut ANTERIEUR a "Traite" : on exclut donc
  // les incidents traites/termines et ceux deja places dans un etat de blocage.
  // PENDING_VALIDATION est exclu aussi : le delai de traitement ne court pas tant que
  // l'incident n'est pas entre dans le circuit, personne ne peut encore le traiter.
  public static final List<IncidentStatus> AUTO_BLOCK_EXCLUDED_STATUSES =
    Stream.concat(
      IncidentStatus.SLA_CLOCK_STOPPED_STATUSES.stream(),
      Stream.concat(
        IncidentStatus.AWAITING_VALIDATION_STATUSES.stream(),
        Stream.of(
          IncidentStatus.BLOCKED,
          IncidentStatus.UNRESOLVED_PROLONGED_WAIT
        )
      )
    ).toList();

  @Scheduled(cron = "0 30 * * * *") // Runs at minute 30 of every hour
  public void processAutoTransitions() {
    LocalDateTime now = LocalDateTime.now();
    long overdueWorkingDays = Math.max(
      1L,
      reportingSystemConfigClientService.getThresholdLong(
        "autoBlockOverdueWorkingDays",
        autoBlockOverdueWorkingDays
      )
    );
    long prolongedDays = Math.max(
      1L,
      reportingSystemConfigClientService.getThresholdLong(
        "prolongedWaitDays",
        prolongedWaitDays
      )
    );

    // 1. Transition active -> BLOCKED si le delai est depasse de >= N jours ouvres (hors dimanche).
    List<Incident> breachedIncidents =
      incidentRepository.findBreachedActiveIncidents(
        AUTO_BLOCK_EXCLUDED_STATUSES,
        now
      );

    int blockableCount = 0;
    int blockedCount = 0;
    for (Incident incident : breachedIncidents) {
      if (incident.getDueDate() == null) {
        continue;
      }
      if (
        getDaysBetweenExcludingSundays(incident.getDueDate(), now) <
        overdueWorkingDays
      ) {
        continue;
      }
      blockableCount++;
      try {
        // Transaction dediee : un echec ici est isole et n'emporte pas le reste du lot.
        if (executor.block(incident.getId(), now, overdueWorkingDays)) {
          blockedCount++;
        }
      } catch (Exception e) {
        log.error(
          "Échec de la transition automatique de l'incident {} vers BLOCKED",
          incident.getId(),
          e
        );
      }
    }

    // 2. Transition BLOCKED -> UNRESOLVED_PROLONGED_WAIT si bloque depuis >= N jours.
    LocalDateTime prolongedCutoff = now.minusDays(prolongedDays);
    LocalDateTime appliesFrom = prolongedWaitAppliesFrom();
    List<Incident> blockedIncidents =
      incidentRepository.findBlockedIncidentsOlderThan(
        IncidentStatus.BLOCKED,
        prolongedCutoff,
        appliesFrom
      );

    int prolongedCount = 0;
    for (Incident incident : blockedIncidents) {
      try {
        if (executor.prolong(incident.getId(), prolongedDays)) {
          prolongedCount++;
        }
      } catch (Exception e) {
        log.error(
          "Échec de la transition automatique de l'incident {} vers UNRESOLVED_PROLONGED_WAIT",
          incident.getId(),
          e
        );
      }
    }

    // Heartbeat systematique : distingue "job qui n'a rien a faire" de "job qui ne tourne pas",
    // et rend visible un lot ou tout echoue (candidats > 0 mais transitions = 0).
    log.info(
      "Transitions automatiques terminées. Retard: {} candidats / {} bloqués. Blocage prolongé: {} candidats / {} en attente prolongée.",
      blockableCount,
      blockedCount,
      blockedIncidents.size(),
      prolongedCount
    );
  }

  // Compte les jours entre deux dates en excluant les dimanches.
  public static long getDaysBetweenExcludingSundays(
    LocalDateTime start,
    LocalDateTime end
  ) {
    if (start == null || end == null || start.isAfter(end)) {
      return 0;
    }
    long days = 0;
    LocalDate current = start.toLocalDate();
    LocalDate endDate = end.toLocalDate();
    while (current.isBefore(endDate)) {
      current = current.plusDays(1);
      if (current.getDayOfWeek() != DayOfWeek.SUNDAY) {
        days++;
      }
    }
    return days;
  }

  // Borne de declaration du dispositif d'attente prolongee. Accepte une date ou un
  // horodatage ISO. Une valeur illisible est traitee comme absente, et signalee : mieux
  // vaut un dispositif qui tourne sur tout le stock qu'un dispositif muet.
  private LocalDateTime prolongedWaitAppliesFrom() {
    if (prolongedWaitAppliesFrom == null || prolongedWaitAppliesFrom.isBlank()) {
      return null;
    }
    String value = prolongedWaitAppliesFrom.trim();
    try {
      return value.length() == 10
        ? LocalDate.parse(value).atStartOfDay()
        : LocalDateTime.parse(value);
    } catch (DateTimeParseException e) {
      log.warn(
        "incident.prolonged-wait.applies-from illisible ({}) : borne ignorée, tous les incidents restent éligibles",
        prolongedWaitAppliesFrom
      );
      return null;
    }
  }
}
