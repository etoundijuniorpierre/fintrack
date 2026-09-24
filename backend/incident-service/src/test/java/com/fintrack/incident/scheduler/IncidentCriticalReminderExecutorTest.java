// Securise UNE relance : idempotence et marquage. Le lot : IncidentCriticalReminderJobTest.

package com.fintrack.incident.scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.model.constant.Criticality;
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
class IncidentCriticalReminderExecutorTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private NotificationClientService notificationClientService;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @InjectMocks
  private IncidentCriticalReminderExecutor executor;

  private Incident critical(IncidentStatus status) {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTitle("Écart de caisse");
    incident.setCriticality(Criticality.CRITICAL);
    incident.setStatus(status);
    incident.setCreatedAt(LocalDateTime.now().minusHours(9));
    return incident;
  }

  @Test
  @DisplayName("Marks the reminder and announces how long the incident has been open")
  void sendsReminderAndMarksIt() {
    Incident incident = critical(IncidentStatus.IN_PROGRESS);
    LocalDateTime now = LocalDateTime.now();
    when(incidentRepository.findById(incident.getId()))
      .thenReturn(Optional.of(incident));

    assertTrue(executor.sendReminder(incident.getId(), now));

    // Sans marquage, le passage suivant renverrait la meme relance.
    assertNotNull(incident.getLastCriticalReminderSentAt());
    verify(incidentRepository).saveAndFlush(incident);
    verify(notificationClientService).notifyCriticalIncidentReminder(
      eq(incident),
      anyLong()
    );
  }

  @Test
  @DisplayName("A resolved incident is no longer reminded")
  void skipsIncidentThatLeftTheOpenStatuses() {
    Incident incident = critical(IncidentStatus.CLOSED);
    when(incidentRepository.findById(incident.getId()))
      .thenReturn(Optional.of(incident));

    // Cloture depuis la lecture du lot : on renonce plutot qu'alarmer a tort.
    assertFalse(executor.sendReminder(incident.getId(), LocalDateTime.now()));

    verify(incidentRepository, never()).saveAndFlush(any());
    verify(notificationClientService, never()).notifyCriticalIncidentReminder(
      any(),
      anyLong()
    );
  }

  @Test
  @DisplayName("A downgraded incident is no longer reminded")
  void skipsIncidentNoLongerCritical() {
    Incident incident = critical(IncidentStatus.IN_PROGRESS);
    incident.setCriticality(Criticality.MEDIUM);
    when(incidentRepository.findById(incident.getId()))
      .thenReturn(Optional.of(incident));

    assertFalse(executor.sendReminder(incident.getId(), LocalDateTime.now()));

    verify(notificationClientService, never()).notifyCriticalIncidentReminder(
      any(),
      anyLong()
    );
  }

  @Test
  @DisplayName("A vanished incident does not raise")
  void skipsMissingIncident() {
    UUID id = UUID.randomUUID();
    when(incidentRepository.findById(id)).thenReturn(Optional.empty());

    assertFalse(executor.sendReminder(id, LocalDateTime.now()));
  }
}
