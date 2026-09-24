// Tests unitaires : securisent UNE relance de validation (marquage, idempotence si la
// validation est arrivee entre-temps, et le piege des deux sens de "Attente validation
// Direction").

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
class IncidentValidationReminderExecutorTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private NotificationClientService notificationClientService;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @InjectMocks
  private IncidentValidationReminderExecutor executor;

  private Incident awaitingValidation(IncidentStatus status) {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTitle("Incident en attente de validation");
    incident.setStatus(status);
    incident.setDecisionAwaitedSince(LocalDateTime.now().minusHours(72));
    return incident;
  }

  @Test
  @DisplayName("sendReminder - marks the reminder date and notifies the validator")
  void sendReminder_persistsAndNotifies() {
    Incident incident = awaitingValidation(IncidentStatus.PENDING_VALIDATION);
    when(incidentRepository.findById(incident.getId())).thenReturn(
      Optional.of(incident)
    );

    boolean result = executor.sendReminder(
      incident.getId(),
      LocalDateTime.now()
    );

    assertTrue(result);
    assertNotNull(incident.getLastDecisionReminderSentAt());
    verify(incidentRepository).saveAndFlush(incident);
    // Hors transaction active : la notification part immediatement.
    verify(notificationClientService).notifyValidationReminder(
      eq(incident),
      anyLong()
    );
  }

  @Test
  @DisplayName(
    "sendReminder - stays idempotent when the validation arrived in the meantime"
  )
  void sendReminder_validatedInTheMeantime_doesNothing() {
    Incident incident = awaitingValidation(IncidentStatus.PENDING_VALIDATION);
    incident.setStatus(IncidentStatus.VALIDATED);
    when(incidentRepository.findById(incident.getId())).thenReturn(
      Optional.of(incident)
    );

    boolean result = executor.sendReminder(
      incident.getId(),
      LocalDateTime.now()
    );

    assertFalse(result);
    verify(incidentRepository, never()).saveAndFlush(any());
    verify(notificationClientService, never()).notifyValidationReminder(
      any(),
      anyLong()
    );
  }

  @Test
  @DisplayName(
    "sendReminder - a solution rejected by the Direction is chased too, at the handler"
  )
  void sendReminder_directionRejected_isChasedAsWell() {
    // Meme statut, sens inverse : avec un motif de rejet, l'incident n'attend plus la
    // Direction mais que le traitant resoumette. L'attente est reelle, elle se relance
    // aussi ; c'est le destinataire qui change (cf. notifyValidationReminder).
    Incident incident = awaitingValidation(IncidentStatus.DRAFT);
    incident.setDirectionRejectionReason("solution incomplète");
    when(incidentRepository.findById(incident.getId())).thenReturn(
      Optional.of(incident)
    );

    boolean result = executor.sendReminder(
      incident.getId(),
      LocalDateTime.now()
    );

    assertTrue(result);
    verify(notificationClientService).notifyValidationReminder(
      eq(incident),
      anyLong()
    );
  }
}
