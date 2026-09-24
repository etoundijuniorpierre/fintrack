// Securise l'orchestration : cadence et isolation des echecs.

package com.fintrack.incident.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentCriticalReminderJobTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private ReportingSystemConfigClientService reportingSystemConfigClientService;

  @Mock
  private IncidentCriticalReminderExecutor executor;

  @InjectMocks
  private IncidentCriticalReminderJob job;

  private Incident criticalIncident() {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setCriticality(Criticality.CRITICAL);
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    incident.setCreatedAt(LocalDateTime.now().minusDays(1));
    return incident;
  }

  private void interval(long hours) {
    when(
      reportingSystemConfigClientService.getThresholdLong(
        eq("criticalReminderIntervalHours"),
        anyLong()
      )
    ).thenReturn(hours);
  }

  @Test
  @DisplayName("Reads the candidate window from the configured interval")
  void usesConfiguredInterval() {
    interval(6);
    when(
      incidentRepository.findCriticalOpenWithoutRecentReminder(
        any(),
        any(),
        any(),
        any()
      )
    ).thenReturn(List.of());

    job.processCriticalReminders();

    ArgumentCaptor<LocalDateTime> declaredBefore = ArgumentCaptor.forClass(
      LocalDateTime.class
    );
    ArgumentCaptor<LocalDateTime> reminderCutoff = ArgumentCaptor.forClass(
      LocalDateTime.class
    );
    verify(incidentRepository).findCriticalOpenWithoutRecentReminder(
      eq(Criticality.CRITICAL),
      eq(IncidentStatus.OPEN_STATUSES),
      declaredBefore.capture(),
      reminderCutoff.capture()
    );
    // Une declaration toute fraiche n'est pas relancee : sa notification vient de partir.
    LocalDateTime expected = LocalDateTime.now().minusHours(6);
    assertThat(declaredBefore.getValue())
      .isBetween(expected.minusMinutes(1), expected.plusMinutes(1))
      .isEqualTo(reminderCutoff.getValue());
  }

  @Test
  @DisplayName("A failing incident does not stop the batch")
  void oneFailureDoesNotStopTheBatch() {
    interval(4);
    Incident first = criticalIncident();
    Incident second = criticalIncident();
    when(
      incidentRepository.findCriticalOpenWithoutRecentReminder(
        any(),
        any(),
        any(),
        any()
      )
    ).thenReturn(List.of(first, second));
    when(executor.sendReminder(eq(first.getId()), any()))
      .thenThrow(new IllegalStateException("boom"));
    when(executor.sendReminder(eq(second.getId()), any())).thenReturn(true);

    assertDoesNotThrow(() -> job.processCriticalReminders());

    verify(executor, times(2)).sendReminder(any(), any());
  }
}
