// Execute UN rappel SLA dans SA PROPRE transaction, pour que l'echec d'un incident
// n'annule jamais le lot entier (cf. IncidentSlaReminderJob).

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.audit.constant.AuditAction;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentSlaReminderExecutor {

  private final IncidentRepository incidentRepository;
  private final NotificationClientService notificationClientService;
  private final AuditServiceClientService auditServiceClientService;
  private final MessageSource messageSource;

  private static final Locale DEFAULT_LOCALE = Locale.FRENCH;

  // Envoie le rappel SLA d'un incident dans une transaction dediee (REQUIRES_NEW : isolee
  // du lot). Recharge l'incident, marque la date de rappel, puis diffère notification et
  // audit apres le commit. Renvoie true si le rappel a ete persiste.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean sendReminder(UUID id) {
    Incident incident = incidentRepository.findById(id).orElse(null);
    if (incident == null) {
      return false;
    }
    String statusLabel = messageSource.getMessage(
      incident.getStatus().getNameKey(),
      null,
      incident.getStatus().getName(),
      DEFAULT_LOCALE
    );

    incident.setLastSlaReminderSentAt(LocalDateTime.now());
    // saveAndFlush : fait remonter une eventuelle erreur de persistance ICI (dans cette
    // transaction isolee), et non a un commit de lot qui emporterait tout le reste.
    incidentRepository.saveAndFlush(incident);

    // Notification et audit differes apres commit : le push (et donc le refetch frontend)
    // et la trace d'audit ne doivent refleter qu'un rappel reellement persiste.
    notifyAfterCommit(
      () -> notificationClientService.notifySlaBreached(incident, statusLabel)
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
            "dueDate", String.valueOf(incident.getDueDate()),
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
