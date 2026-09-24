// Service metier : coordonne les operations du domaine resolution cycle.

package com.fintrack.incident.service;

import com.fintrack.incident.model.constant.ResolutionCycleOutcome;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentResolutionCycle;
import com.fintrack.incident.repository.IncidentResolutionCycleRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Tient le journal des cycles de traitement d'un incident.
// Les colonnes scalaires de Incident restent la vue du cycle courant ; c'est ici que
// se conserve l'historique complet, seul support de metriques reproductibles.
@Service
@RequiredArgsConstructor
@Slf4j
public class ResolutionCycleRecorder {

  private final IncidentResolutionCycleRepository cycleRepository;

  // Ouvre le premier cycle a la creation de l'incident.
  public void openFirstCycle(Incident incident) {
    openCycle(incident, 1, incident.getCreatedAt());
  }

  // Renseigne la prise en charge du cycle courant.
  public void markValidated(Incident incident, LocalDateTime at) {
    updateCurrent(incident, cycle -> cycle.setValidatedAt(at));
  }

  // Renseigne le traitement du cycle courant.
  public void markTreated(Incident incident, LocalDateTime at) {
    updateCurrent(incident, cycle -> cycle.setTreatedAt(at));
  }

  // Renseigne la resolution du cycle courant.
  public void markResolved(Incident incident, LocalDateTime at) {
    updateCurrent(incident, cycle -> cycle.setResolvedAt(at));
  }

  // La resolution est jugee insuffisante : le cycle reste ouvert, son jalon tombe.
  public void clearResolved(Incident incident) {
    updateCurrent(incident, cycle -> cycle.setResolvedAt(null));
  }

  // Solde le cycle courant sur son issue definitive.
  public void closeCycle(
    Incident incident,
    ResolutionCycleOutcome outcome,
    LocalDateTime at
  ) {
    updateCurrent(incident, cycle -> {
      cycle.setOutcome(outcome);
      if (outcome == ResolutionCycleOutcome.CLOSED) {
        cycle.setClosedAt(at);
      }
    });
  }

  // Reouverture : le cycle courant est solde tel quel, un nouveau commence. Un cycle
  // deja solde garde son issue — reouvrir un incident rejete ne doit pas effacer le
  // fait qu'il l'avait ete ; seul un cycle encore ouvert se solde en REOPENED.
  public void reopenCycle(Incident incident, LocalDateTime at) {
    Optional<IncidentResolutionCycle> current = current(incident);
    current.ifPresent(cycle -> {
      if (cycle.getOutcome() == ResolutionCycleOutcome.OPEN) {
        cycle.setOutcome(ResolutionCycleOutcome.REOPENED);
        cycleRepository.save(cycle);
      }
    });
    openCycle(incident, current.map(c -> c.getCycleNo() + 1).orElse(1), at);
  }

  // Ajoute au cycle courant le temps pendant lequel l'horloge etait arretee.
  public void addPausedMinutes(Incident incident, long minutes) {
    if (minutes <= 0) return;
    updateCurrent(incident, cycle ->
      cycle.setPausedMinutes(cycle.getPausedMinutes() + minutes)
    );
  }

  // Recupere le cycle courant.
  public Optional<IncidentResolutionCycle> current(Incident incident) {
    return cycleRepository.findFirstByIncidentIdOrderByCycleNoDesc(
      incident.getId()
    );
  }

  private void openCycle(Incident incident, int cycleNo, LocalDateTime at) {
    IncidentResolutionCycle cycle = new IncidentResolutionCycle();
    cycle.setIncident(incident);
    cycle.setCycleNo(cycleNo);
    cycle.setStartedAt(at != null ? at : LocalDateTime.now());
    cycle.setDueDateSnapshot(incident.getInitialDueDate());
    cycle.setOutcome(ResolutionCycleOutcome.OPEN);
    cycle.setPausedMinutes(0L);
    cycleRepository.save(cycle);
  }

  // Un incident cree avant la reprise Flyway n'a pas de cycle : on n'echoue pas
  // sur une metrique, on trace et on laisse le prochain jalon le recreer.
  private void updateCurrent(
    Incident incident,
    Consumer<IncidentResolutionCycle> mutation
  ) {
    Optional<IncidentResolutionCycle> current = current(incident);
    if (current.isEmpty()) {
      log.warn(
        "Incident {} sans cycle de traitement : jalon non enregistre",
        incident.getId()
      );
      return;
    }
    IncidentResolutionCycle cycle = current.get();
    mutation.accept(cycle);
    cycleRepository.save(cycle);
  }
}
