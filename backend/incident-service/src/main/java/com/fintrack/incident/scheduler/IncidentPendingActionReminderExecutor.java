// Execute UNE relance d'action attendue dans SA PROPRE transaction, pour que l'echec d'un
// incident n'annule jamais le lot entier (cf. IncidentPendingActionReminderJob).

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.audit.constant.AuditAction;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.IncidentAction;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.security.IncidentWorkflowGuard;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentPendingActionReminderExecutor {

  private final IncidentRepository incidentRepository;
  private final NotificationClientService notificationClientService;
  private final AuditServiceClientService auditServiceClientService;
  private final IncidentWorkflowGuard incidentWorkflowGuard;

  // Relance une action attendue dans une transaction dediee (REQUIRES_NEW : isolee du
  // lot). Recharge et revalide l'etat pour rester idempotent si l'action est arrivee
  // entre la lecture du lot et ce commit. Renvoie true si la relance a ete persistee.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean sendReminder(
    UUID id,
    LocalDateTime now,
    boolean internalEnabled
  ) {
    Incident incident = incidentRepository.findById(id).orElse(null);
    if (
      incident == null ||
      !IncidentStatus.AWAITING_ACTION_STATUSES.contains(incident.getStatus()) ||
      incident.getDecisionAwaitedSince() == null
    ) {
      return false;
    }

    // Traite attend une resolution, Resolu attend une cloture : on relance ceux que le
    // garde laisserait agir.
    IncidentAction awaited = incident.getStatus() == IncidentStatus.TREATED
      ? IncidentAction.RESOLVE
      : IncidentAction.CLOSE;
    Set<UUID> actors = incidentWorkflowGuard.resolveExpectedActors(
      incident,
      awaited
    );

    long waitingHours = Duration
      .between(incident.getDecisionAwaitedSince(), now)
      .toHours();

    incident.setLastDecisionReminderSentAt(now);
    // saveAndFlush : fait remonter une eventuelle erreur de persistance ICI (dans cette
    // transaction isolee), et non a un commit de lot qui emporterait tout le reste.
    incidentRepository.saveAndFlush(incident);

    // Notification et audit differes apres commit : ils ne doivent refleter qu'une
    // relance reellement persistee, sinon le run horaire suivant la redupliquerait.
    notifyAfterCommit(
      () ->
        notificationClientService.notifyPendingActionReminder(
          incident,
          actors,
          waitingHours,
          internalEnabled
        )
    );
    notifyAfterCommit(
      () ->
        auditServiceClientService.audit(
          null,
          "system",
          List.of("SYSTEM"),
          AuditAction.INCIDENT_SLA_REMINDER.getName(),
          "incident",
          incident.getId().toString(),
          "SUCCESS",
          Map.of(
            "incidentTitle", incident.getTitle(),
            "decisionAwaitedSince",
            String.valueOf(incident.getDecisionAwaitedSince()),
            "status", incident.getStatus().name()
          )
        )
    );
    return true;
  }

  // Diffuse l'evenement seulement lorsque la transaction metier est validee.
  private void notifyAfterCommit(Runnable notification) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      notification.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
      new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          notification.run();
        }
      }
    );
  }
}
