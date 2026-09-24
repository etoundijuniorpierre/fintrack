// Tests unitaires : securisent la TRANSITION elle-meme (mutation + historique + effets
// differes apres commit + idempotence au rechargement). L'orchestration/lot est couverte
// par IncidentAutoTransitionJobTest.

package com.fintrack.incident.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.service.IncidentHistoryService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class IncidentAutoTransitionExecutorTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private IncidentHistoryService historyService;

  @Mock
  private NotificationClientService notificationClientService;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @InjectMocks
  private IncidentAutoTransitionExecutor executor;

  private Incident assignedIncident() {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTitle("Overdue assigned incident");
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setBlockedBy(UUID.randomUUID());
    incident.setUnblockedAt(LocalDateTime.now().minusDays(1));
    return incident;
  }

  @Test
  @DisplayName("block - persists the transition, records history and reloads by id")
  void block_persistsTransition() {
    Incident incident = assignedIncident();
    when(incidentRepository.findById(incident.getId()))
      .thenReturn(Optional.of(incident));

    boolean result = executor.block(incident.getId(), LocalDateTime.now(), 7L);

    assertTrue(result);
    assertEquals(IncidentStatus.BLOCKED, incident.getStatus());
    assertNull(incident.getBlockedBy());
    assertNull(incident.getUnblockedAt());
    assertEquals("ASSIGNED", incident.getPreBlockStatus());
    assertEquals(
      "Délai limite initial dépassé de plus de 7 jours ouvrés sans traitement",
      incident.getBlockedReason()
    );
    verify(incidentRepository).saveAndFlush(incident);
    verify(historyService).record(
      eq(incident),
      isNull(),
      eq(ActionType.STATUS_CHANGE),
      eq("ASSIGNED"),
      eq("BLOCKED"),
      eq("Délai limite initial dépassé de plus de 7 jours ouvrés sans traitement")
    );
    // Hors transaction active : la notification part immediatement.
    verify(notificationClientService).notifyAutoStatusChanged(
      eq(incident),
      isNull(),
      eq("notification.incident.auto_blocked.comment"),
      eq(7L)
    );
  }

  @Test
  @DisplayName(
    "block - notification and audit are deferred until the transaction commits"
  )
  void block_emitsSideEffectsAfterCommit() {
    Incident incident = assignedIncident();
    when(incidentRepository.findById(incident.getId()))
      .thenReturn(Optional.of(incident));

    TransactionSynchronizationManager.initSynchronization();
    try {
      executor.block(incident.getId(), LocalDateTime.now(), 7L);

      verify(notificationClientService, never()).notifyAutoStatusChanged(
        any(),
        any(),
        any(),
        any()
      );
      verify(auditServiceClientService, never()).audit(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      );

      TransactionSynchronizationManager.getSynchronizations()
        .forEach(TransactionSynchronization::afterCommit);

      verify(notificationClientService).notifyAutoStatusChanged(
        eq(incident),
        isNull(),
        eq("notification.incident.auto_blocked.comment"),
        eq(7L)
      );
      verify(auditServiceClientService).audit(
        isNull(),
        eq("system"),
        eq(java.util.List.of("SYSTEM")),
        any(),
        eq("INCIDENT"),
        eq(incident.getId().toString()),
        any(),
        any()
      );
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName(
    "block - idempotent : does nothing if the incident is already in an excluded status"
  )
  void block_idempotentWhenAlreadyExcluded() {
    Incident incident = assignedIncident();
    incident.setStatus(IncidentStatus.BLOCKED);
    when(incidentRepository.findById(incident.getId()))
      .thenReturn(Optional.of(incident));

    assertFalse(executor.block(incident.getId(), LocalDateTime.now(), 7L));

    verify(incidentRepository, never()).saveAndFlush(any());
    verify(historyService, never()).record(
      any(),
      any(),
      any(),
      any(),
      any(),
      any()
    );
  }

  @Test
  @DisplayName("block - returns false when the incident no longer exists")
  void block_returnsFalseWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(incidentRepository.findById(id)).thenReturn(Optional.empty());

    assertFalse(executor.block(id, LocalDateTime.now(), 7L));

    verify(incidentRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName(
    "prolong - moves a long-blocked incident to UNRESOLVED_PROLONGED_WAIT"
  )
  void prolong_movesBlockedToProlongedWait() {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTitle("Prolonged blocked incident");
    incident.setStatus(IncidentStatus.BLOCKED);
    incident.setPreBlockStatus(IncidentStatus.IN_PROGRESS.name());
    when(incidentRepository.findById(incident.getId()))
      .thenReturn(Optional.of(incident));

    boolean result = executor.prolong(incident.getId(), 30L);

    assertTrue(result);
    assertEquals(
      IncidentStatus.UNRESOLVED_PROLONGED_WAIT,
      incident.getStatus()
    );
    assertNull(incident.getPreBlockStatus());
    verify(incidentRepository).saveAndFlush(incident);
    verify(historyService).record(
      eq(incident),
      isNull(),
      eq(ActionType.STATUS_CHANGE),
      eq("BLOCKED"),
      eq("UNRESOLVED_PROLONGED_WAIT"),
      eq("Incident resté bloqué pendant plus de 30 jours")
    );
    verify(notificationClientService).notifyAutoStatusChanged(
      eq(incident),
      isNull(),
      eq("notification.incident.auto_prolonged.comment"),
      eq(30L)
    );
  }
}
