// Tests unitaires : securisent l'ORCHESTRATION des relances de confirmation d'actualite
// (cadence, isolation des echecs). La relance elle-meme (marquage + notification + audit)
// est couverte par IncidentConfirmationReminderExecutorTest.

package com.fintrack.incident.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentConfirmationReminderJobTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private ReportingSystemConfigClientService reportingSystemConfigClientService;

  @Mock
  private IncidentConfirmationReminderExecutor executor;

  @InjectMocks
  private IncidentConfirmationReminderJob job;

  @BeforeEach
  void setUp() {
    when(
      reportingSystemConfigClientService.getThresholdLong(
        eq("blockedReminderIntervalDays"),
        anyLong()
      )
    ).thenReturn(7L);
  }

  private Incident pendingSince(long days) {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setStatus(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);
    incident.setConfirmationRequestedAt(LocalDateTime.now().minusDays(days));
    return incident;
  }

  @Test
  @DisplayName("Chases every request left unanswered past the reminder interval")
  void chasesUnansweredRequests() {
    when(
      incidentRepository.findPendingConfirmationsWithoutRecentReminder(
        eq(IncidentStatus.UNRESOLVED_PROLONGED_WAIT),
        any(),
        any()
      )
    ).thenReturn(List.of(pendingSince(10)));
    when(executor.sendReminder(any(), any())).thenReturn(true);

    job.processPendingConfirmations();

    verify(executor).sendReminder(any(), any());
  }

  @Test
  @DisplayName("One failing incident never takes the rest of the batch down")
  void isolatesAFailingIncident() {
    Incident failing = pendingSince(4);
    Incident healthy = pendingSince(4);
    when(
      incidentRepository.findPendingConfirmationsWithoutRecentReminder(
        eq(IncidentStatus.UNRESOLVED_PROLONGED_WAIT),
        any(),
        any()
      )
    ).thenReturn(List.of(failing, healthy));
    when(executor.sendReminder(eq(failing.getId()), any())).thenThrow(
      new RuntimeException("boom")
    );
    when(executor.sendReminder(eq(healthy.getId()), any())).thenReturn(true);

    assertDoesNotThrow(() -> job.processPendingConfirmations());

    verify(executor).sendReminder(eq(healthy.getId()), any());
  }
}
