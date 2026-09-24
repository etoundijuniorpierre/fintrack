// Tests unitaires : securisent l'ORCHESTRATION des rappels SLA (selection, delegation par
// incident, isolation des echecs, intervalle configure). Le rappel lui-meme (mutation +
// notification + audit) est couvert par IncidentSlaReminderExecutorTest.

package com.fintrack.incident.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IncidentSlaReminderJobTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private ReportingSystemConfigClientService reportingSystemConfigClientService;

  @Mock
  private IncidentSlaReminderExecutor executor;

  @InjectMocks
  private IncidentSlaReminderJob slaReminderJob;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
      slaReminderJob,
      "slaReminderIntervalHours",
      24L
    );
    lenient()
      .when(
        reportingSystemConfigClientService.getThresholdLong(
          "slaReminderIntervalHours",
          24L
        )
      )
      .thenReturn(24L);
  }

  private Incident breachedIncident() {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    return incident;
  }

  @Test
  @DisplayName(
    "Delegates each breached incident to the executor in its own transaction"
  )
  void delegatesBreachedIncidentsToExecutor() {
    Incident incident = breachedIncident();
    when(
      incidentRepository.findBreachedSlaWithoutRecentReminder(any(), any(), any())
    ).thenReturn(List.of(incident), Collections.emptyList());
    when(executor.sendReminder(incident.getId())).thenReturn(true);

    slaReminderJob.processBreachedSlaReminders();

    verify(executor).sendReminder(incident.getId());
  }

  @Test
  @DisplayName("Does not delegate when the selection query returns nothing")
  void doesNotDelegateWhenNoBreach() {
    when(
      incidentRepository.findBreachedSlaWithoutRecentReminder(any(), any(), any())
    ).thenReturn(Collections.emptyList());

    slaReminderJob.processBreachedSlaReminders();

    verify(executor, never()).sendReminder(any());
  }

  @Test
  @DisplayName(
    "Isolation - a failure on one incident does not stop the rest of the batch"
  )
  void oneFailureDoesNotStopTheBatch() {
    Incident first = breachedIncident();
    Incident second = breachedIncident();
    when(
      incidentRepository.findBreachedSlaWithoutRecentReminder(any(), any(), any())
    ).thenReturn(List.of(first, second), Collections.emptyList());
    when(executor.sendReminder(first.getId()))
      .thenThrow(new RuntimeException("boom"));
    when(executor.sendReminder(second.getId())).thenReturn(true);

    slaReminderJob.processBreachedSlaReminders();

    verify(executor).sendReminder(first.getId());
    verify(executor).sendReminder(second.getId());
  }

  @Test
  @DisplayName(
    "Deadline reminder - Neither pass ever targets a status whose clock has stopped"
  )
  void reminder_NeverTargetsStatusesWhoseClockStopped() {
    when(
      incidentRepository.findBreachedSlaWithoutRecentReminder(any(), any(), any())
    ).thenReturn(Collections.emptyList());

    slaReminderJob.processBreachedSlaReminders();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IncidentStatus>> excludedCaptor = ArgumentCaptor.forClass(
      List.class
    );
    verify(incidentRepository, times(2))
      .findBreachedSlaWithoutRecentReminder(
        excludedCaptor.capture(),
        any(),
        any()
      );

    // Un incident annule, rejete, clos ou resolu ne doit recevoir aucun rappel :
    // ni le passage standard, ni celui des incidents bloques. Un incident qui attend
    // une validation non plus : son delai de traitement n'a pas commence.
    for (List<IncidentStatus> excluded : excludedCaptor.getAllValues()) {
      Assertions.assertThat(excluded).containsAll(
        IncidentStatus.SLA_CLOCK_STOPPED_STATUSES
      );
      Assertions.assertThat(excluded).containsAll(
        IncidentStatus.AWAITING_VALIDATION_STATUSES
      );
    }
  }

  @Test
  @DisplayName("Deadline reminder - Uses the interval configured by Super Admin")
  void reminder_UsesConfiguredInterval() {
    when(
      reportingSystemConfigClientService.getThresholdLong(
        "slaReminderIntervalHours",
        24L
      )
    ).thenReturn(6L);
    when(
      incidentRepository.findBreachedSlaWithoutRecentReminder(any(), any(), any())
    ).thenReturn(Collections.emptyList());

    slaReminderJob.processBreachedSlaReminders();

    ArgumentCaptor<LocalDateTime> currentCaptor =
      ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> cutoffCaptor =
      ArgumentCaptor.forClass(LocalDateTime.class);
    verify(incidentRepository, times(2))
      .findBreachedSlaWithoutRecentReminder(
        any(),
        currentCaptor.capture(),
        cutoffCaptor.capture()
      );

    Assertions.assertThat(
      Duration.between(
        cutoffCaptor.getAllValues().getFirst(),
        currentCaptor.getAllValues().getFirst()
      )
    ).isEqualTo(Duration.ofHours(6));
  }
}
