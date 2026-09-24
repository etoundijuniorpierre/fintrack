package com.fintrack.incident.model.constant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// Les ensembles de statuts sont la reference unique des populations mesurees.
// Sans ce garde, chaque requete reenumere sa liste et les populations divergent :
// c'est ainsi qu'un incident annule comptait encore comme assigne et dans la charge.
class IncidentStatusSetsTest {

  @Test
  @DisplayName(
    "OPEN_STATUSES - Partitions the enum with the terminal statuses"
  )
  void openStatuses_PartitionTheEnum() {
    Set<IncidentStatus> all = EnumSet.allOf(IncidentStatus.class);
    Set<IncidentStatus> covered = EnumSet.copyOf(IncidentStatus.OPEN_STATUSES);
    covered.addAll(IncidentStatus.TERMINAL_STATUSES);

    assertThat(covered).isEqualTo(all);
    assertThat(IncidentStatus.OPEN_STATUSES).doesNotContainAnyElementsOf(
      IncidentStatus.TERMINAL_STATUSES
    );
  }

  @Test
  @DisplayName(
    "OPEN_STATUSES - Keeps the handed-over statuses in the active population"
  )
  void openStatuses_KeepHandedOverWork() {
    // TREATED et RESOLVED arretent l'horloge SLA mais l'incident n'est pas fini :
    // quelqu'un doit encore valider puis cloturer. Ils restent donc actifs.
    assertThat(IncidentStatus.OPEN_STATUSES).contains(
      IncidentStatus.TREATED,
      IncidentStatus.RESOLVED,
      IncidentStatus.UNRESOLVED_PROLONGED_WAIT
    );
    // DRAFT n'est pas un brouillon de declaration : aucun incident n'est cree dans
    // cet etat, on n'y entre qu'en proposant une solution a la Direction. C'est du
    // travail en cours, comme PENDING_VALIDATION avec lequel il est groupe.
    assertThat(IncidentStatus.AWAITING_VALIDATION_STATUSES).contains(
      IncidentStatus.DRAFT,
      IncidentStatus.PENDING_VALIDATION
    );
    assertThat(IncidentStatus.OPEN_STATUSES).containsAll(
      IncidentStatus.AWAITING_VALIDATION_STATUSES
    );
    assertThat(IncidentStatus.SLA_CLOCK_STOPPED_STATUSES).contains(
      IncidentStatus.TREATED,
      IncidentStatus.RESOLVED
    );
  }

  @Test
  @DisplayName(
    "TERMINAL_STATUSES - Cover every status that leaves the backlog for good"
  )
  void terminalStatuses_CoverEveryExit() {
    Set<String> terminalNames = IncidentStatus.TERMINAL_STATUSES.stream()
      .map(Enum::name)
      .collect(Collectors.toSet());

    assertThat(terminalNames).containsExactlyInAnyOrder(
      "CLOSED",
      "REJECTED",
      "CANCELLED"
    );
    // Toute valeur terminale doit aussi arreter l'horloge SLA.
    assertThat(IncidentStatus.SLA_CLOCK_STOPPED_STATUSES).containsAll(
      IncidentStatus.TERMINAL_STATUSES
    );
    assertThat(Arrays.stream(IncidentStatus.values()).count()).isEqualTo(
      IncidentStatus.OPEN_STATUSES.size() +
      IncidentStatus.TERMINAL_STATUSES.size()
    );
  }
}
