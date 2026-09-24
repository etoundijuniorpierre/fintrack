// Tests unitaires : securisent le journal des cycles de traitement (ouverture,
// jalons, et surtout ce qu'une reouverture a le droit d'ecraser).

package com.fintrack.incident.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fintrack.incident.model.constant.ResolutionCycleOutcome;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentResolutionCycle;
import com.fintrack.incident.repository.IncidentResolutionCycleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolutionCycleRecorderTest {

  @Mock
  private IncidentResolutionCycleRepository cycleRepository;

  @InjectMocks
  private ResolutionCycleRecorder recorder;

  private Incident incident;

  @BeforeEach
  void setUp() {
    incident = new Incident();
    incident.setId(UUID.randomUUID());
    incident.setCreatedAt(LocalDateTime.now().minusDays(20));
  }

  // Construit un cycle courant deja enregistre, avec l'issue demandee.
  private IncidentResolutionCycle currentCycle(ResolutionCycleOutcome outcome) {
    IncidentResolutionCycle cycle = new IncidentResolutionCycle();
    cycle.setIncident(incident);
    cycle.setCycleNo(1);
    cycle.setStartedAt(incident.getCreatedAt());
    cycle.setOutcome(outcome);
    cycle.setPausedMinutes(0L);
    when(
      cycleRepository.findFirstByIncidentIdOrderByCycleNoDesc(incident.getId())
    ).thenReturn(Optional.of(cycle));
    return cycle;
  }

  @Test
  @DisplayName("reopenCycle - Settles an open cycle as reopened and opens the next")
  void reopenCycle_SettlesOpenCycle() {
    IncidentResolutionCycle current = currentCycle(
      ResolutionCycleOutcome.OPEN
    );
    LocalDateTime reopenedAt = LocalDateTime.now();

    recorder.reopenCycle(incident, reopenedAt);

    assertThat(current.getOutcome()).isEqualTo(
      ResolutionCycleOutcome.REOPENED
    );
    ArgumentCaptor<IncidentResolutionCycle> saved = ArgumentCaptor.forClass(
      IncidentResolutionCycle.class
    );
    verify(cycleRepository, times(2)).save(saved.capture());
    List<IncidentResolutionCycle> all = saved.getAllValues();
    assertThat(all.get(1).getCycleNo()).isEqualTo(2);
    assertThat(all.get(1).getStartedAt()).isEqualTo(reopenedAt);
    assertThat(all.get(1).getOutcome()).isEqualTo(ResolutionCycleOutcome.OPEN);
  }

  @Test
  @DisplayName("reopenCycle - Keeps the outcome of an already settled cycle")
  void reopenCycle_KeepsSettledOutcome() {
    // Rouvrir un incident rejete ne doit pas effacer le fait qu'il l'a ete : sans
    // cela, l'issue du cycle precedent devenait irrecuperable.
    IncidentResolutionCycle current = currentCycle(
      ResolutionCycleOutcome.REJECTED
    );

    recorder.reopenCycle(incident, LocalDateTime.now());

    assertThat(current.getOutcome()).isEqualTo(
      ResolutionCycleOutcome.REJECTED
    );
    // Seul le nouveau cycle est ecrit : l'ancien n'a pas change.
    verify(cycleRepository, times(1)).save(any(IncidentResolutionCycle.class));
  }

  @Test
  @DisplayName("closeCycle - Only a closure carries a closing date")
  void closeCycle_SetsClosedAtForClosureOnly() {
    IncidentResolutionCycle current = currentCycle(
      ResolutionCycleOutcome.OPEN
    );

    recorder.closeCycle(incident, ResolutionCycleOutcome.CANCELLED, null);

    assertThat(current.getOutcome()).isEqualTo(
      ResolutionCycleOutcome.CANCELLED
    );
    assertThat(current.getClosedAt()).isNull();
  }

  @Test
  @DisplayName("updateCurrent - Records nothing when the incident has no cycle")
  void markResolved_WithoutCycle_DoesNotFail() {
    when(
      cycleRepository.findFirstByIncidentIdOrderByCycleNoDesc(incident.getId())
    ).thenReturn(Optional.empty());

    recorder.markResolved(incident, LocalDateTime.now());

    verify(cycleRepository, never()).save(any(IncidentResolutionCycle.class));
  }
}
