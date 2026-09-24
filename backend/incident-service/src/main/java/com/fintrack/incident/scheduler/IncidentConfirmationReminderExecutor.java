// Execute UNE relance de confirmation d'actualite dans SA PROPRE transaction, pour que
// l'echec d'un incident n'annule jamais le lot entier (cf. IncidentConfirmationReminderJob).

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.audit.constant.AuditAction;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
public class IncidentConfirmationReminderExecutor {

  private final IncidentRepository incidentRepository;
  private final NotificationClientService notificationClientService;
  private final AuditServiceClientService auditServiceClientService;

  // Relance une demande d'actualite dans une transaction dediee (REQUIRES_NEW : isolee du
  // lot). Recharge et revalide l'etat pour rester idempotent si l'entite source a repondu
  // entre la lecture du lot et ce commit. Renvoie true si la relance a ete persistee.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean sendReminder(UUID id, LocalDateTime now) {
    Incident incident = incidentRepository.findById(id).orElse(null);
    if (
      incident == null ||
      incident.getStatus() != IncidentStatus.UNRESOLVED_PROLONGED_WAIT ||
      incident.getConfirmationRequestedAt() == null
    ) {
      return false;
    }

    long waitingDays = Duration
      .between(incident.getConfirmationRequestedAt(), now)
      .toDays();

    incident.setLastConfirmationReminderSentAt(now);
    // saveAndFlush : fait remonter une eventuelle erreur de persistance ICI (dans cette
    // transaction isolee), et non a un commit de lot qui emporterait tout le reste.
    incidentRepository.saveAndFlush(incident);

    // Notification et audit differes apres commit : ils ne doivent refleter qu'une
    // relance reellement persistee, sinon le run horaire suivant la redupliquerait.
    notifyAfterCommit(
      () ->
        notificationClientService.notifyConfirmationReminder(
          incident,
          waitingDays
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
            "confirmationRequestedAt",
            String.valueOf(incident.getConfirmationRequestedAt())
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
