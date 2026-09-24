package com.fintrack.incident.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.ResolutionCycleOutcome;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.model.entity.IncidentResolutionCycle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class IncidentResolutionCycleRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private IncidentResolutionCycleRepository repository;

  private UUID agencyId;
  private UUID creatorId;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    agencyId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    now = LocalDateTime.now();
  }

  @Test
  @DisplayName(
    "findClosureStatsScoped - Aggregates average, percentiles and sample size in one pass"
  )
  void closureStats_AreAggregatedInTheDatabase() {
    // Trois cycles clotures : 1h, 2h et 12h. La mediane est 2h, la moyenne 5h.
    persistClosedCycle(now, 1, 0);
    persistClosedCycle(now, 2, 0);
    // Sur ce dernier, 6h d'horloge arretee : son delai net tombe a 6h.
    persistClosedCycle(now, 12, 360);
    entityManager.flush();

    Object[] stats = repository
      .findClosureStatsScoped(null, agencyId, null, null, null)
      .get(0);

    assertThat(((Number) stats[0]).doubleValue()).isCloseTo(
      5.0,
      within(0.01)
    );
    assertThat(((Number) stats[1]).doubleValue()).isCloseTo(2.0, within(0.01));
    assertThat(((Number) stats[3]).longValue()).isEqualTo(3L);
    // Net : 1h, 2h et 6h -> moyenne 3h.
    assertThat(((Number) stats[4]).doubleValue()).isCloseTo(
      3.0,
      within(0.01)
    );
  }

  @Test
  @DisplayName(
    "findClosureDatesScoped - Keeps the closure milestone, resolution keeps its own"
  )
  void closureAndResolutionDates_UseDistinctMilestones() {
    // Cycle resolu puis cloture : compte dans les deux familles.
    persistCycle(
      persistIncident(IncidentStatus.CLOSED),
      1,
      now.minusHours(5),
      now.minusHours(2),
      now,
      ResolutionCycleOutcome.CLOSED
    );
    // Cycle resolu mais pas encore cloture : compte seulement dans la resolution.
    persistCycle(
      persistIncident(IncidentStatus.RESOLVED),
      1,
      now.minusHours(4),
      now.minusHours(1),
      null,
      ResolutionCycleOutcome.OPEN
    );
    // Cycle encore ouvert : ne compte nulle part.
    persistCycle(
      persistIncident(IncidentStatus.OPEN),
      1,
      now.minusHours(3),
      null,
      null,
      ResolutionCycleOutcome.OPEN
    );
    entityManager.flush();

    Object[] closures = repository
      .findClosureStatsScoped(null, agencyId, null, null, null)
      .get(0);
    Object[] resolutions = repository
      .findResolutionStatsScoped(null, agencyId, null, null, null)
      .get(0);

    assertThat(((Number) closures[3]).longValue()).isEqualTo(1L);
    assertThat(((Number) resolutions[3]).longValue()).isEqualTo(2L);
  }

  @Test
  @DisplayName(
    "findClosureDatesScoped - Measures each cycle from its own opening, not the incident creation"
  )
  void closureDates_MeasureTheCycleNotTheWholeIncident() {
    // Un incident rouvert : le premier cycle garde ses jalons, le second a les siens.
    Incident reopened = persistIncident(IncidentStatus.CLOSED);
    persistCycle(
      reopened,
      1,
      now.minusDays(30),
      now.minusDays(29),
      now.minusDays(28),
      ResolutionCycleOutcome.REOPENED
    );
    persistCycle(
      reopened,
      2,
      now.minusHours(3),
      now.minusHours(2),
      now.minusHours(1),
      ResolutionCycleOutcome.CLOSED
    );
    entityManager.flush();

    Object[] closures = repository
      .findClosureStatsScoped(null, agencyId, null, null, null)
      .get(0);

    // Les deux cycles restent mesurables : un rapport passe ne change plus.
    assertThat(((Number) closures[3]).longValue()).isEqualTo(2L);
    assertThat(
      repository
        .findFirstByIncidentIdOrderByCycleNoDesc(reopened.getId())
        .orElseThrow()
        .getCycleNo()
    ).isEqualTo(2);
  }

  @Test
  @DisplayName(
    "countTransferredScoped - Counts the transfer against its own cycle, not the current validation"
  )
  void transferRate_IsAttachedToTheCycleThatWasTransferred() {
    // Cycle 1 pris en charge puis transfere, puis rouvert : le cycle 2 est repris en
    // charge plus tard. Sur la periode du cycle 1, le transfert doit rester compte.
    Incident incident = persistIncident(IncidentStatus.IN_PROGRESS);
    // La reouverture a remis i.validated_at a NULL puis une revalidation l'a repose :
    // lire le scalaire ferait disparaitre le cycle 1 du denominateur.
    incident.setValidatedAt(now.minusHours(1));
    IncidentResolutionCycle first = new IncidentResolutionCycle();
    first.setIncident(incident);
    first.setCycleNo(1);
    first.setStartedAt(now.minusDays(10));
    first.setValidatedAt(now.minusDays(9));
    first.setClosedAt(now.minusDays(8));
    first.setOutcome(ResolutionCycleOutcome.REOPENED);
    first.setPausedMinutes(0L);
    entityManager.persist(first);

    // Reroutage : le service vise n'est pas celui du depart.
    persistRouting(incident, UUID.randomUUID(), now.minusDays(9).plusHours(1));

    LocalDateTime from = now.minusDays(11);
    LocalDateTime to = now.minusDays(7);

    assertThat(
      repository.countTakenInChargeScoped(null, agencyId, null, from, to)
    ).isEqualTo(1L);
    assertThat(
      repository.countTransferredScoped(null, agencyId, null, from, to)
    ).isEqualTo(1L);
  }

  @Test
  @DisplayName(
    "countTransferredScoped - Ignores the initial routing and counts only a reroute elsewhere"
  )
  void transferRate_CountsOnlyReroutesAwayFromTheCreationTarget() {
    UUID originService = UUID.randomUUID();
    LocalDateTime from = now.minusDays(11);
    LocalDateTime to = now.minusDays(7);

    // Incident route vers son service de depart, et rien d'autre : fonctionnement
    // normal, il ne doit pas compter comme transfere.
    Incident routedOnce = persistIncident(IncidentStatus.IN_PROGRESS);
    routedOnce.setInitialTargetServiceId(originService);
    persistValidatedCycle(routedOnce);
    persistRouting(routedOnce, originService, now.minusDays(9));

    // Incident route vers son service de depart puis reroute ailleurs : compte.
    Incident rerouted = persistIncident(IncidentStatus.IN_PROGRESS);
    rerouted.setInitialTargetServiceId(originService);
    persistValidatedCycle(rerouted);
    persistRouting(rerouted, originService, now.minusDays(9));
    persistRouting(rerouted, UUID.randomUUID(), now.minusDays(8));

    assertThat(
      repository.countTakenInChargeScoped(null, agencyId, null, from, to)
    ).isEqualTo(2L);
    assertThat(
      repository.countTransferredScoped(null, agencyId, null, from, to)
    ).isEqualTo(1L);
  }

  // Cycle unique deja pris en charge, sur la fenetre utilisee par les tests transfert.
  private void persistValidatedCycle(Incident incident) {
    IncidentResolutionCycle cycle = new IncidentResolutionCycle();
    cycle.setIncident(incident);
    cycle.setCycleNo(1);
    cycle.setStartedAt(now.minusDays(10));
    cycle.setValidatedAt(now.minusDays(9).minusHours(1));
    cycle.setOutcome(ResolutionCycleOutcome.OPEN);
    cycle.setPausedMinutes(0L);
    entityManager.persist(cycle);
  }

  // Deplacement de service historise : old/new portent les identifiants de service.
  private void persistRouting(
    Incident incident,
    UUID targetServiceId,
    LocalDateTime at
  ) {
    IncidentHistory routing = new IncidentHistory();
    routing.setIncident(incident);
    routing.setUserId(UUID.randomUUID());
    routing.setAction(ActionType.ROUTING);
    routing.setNewValue(targetServiceId.toString());
    entityManager.persist(routing);
    // @PrePersist repose created_at a maintenant : on le replace en natif.
    setHistoryCreatedAt(routing, at);
  }

  // Repositionne la date d'un evenement d'historique, que BaseEntity ecrase a l'insert.
  private void setHistoryCreatedAt(
    IncidentHistory history,
    LocalDateTime createdAt
  ) {
    entityManager.flush();
    entityManager
      .getEntityManager()
      .createNativeQuery(
        "UPDATE incident_histories SET created_at = :createdAt WHERE id = :id"
      )
      .setParameter("createdAt", createdAt)
      .setParameter("id", history.getId())
      .executeUpdate();
    entityManager.clear();
  }

  // Cycle cloture de duree donnee, avec un temps d'horloge arretee.
  private void persistClosedCycle(
    LocalDateTime closedAt,
    long grossHours,
    long pausedMinutes
  ) {
    IncidentResolutionCycle cycle = new IncidentResolutionCycle();
    cycle.setIncident(persistIncident(IncidentStatus.CLOSED));
    cycle.setCycleNo(1);
    cycle.setStartedAt(closedAt.minusHours(grossHours));
    cycle.setClosedAt(closedAt);
    cycle.setOutcome(ResolutionCycleOutcome.CLOSED);
    cycle.setPausedMinutes(pausedMinutes);
    entityManager.persist(cycle);
  }

  @Test
  @DisplayName(
    "countReopenedAfterResolutionScoped - Counts resolutions per cycle, not per incident"
  )
  void reopenRate_CountsEveryResolutionOfAnIncident() {
    Incident incident = persistIncident(IncidentStatus.IN_PROGRESS);
    // Deux resolutions rouvertes, puis une troisieme qui a tenu jusqu'a la cloture.
    persistCycle(
      incident,
      1,
      now.minusDays(30),
      now.minusDays(28),
      null,
      ResolutionCycleOutcome.REOPENED
    );
    persistCycle(
      incident,
      2,
      now.minusDays(28),
      now.minusDays(20),
      null,
      ResolutionCycleOutcome.REOPENED
    );
    persistCycle(
      incident,
      3,
      now.minusDays(20),
      now.minusDays(10),
      now.minusDays(9),
      ResolutionCycleOutcome.CLOSED
    );
    entityManager.flush();

    // L'historique comptait un incident distinct de chaque cote : 1 sur 1, soit 100 %.
    // Les cycles disent ce qui s'est reellement passe : 2 reouvertures sur 3 resolutions.
    assertThat(
      repository.countResolvedCyclesScoped(null, agencyId, null, null, null)
    ).isEqualTo(3L);
    assertThat(
      repository.countReopenedAfterResolutionScoped(
        null,
        agencyId,
        null,
        null,
        null
      )
    ).isEqualTo(2L);
  }

  @Test
  @DisplayName(
    "countReopenedAfterResolutionScoped - Ignores a cycle reopened before any resolution"
  )
  void reopenRate_IgnoresCycleReopenedBeforeResolution() {
    Incident incident = persistIncident(IncidentStatus.REOPENED);
    // Attente prolongee relancee : le cycle est rouvert sans avoir jamais ete resolu.
    persistCycle(
      incident,
      1,
      now.minusDays(15),
      null,
      null,
      ResolutionCycleOutcome.REOPENED
    );
    entityManager.flush();

    assertThat(
      repository.countReopenedAfterResolutionScoped(
        null,
        agencyId,
        null,
        null,
        null
      )
    ).isZero();
  }

  private Incident persistIncident(IncidentStatus status) {
    Incident incident = new Incident();
    incident.setTitle("Test incident");
    incident.setDescription("Description");
    incident.setTypeId(UUID.randomUUID());
    incident.setCriticality(Criticality.MEDIUM);
    incident.setStatus(status);
    incident.setCreatedBy(creatorId);
    incident.setAgencyId(agencyId);
    return entityManager.persist(incident);
  }

  private void persistCycle(
    Incident incident,
    int cycleNo,
    LocalDateTime startedAt,
    LocalDateTime resolvedAt,
    LocalDateTime closedAt,
    ResolutionCycleOutcome outcome
  ) {
    IncidentResolutionCycle cycle = new IncidentResolutionCycle();
    cycle.setIncident(incident);
    cycle.setCycleNo(cycleNo);
    cycle.setStartedAt(startedAt);
    cycle.setResolvedAt(resolvedAt);
    cycle.setClosedAt(closedAt);
    cycle.setOutcome(outcome);
    cycle.setPausedMinutes(0L);
    entityManager.persist(cycle);
  }
}
