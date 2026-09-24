// Tests unitaires : securisent l'ORCHESTRATION des transitions automatiques (pre-filtre
// jours ouvres, delegation par incident, isolation des echecs). La transition elle-meme
// (mutation + historique + notification) est couverte par IncidentAutoTransitionExecutorTest.

package com.fintrack.incident.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.repository.IncidentRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
class IncidentAutoTransitionJobTest {

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private ReportingSystemConfigClientService reportingSystemConfigClientService;

  @Mock
  private IncidentAutoTransitionExecutor executor;

  @InjectMocks
  private IncidentAutoTransitionJob autoTransitionJob;

  @BeforeEach
  void setUp() {
    // lenient : le test du calculateur de jours n'invoque pas processAutoTransitions.
    lenient()
      .when(
        reportingSystemConfigClientService.getThresholdLong(
          eq("autoBlockOverdueWorkingDays"),
          anyLong()
        )
      )
      .thenReturn(7L);
    lenient()
      .when(
        reportingSystemConfigClientService.getThresholdLong(
          eq("prolongedWaitDays"),
          anyLong()
        )
      )
      .thenReturn(30L);
  }

  private Incident breachedIncident(long overdueWorkingDays) {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setTitle("Overdue incident");
    incident.setStatus(IncidentStatus.ASSIGNED);
    incident.setDueDate(dueDateOverdueBy(overdueWorkingDays));
    return incident;
  }

  // Le job compte les jours OUVRES (dimanches exclus) entre l'echeance et maintenant.
  // Une echeance posee a huit jours calendaires n'en vaut sept ouvres que si la fenetre
  // n'enjambe qu'un dimanche : le dimanche elle en enjambe deux, le compte tombe a six
  // et le test rougissait un jour sur sept. On remonte donc le calendrier jusqu'au
  // nombre de jours ouvres voulu, quel que soit le jour ou la suite tourne.
  private LocalDateTime dueDateOverdueBy(long workingDays) {
    LocalDateTime dueDate = LocalDateTime.now();
    long counted = 0;
    while (counted < workingDays) {
      if (dueDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
        counted++;
      }
      dueDate = dueDate.minusDays(1);
    }
    return dueDate;
  }

  @Test
  @DisplayName("SLA working days calculator - should correctly exclude Sundays")
  void testWorkingDaysExcludingSundays() {
    LocalDateTime start = LocalDateTime.of(2026, 7, 23, 10, 0); // Thursday
    LocalDateTime end = LocalDateTime.of(2026, 7, 30, 10, 0); // Thursday
    assertEquals(
      6,
      IncidentAutoTransitionJob.getDaysBetweenExcludingSundays(start, end)
    );

    LocalDateTime endNextFriday = LocalDateTime.of(2026, 7, 31, 10, 0); // Friday
    assertEquals(
      7,
      IncidentAutoTransitionJob.getDaysBetweenExcludingSundays(
        start,
        endNextFriday
      )
    );
  }

  @Test
  @DisplayName(
    "Passes the declaration cutoff to the prolonged-wait query when configured"
  )
  void passesDeclarationCutoffToProlongedWaitQuery() {
    ReflectionTestUtils.setField(
      autoTransitionJob,
      "prolongedWaitAppliesFrom",
      "2026-09-05"
    );
    when(incidentRepository.findBreachedActiveIncidents(any(), any()))
      .thenReturn(Collections.emptyList());
    when(incidentRepository.findBlockedIncidentsOlderThan(any(), any(), any()))
      .thenReturn(Collections.emptyList());

    autoTransitionJob.processAutoTransitions();

    ArgumentCaptor<LocalDateTime> appliesFrom = ArgumentCaptor.forClass(
      LocalDateTime.class
    );
    verify(incidentRepository).findBlockedIncidentsOlderThan(
      any(),
      any(),
      appliesFrom.capture()
    );
    assertEquals(
      LocalDateTime.of(2026, 9, 5, 0, 0),
      appliesFrom.getValue()
    );
  }

  @Test
  @DisplayName(
    "Leaves the whole backlog eligible when no declaration cutoff is set"
  )
  void noCutoffLeavesWholeBacklogEligible() {
    when(incidentRepository.findBreachedActiveIncidents(any(), any()))
      .thenReturn(Collections.emptyList());
    when(incidentRepository.findBlockedIncidentsOlderThan(any(), any(), any()))
      .thenReturn(Collections.emptyList());

    autoTransitionJob.processAutoTransitions();

    // Une borne illisible ou absente ne doit jamais eteindre le dispositif en silence.
    verify(incidentRepository).findBlockedIncidentsOlderThan(
      any(),
      any(),
      isNull()
    );
  }

  @Test
  @DisplayName(
    "Delegates a breached incident (>= 7 working days) to the executor, in its own transaction"
  )
  void delegatesBreachedIncidentToExecutor() {
    Incident incident = breachedIncident(7);
    when(incidentRepository.findBreachedActiveIncidents(any(), any()))
      .thenReturn(List.of(incident));
    when(incidentRepository.findBlockedIncidentsOlderThan(any(), any(), any()))
      .thenReturn(Collections.emptyList());
    when(executor.block(eq(incident.getId()), any(), eq(7L))).thenReturn(true);

    autoTransitionJob.processAutoTransitions();

    verify(executor).block(eq(incident.getId()), any(), eq(7L));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IncidentStatus>> excluded =
      ArgumentCaptor.forClass(List.class);
    verify(incidentRepository).findBreachedActiveIncidents(
      excluded.capture(),
      any()
    );
    assertEquals(
      Set.of(
        // Personne ne peut traiter un incident qui attend encore une validation, que ce
        // soit celle du valideur configure ou celle de la Direction.
        IncidentStatus.PENDING_VALIDATION,
        IncidentStatus.DRAFT,
        IncidentStatus.TREATED,
        IncidentStatus.RESOLVED,
        IncidentStatus.CLOSED,
        IncidentStatus.REJECTED,
        IncidentStatus.CANCELLED,
        IncidentStatus.BLOCKED,
        IncidentStatus.UNRESOLVED_PROLONGED_WAIT
      ),
      Set.copyOf(excluded.getValue())
    );
  }

  @Test
  @DisplayName(
    "Does NOT delegate when breached by < 7 working days (working-days pre-filter)"
  )
  void doesNotDelegateBelowThreshold() {
    Incident incident = breachedIncident(6);
    when(incidentRepository.findBreachedActiveIncidents(any(), any()))
      .thenReturn(List.of(incident));
    when(incidentRepository.findBlockedIncidentsOlderThan(any(), any(), any()))
      .thenReturn(Collections.emptyList());

    autoTransitionJob.processAutoTransitions();

    verify(executor, never()).block(any(), any(), anyLong());
  }

  @Test
  @DisplayName(
    "Isolation - a failure on one incident does not stop the rest of the batch"
  )
  void oneFailureDoesNotStopTheBatch() {
    Incident first = breachedIncident(7);
    Incident second = breachedIncident(7);
    when(incidentRepository.findBreachedActiveIncidents(any(), any()))
      .thenReturn(List.of(first, second));
    when(incidentRepository.findBlockedIncidentsOlderThan(any(), any(), any()))
      .thenReturn(Collections.emptyList());
    when(executor.block(eq(first.getId()), any(), anyLong()))
      .thenThrow(new RuntimeException("boom"));
    when(executor.block(eq(second.getId()), any(), anyLong())).thenReturn(true);

    assertDoesNotThrow(() -> autoTransitionJob.processAutoTransitions());

    verify(executor).block(eq(first.getId()), any(), anyLong());
    verify(executor).block(eq(second.getId()), any(), anyLong());
  }

  @Test
  @DisplayName("Delegates a long-blocked incident to the executor for prolongation")
  void delegatesProlongedIncidentToExecutor() {
    Incident incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setStatus(IncidentStatus.BLOCKED);
    when(incidentRepository.findBreachedActiveIncidents(any(), any()))
      .thenReturn(Collections.emptyList());
    when(incidentRepository.findBlockedIncidentsOlderThan(any(), any(), any()))
      .thenReturn(List.of(incident));
    when(executor.prolong(eq(incident.getId()), eq(30L))).thenReturn(true);

    autoTransitionJob.processAutoTransitions();

    verify(executor).prolong(eq(incident.getId()), eq(30L));
  }
}
