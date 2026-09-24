// Tests unitaires : securisent l'ORCHESTRATION des relances de validation (interrupteur,
// delai, isolation des echecs). La relance elle-meme (marquage + notification + audit)
// est couverte par IncidentValidationReminderExecutorTest.

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentValidationReminderJobTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private ReportingSystemConfigClientService reportingSystemConfigClientService;

  @Mock
  private IncidentValidationReminderExecutor executor;

  @InjectMocks
  private IncidentValidationReminderJob job;

  private void enabled(boolean on) {
    when(
      reportingSystemConfigClientService.getThresholdLong(
        eq("validationReminderEnabled"),
        anyLong()
      )
    ).thenReturn(on ? 1L : 0L);
  }

  private Incident awaitingSince(long hours) {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    incident.setDecisionAwaitedSince(LocalDateTime.now().minusHours(hours));
    return incident;
  }

  @Test
  @DisplayName("Chases a validation awaited past the configured delay")
  void chasesOverdueValidations() {
    enabled(true);
    when(
      reportingSystemConfigClientService.getThresholdLong(
        eq("validationDelayHours"),
        anyLong()
      )
    ).thenReturn(48L);
    when(
      incidentRepository.findOverdueDecisionsWithoutRecentReminder(
        eq(IncidentStatus.AWAITING_VALIDATION_STATUSES),
        any(),
        any()
      )
    ).thenReturn(List.of(awaitingSince(72)));
    when(executor.sendReminder(any(), any())).thenReturn(true);

    job.processOverdueValidations();

    verify(executor).sendReminder(any(), any());
  }

  @Test
  @DisplayName("Switched off from the Super Admin, it never touches the database")
  void switchedOff_DoesNothing() {
    enabled(false);

    job.processOverdueValidations();

    verify(incidentRepository, never())
      .findOverdueDecisionsWithoutRecentReminder(any(), any(), any());
    verifyNoInteractions(executor);
  }

  @Test
  @DisplayName("One failing incident never takes the rest of the batch down")
  void isolatesAFailingIncident() {
    enabled(true);
    when(
      reportingSystemConfigClientService.getThresholdLong(
        eq("validationDelayHours"),
        anyLong()
      )
    ).thenReturn(48L);
    Incident failing = awaitingSince(72);
    Incident healthy = awaitingSince(72);
    when(
      incidentRepository.findOverdueDecisionsWithoutRecentReminder(
        eq(IncidentStatus.AWAITING_VALIDATION_STATUSES),
        any(),
        any()
      )
    ).thenReturn(List.of(failing, healthy));
    when(executor.sendReminder(eq(failing.getId()), any())).thenThrow(
      new RuntimeException("boom")
    );
    when(executor.sendReminder(eq(healthy.getId()), any())).thenReturn(true);

    assertDoesNotThrow(() -> job.processOverdueValidations());

    verify(executor).sendReminder(eq(healthy.getId()), any());
  }
}
