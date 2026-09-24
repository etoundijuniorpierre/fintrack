// Tests unitaires : securisent UNE relance d'action attendue (destinataire selon l'etape,
// idempotence si la decision est arrivee entre-temps, coupure du canal interne).

package com.fintrack.incident.scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.IncidentAction;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.security.IncidentWorkflowGuard;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentPendingActionReminderExecutorTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private NotificationClientService notificationClientService;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @Mock
  private IncidentWorkflowGuard incidentWorkflowGuard;

  @InjectMocks
  private IncidentPendingActionReminderExecutor executor;

  private Incident awaiting(IncidentStatus status) {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTitle("Incident en attente d'action");
    incident.setStatus(status);
    incident.setDecisionAwaitedSince(LocalDateTime.now().minusHours(6));
    return incident;
  }

  @Test
  @DisplayName("sendReminder - Treated chases whoever may resolve")
  void sendReminder_treated_chasesTheResolver() {
    Incident incident = awaiting(IncidentStatus.TREATED);
    UUID resolver = UUID.randomUUID();
    when(incidentRepository.findById(incident.getId())).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentWorkflowGuard.resolveExpectedActors(
        incident,
        IncidentAction.RESOLVE
      )
    ).thenReturn(Set.of(resolver));

    boolean result = executor.sendReminder(
      incident.getId(),
      LocalDateTime.now(),
      true
    );

    assertTrue(result);
    assertNotNull(incident.getLastDecisionReminderSentAt());
    verify(notificationClientService).notifyPendingActionReminder(
      eq(incident),
      eq(Set.of(resolver)),
      anyLong(),
      eq(true)
    );
  }

  @Test
  @DisplayName("sendReminder - Resolved chases whoever may close")
  void sendReminder_resolved_chasesTheCloser() {
    Incident incident = awaiting(IncidentStatus.RESOLVED);
    UUID closer = UUID.randomUUID();
    when(incidentRepository.findById(incident.getId())).thenReturn(
      Optional.of(incident)
    );
    when(
      incidentWorkflowGuard.resolveExpectedActors(
        incident,
        IncidentAction.CLOSE
      )
    ).thenReturn(Set.of(closer));

    executor.sendReminder(incident.getId(), LocalDateTime.now(), false);

    // Canal interne coupe : l'information passe jusqu'a la notification, qui tranche.
    verify(notificationClientService).notifyPendingActionReminder(
      eq(incident),
      eq(Set.of(closer)),
      anyLong(),
      eq(false)
    );
  }

  @Test
  @DisplayName(
    "sendReminder - stays idempotent when the decision arrived in the meantime"
  )
  void sendReminder_decisionTaken_doesNothing() {
    Incident incident = awaiting(IncidentStatus.TREATED);
    incident.setStatus(IncidentStatus.CLOSED);
    when(incidentRepository.findById(incident.getId())).thenReturn(
      Optional.of(incident)
    );

    boolean result = executor.sendReminder(
      incident.getId(),
      LocalDateTime.now(),
      true
    );

    assertFalse(result);
    verify(incidentRepository, never()).saveAndFlush(any());
    verify(notificationClientService, never()).notifyPendingActionReminder(
      any(),
      any(),
      anyLong(),
      anyBoolean()
    );
  }
}
