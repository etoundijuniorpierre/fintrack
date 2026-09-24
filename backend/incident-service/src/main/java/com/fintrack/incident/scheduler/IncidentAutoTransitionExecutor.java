// Execute UNE transition automatique dans SA PROPRE transaction, pour que l'echec
// d'un incident n'annule jamais le lot entier (cf. IncidentAutoTransitionJob).

package com.fintrack.incident.scheduler;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.audit.constant.AuditAction;
import com.fintrack.incident.client.audit.constant.AuditStatus;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.service.IncidentHistoryService;
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
public class IncidentAutoTransitionExecutor {

  private final IncidentRepository incidentRepository;
  private final IncidentHistoryService historyService;
  private final NotificationClientService notificationClientService;
  private final AuditServiceClientService auditServiceClientService;

  // Bascule un incident vers BLOCKED dans une transaction dediee (REQUIRES_NEW : isolee
  // du lot). Recharge et revalide l'etat pour rester idempotent si l'incident a bouge
  // entre la lecture du lot et ce commit. Renvoie true si la transition a ete persistee.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean block(UUID id, LocalDateTime now, long overdueWorkingDays) {
    Incident incident = incidentRepository.findById(id).orElse(null);
    if (
      incident == null ||
      IncidentAutoTransitionJob.AUTO_BLOCK_EXCLUDED_STATUSES.contains(
        incident.getStatus()
      )
    ) {
      return false;
    }
    IncidentStatus oldStatus = incident.getStatus();
    String reason =
      "Délai limite initial dépassé de plus de " +
      overdueWorkingDays +
      " jours ouvrés sans traitement";
    // Memorise le statut d'origine pour une reprise fidele (blocage SLA possible
    // depuis n'importe quel statut avant Traite).
    incident.setPreBlockStatus(oldStatus.name());
    incident.setStatus(IncidentStatus.BLOCKED);
    incident.setBlockedAt(now);
    incident.setBlockedBy(null);
    incident.setBlockedReason(reason);
    incident.setUnblockedAt(null);
    // saveAndFlush : fait remonter une eventuelle erreur de persistance ICI (dans cette
    // transaction isolee), et non a un commit de lot qui emporterait tout le reste.
    incidentRepository.saveAndFlush(incident);

    historyService.record(
      incident,
      null,
      ActionType.STATUS_CHANGE,
      oldStatus.name(),
      IncidentStatus.BLOCKED.name(),
      reason
    );

    // Notification et audit differes apres commit : l'audit ne doit refleter que des
    // transitions reellement persistees, sinon un rollback laisse des traces orphelines
    // que le run horaire suivant redupliquerait.
    notifyAfterCommit(
      () ->
        notificationClientService.notifyAutoStatusChanged(
          incident,
          null,
          "notification.incident.auto_blocked.comment",
          overdueWorkingDays
        )
    );
    notifyAfterCommit(
      () ->
        auditServiceClientService.audit(
          null,
          "system",
          List.of("SYSTEM"),
          AuditAction.INCIDENT_STATUS_CHANGE.getName(),
          "INCIDENT",
          incident.getId().toString(),
          AuditStatus.SUCCESS.getName(),
          Map.of(
            "incidentTitle", incident.getTitle(),
            "transition", oldStatus.name() + " -> BLOCKED (Auto SLA Breach)"
          )
        )
    );
    return true;
  }

  // Bascule un incident BLOCKED vers UNRESOLVED_PROLONGED_WAIT dans une transaction dediee.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean prolong(UUID id, long prolongedDays) {
    Incident incident = incidentRepository.findById(id).orElse(null);
    if (incident == null || incident.getStatus() != IncidentStatus.BLOCKED) {
      return false;
    }
    String reason =
      "Incident resté bloqué pendant plus de " + prolongedDays + " jours";
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    // On ne sort plus de l'attente prolongee par une reprise directe mais par une
    // confirmation d'actualite, qui remet l'incident en traitement : le statut
    // d'origine memorise pour la reprise devient obsolete, on le solde.
    incident.setPreBlockStatus(null);
    incidentRepository.saveAndFlush(incident);

    historyService.record(
      incident,
      null,
      ActionType.STATUS_CHANGE,
      IncidentStatus.BLOCKED.name(),
      IncidentStatus.UNRESOLVED_PROLONGED_WAIT.name(),
      reason
    );

    notifyAfterCommit(
      () ->
        notificationClientService.notifyAutoStatusChanged(
          incident,
          null,
          "notification.incident.auto_prolonged.comment",
          prolongedDays
        )
    );
    notifyAfterCommit(
      () ->
        auditServiceClientService.audit(
          null,
          "system",
          List.of("SYSTEM"),
          AuditAction.INCIDENT_STATUS_CHANGE.getName(),
          "INCIDENT",
          incident.getId().toString(),
          AuditStatus.SUCCESS.getName(),
          Map.of(
            "incidentTitle", incident.getTitle(),
            "transition",
            "BLOCKED -> UNRESOLVED_PROLONGED_WAIT (Auto Prolonged Blocked)"
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
