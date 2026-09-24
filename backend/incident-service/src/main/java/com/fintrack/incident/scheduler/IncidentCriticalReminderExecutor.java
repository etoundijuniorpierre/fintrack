// Execute UNE relance dans SA PROPRE transaction : un echec n'emporte pas le lot.

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.audit.constant.AuditAction;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.Criticality;
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
public class IncidentCriticalReminderExecutor {

  private final IncidentRepository incidentRepository;
  private final NotificationClientService notificationClientService;
  private final AuditServiceClientService auditServiceClientService;

  // Revalide l'etat : l'incident a pu etre resolu ou requalifie depuis la lecture du lot.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean sendReminder(UUID id, LocalDateTime now) {
    Incident incident = incidentRepository.findById(id).orElse(null);
    if (
      incident == null ||
      incident.getCriticality() != Criticality.CRITICAL ||
      !IncidentStatus.OPEN_STATUSES.contains(incident.getStatus())
    ) {
      return false;
    }

    long openHours = incident.getCreatedAt() == null
      ? 0
      : Duration.between(incident.getCreatedAt(), now).toHours();

    incident.setLastCriticalReminderSentAt(now);
    // saveAndFlush : l'erreur de persistance remonte ici, pas au commit du lot.
    incidentRepository.saveAndFlush(incident);

    // Differes apres commit : ne refleter qu'une relance reellement persistee.
    notifyAfterCommit(
      () ->
        notificationClientService.notifyCriticalIncidentReminder(
          incident,
          openHours
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
            "criticality", Criticality.CRITICAL.name(),
            "openHours", String.valueOf(openHours),
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
