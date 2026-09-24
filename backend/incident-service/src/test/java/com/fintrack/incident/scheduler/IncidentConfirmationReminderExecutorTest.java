// Tests unitaires : securisent UNE relance de confirmation d'actualite (marquage,
// idempotence si l'entite source a repondu entre-temps, notification et audit).

package com.fintrack.incident.scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentConfirmationReminderExecutorTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private NotificationClientService notificationClientService;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @InjectMocks
  private IncidentConfirmationReminderExecutor executor;

  private Incident pendingIncident() {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTitle("Incident en attente de confirmation");
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(5));
    return incident;
  }

  @Test
  @DisplayName("sendReminder - marks the reminder date and notifies the waiting side")
  void sendReminder_persistsAndNotifies() {
    Incident incident = pendingIncident();
    when(incidentRepository.findById(incident.getId())).thenReturn(
      Optional.of(incident)
    );

    boolean result = executor.sendReminder(incident.getId(), LocalDateTime.now());

    assertTrue(result);
    assertNotNull(incident.getLastConfirmationReminderSentAt());
    verify(incidentRepository).saveAndFlush(incident);
    // Hors transaction active : la notification part immediatement.
    verify(notificationClientService).notifyConfirmationReminder(
      eq(incident),
      anyLong()
    );
  }

  @Test
  @DisplayName(
    "sendReminder - stays idempotent when the source entity answered in the meantime"
  )
  void sendReminder_answeredInTheMeantime_doesNothing() {
    Incident incident = pendingIncident();
    // Reponse arrivee entre la lecture du lot et ce commit : plus rien a relancer.
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    incident.setConfirmationRequestedAt(null);
    when(incidentRepository.findById(incident.getId())).thenReturn(
      Optional.of(incident)
    );

    boolean result = executor.sendReminder(incident.getId(), LocalDateTime.now());

    assertFalse(result);
    verify(incidentRepository, never()).saveAndFlush(any());
    verify(notificationClientService, never()).notifyConfirmationReminder(
      any(),
      anyLong()
    );
  }
}
