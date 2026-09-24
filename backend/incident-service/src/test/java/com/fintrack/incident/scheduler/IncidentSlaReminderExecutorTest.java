// Tests unitaires : securisent le RAPPEL SLA lui-meme (mutation de la date de rappel +
// notification et audit differes apres commit + rechargement). L'orchestration/lot est
// couverte par IncidentSlaReminderJobTest.

package com.fintrack.incident.scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class IncidentSlaReminderExecutorTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private NotificationClientService notificationClientService;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private IncidentSlaReminderExecutor executor;

  private Incident breachedIncident() {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTitle("Overdue incident");
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    return incident;
  }

  @Test
  @DisplayName("sendReminder - marks the reminder date, saves and reloads by id")
  void sendReminder_persistsReminder() {
    Incident incident = breachedIncident();
    when(incidentRepository.findById(incident.getId()))
      .thenReturn(Optional.of(incident));
    when(
      messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class))
    ).thenReturn("En cours");

    boolean result = executor.sendReminder(incident.getId());

    assertTrue(result);
    assertNotNull(incident.getLastSlaReminderSentAt());
    verify(incidentRepository).saveAndFlush(incident);
    // Hors transaction active : la notification part immediatement.
    verify(notificationClientService).notifySlaBreached(
      eq(incident),
      eq("En cours")
    );
  }

  @Test
  @DisplayName(
    "sendReminder - notification and audit are deferred until the transaction commits"
  )
  void sendReminder_emitsSideEffectsAfterCommit() {
    Incident incident = breachedIncident();
    when(incidentRepository.findById(incident.getId()))
      .thenReturn(Optional.of(incident));
    when(
      messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class))
    ).thenReturn("En cours");

    TransactionSynchronizationManager.initSynchronization();
    try {
      executor.sendReminder(incident.getId());

      verify(notificationClientService, never()).notifySlaBreached(
        any(),
        anyString()
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

      verify(notificationClientService).notifySlaBreached(eq(incident), eq("En cours"));
      verify(auditServiceClientService).audit(
        any(),
        eq("system"),
        any(),
        any(),
        eq("incident"),
        eq(incident.getId().toString()),
        eq("SUCCESS"),
        any()
      );
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName("sendReminder - returns false when the incident no longer exists")
  void sendReminder_returnsFalseWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(incidentRepository.findById(id)).thenReturn(Optional.empty());

    assertFalse(executor.sendReminder(id));

    verify(incidentRepository, never()).saveAndFlush(any());
    verify(notificationClientService, never()).notifySlaBreached(any(), anyString());
  }
}
