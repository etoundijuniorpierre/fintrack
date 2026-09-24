package com.fintrack.incident.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.model.entity.IncidentSearchCriteria;
import com.fintrack.incident.repository.specification.IncidentSpecification;
import com.fintrack.incident.scheduler.IncidentAutoTransitionJob;
import com.fintrack.incident.security.UserDetailsImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class IncidentRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private IncidentRepository repository;

  private UUID agencyId;
  private UUID creatorId;
  private UUID assigneeId;
  private UUID serviceId;
  private UUID typeConfigId;

  @BeforeEach
  void setUp() {
    agencyId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    assigneeId = UUID.randomUUID();
    serviceId = UUID.randomUUID();
    typeConfigId = UUID.randomUUID();
  }

  private Incident buildIncident(IncidentStatus status) {
    Incident incident = new Incident();
    incident.setTitle("Test incident");
    incident.setDescription("Description");
    incident.setTypeId(typeConfigId);
    incident.setCriticality(Criticality.MEDIUM);
    incident.setStatus(status);
    incident.setCreatedBy(creatorId);
    incident.setAgencyId(agencyId);
    return incident;
  }

  // Place la creation hors periode sans dependre du callback JPA de BaseEntity.
  private void setPersistedCreatedAt(
    Incident incident,
    LocalDateTime createdAt
  ) {
    entityManager.flush();
    entityManager
      .getEntityManager()
      .createNativeQuery(
        "UPDATE incidents SET created_at = :createdAt WHERE id = :id"
      )
      .setParameter("createdAt", createdAt)
      .setParameter("id", incident.getId())
      .executeUpdate();
    entityManager.clear();
  }

  @Test
  @DisplayName("findByAgencyId - Returns incidents for given agency")
  void findByAgencyId_ReturnsMatchingIncidents() {
    Incident i1 = buildIncident(IncidentStatus.OPEN);
    Incident i2 = buildIncident(IncidentStatus.PENDING_VALIDATION);
    Incident other = buildIncident(IncidentStatus.OPEN);
    other.setAgencyId(UUID.randomUUID());

    entityManager.persist(i1);
    entityManager.persist(i2);
    entityManager.persist(other);
    entityManager.flush();

    List<Incident> result = repository.findByAgencyId(agencyId);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(i -> i.getAgencyId().equals(agencyId));
  }

  @Test
  @DisplayName("findByAgencyId - Returns empty when no incidents for agency")
  void findByAgencyId_NoMatch_ReturnsEmpty() {
    assertThat(repository.findByAgencyId(UUID.randomUUID())).isEmpty();
  }

  @Test
  @DisplayName("findByCreatedBy - Returns incidents created by given user")
  void findByCreatedBy_ReturnsMatchingIncidents() {
    Incident i1 = buildIncident(IncidentStatus.OPEN);
    Incident i2 = buildIncident(IncidentStatus.VALIDATED);
    Incident other = buildIncident(IncidentStatus.OPEN);
    other.setCreatedBy(UUID.randomUUID());

    entityManager.persist(i1);
    entityManager.persist(i2);
    entityManager.persist(other);
    entityManager.flush();

    List<Incident> result = repository.findByCreatedBy(creatorId);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(i -> i.getCreatedBy().equals(creatorId));
  }

  @Test
  @DisplayName("findByAssignedTo - Returns incidents assigned to given user")
  void findByAssignedTo_ReturnsMatchingIncidents() {
    Incident i1 = buildIncident(IncidentStatus.IN_PROGRESS);
    i1.setAssignedTo(assigneeId);
    Incident i2 = buildIncident(IncidentStatus.IN_PROGRESS);
    i2.setAssignedTo(assigneeId);
    Incident unassigned = buildIncident(IncidentStatus.TRANSFERRED);

    entityManager.persist(i1);
    entityManager.persist(i2);
    entityManager.persist(unassigned);
    entityManager.flush();

    List<Incident> result = repository.findByAssignedTo(assigneeId);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(i -> assigneeId.equals(i.getAssignedTo()));
  }

  @Test
  @DisplayName("findByAssignedTo - Returns empty when no incidents assigned")
  void findByAssignedTo_NoMatch_ReturnsEmpty() {
    assertThat(repository.findByAssignedTo(UUID.randomUUID())).isEmpty();
  }


  @Test
  @DisplayName(
    "countSlaCompliantInAgency - Judges compliance on the reference deadline, not the reset one"
  )
  void countSlaCompliant_UsesTheReferenceDeadline() {
    // Incident parti en attente prolongee puis repris : son echeance courante est
    // neuve, mais l'engagement pris a la declaration, lui, a bien ete manque.
    Incident resumedAndLate = buildIncident(IncidentStatus.CLOSED);
    resumedAndLate.setInitialDueDate(LocalDateTime.now().minusDays(40));
    resumedAndLate.setDueDate(LocalDateTime.now().plusDays(2));
    resumedAndLate.setClosedAt(LocalDateTime.now().minusDays(1));

    Incident onTime = buildIncident(IncidentStatus.CLOSED);
    onTime.setInitialDueDate(LocalDateTime.now().plusDays(3));
    onTime.setDueDate(LocalDateTime.now().plusDays(3));
    onTime.setClosedAt(LocalDateTime.now().minusDays(1));

    entityManager.persist(resumedAndLate);
    entityManager.persist(onTime);
    entityManager.flush();

    long compliant = repository.countSlaCompliantInAgency(agencyId, null, null);

    assertThat(compliant).isEqualTo(1L);
  }

  @Test
  @DisplayName(
    "countSlaBreachNow - Every status whose clock has stopped leaves the overdue counter"
  )
  void countSlaBreachNow_ExcludesStatusesWhoseClockStopped() {
    // Horloge arretee, ou validation attendue : dans les deux cas le delai du traitant
    // ne court pas, l'incident ne peut pas etre "en retard maintenant".
    for (IncidentStatus stopped : IncidentStatus.NOT_LATE_STATUSES) {
      Incident incident = buildIncident(stopped);
      incident.setDueDate(LocalDateTime.now().minusDays(30));
      entityManager.persist(incident);
    }
    entityManager.flush();

    long breach = repository.countSlaBreachNow(
      IncidentStatus.NOT_LATE_STATUSES,
      null,
      agencyId,
      null,
      LocalDateTime.now()
    );

    assertThat(breach).isZero();
  }

  @Test
  @DisplayName(
    "findBreachedSlaWithoutRecentReminder - Never picks a status whose clock has stopped"
  )
  void findBreachedSla_SkipsStatusesWhoseClockStopped() {
    // Le filtre des jobs de rappel : sans lui, un incident annule recevait des
    // rappels d'echeance a vie.
    for (IncidentStatus stopped : IncidentStatus.SLA_CLOCK_STOPPED_STATUSES) {
      Incident incident = buildIncident(stopped);
      incident.setDueDate(LocalDateTime.now().minusDays(30));
      entityManager.persist(incident);
    }
    Incident stillRunning = buildIncident(IncidentStatus.IN_PROGRESS);
    stillRunning.setDueDate(LocalDateTime.now().minusDays(30));
    entityManager.persist(stillRunning);
    entityManager.flush();

    List<Incident> breached = repository.findBreachedSlaWithoutRecentReminder(
      List.copyOf(IncidentStatus.SLA_CLOCK_STOPPED_STATUSES),
      LocalDateTime.now(),
      LocalDateTime.now()
    );

    assertThat(breached).extracting(Incident::getId).containsExactly(
      stillRunning.getId()
    );
  }

  @Test
  @DisplayName(
    "findOverdueDecisionsWithoutRecentReminder - Picks both senses of the Direction wait, skips the freshly chased"
  )
  void findOverdueValidations_SkipsRecentlyChased() {
    Incident awaitingDirection = buildIncident(IncidentStatus.DRAFT);
    awaitingDirection.setDecisionAwaitedSince(
      LocalDateTime.now().minusHours(72)
    );

    // Meme statut, sens inverse : l'incident attend le traitant, pas la Direction.
    // Il se relance aussi — le destinataire seul change.
    Incident awaitingHandler = buildIncident(IncidentStatus.DRAFT);
    awaitingHandler.setDecisionAwaitedSince(
      LocalDateTime.now().minusHours(72)
    );
    awaitingHandler.setDirectionRejectionReason("solution incomplète");

    Incident recentlyChased = buildIncident(IncidentStatus.PENDING_VALIDATION);
    recentlyChased.setDecisionAwaitedSince(
      LocalDateTime.now().minusHours(72)
    );
    recentlyChased.setLastDecisionReminderSentAt(LocalDateTime.now());

    entityManager.persist(awaitingDirection);
    entityManager.persist(awaitingHandler);
    entityManager.persist(recentlyChased);
    entityManager.flush();

    LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
    List<Incident> overdue =
      repository.findOverdueDecisionsWithoutRecentReminder(
        IncidentStatus.AWAITING_VALIDATION_STATUSES,
        cutoff,
        cutoff
      );

    assertThat(overdue)
      .extracting(Incident::getId)
      .containsExactlyInAnyOrder(
        awaitingDirection.getId(),
        awaitingHandler.getId()
      );
  }

  @Test
  @DisplayName(
    "findPendingConfirmationsWithoutRecentReminder - Picks the unanswered requests, skips the freshly chased"
  )
  void findPendingConfirmations_SkipsRecentlyReminded() {
    Incident unanswered = buildIncident(
      IncidentStatus.UNRESOLVED_PROLONGED_WAIT
    );
    unanswered.setConfirmationRequestedAt(LocalDateTime.now().minusDays(10));

    Incident justChased = buildIncident(
      IncidentStatus.UNRESOLVED_PROLONGED_WAIT
    );
    justChased.setConfirmationRequestedAt(LocalDateTime.now().minusDays(10));
    justChased.setLastConfirmationReminderSentAt(LocalDateTime.now());

    Incident noRequest = buildIncident(IncidentStatus.UNRESOLVED_PROLONGED_WAIT);

    entityManager.persist(unanswered);
    entityManager.persist(justChased);
    entityManager.persist(noRequest);
    entityManager.flush();

    LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
    List<Incident> pending =
      repository.findPendingConfirmationsWithoutRecentReminder(
        IncidentStatus.UNRESOLVED_PROLONGED_WAIT,
        cutoff,
        cutoff
      );

    assertThat(pending).extracting(Incident::getId).containsExactly(
      unanswered.getId()
    );
  }

  @Test
  @DisplayName(
    "countSlaBreachNow - Counts open overdue incidents, excludes closed"
  )
  void countSlaBreachNow_CountsOpenOverdue() {
    Incident overdue = buildIncident(IncidentStatus.IN_PROGRESS);
    overdue.setAssignedTo(assigneeId);
    overdue.setDueDate(LocalDateTime.now().minusDays(1));
    Incident onTime = buildIncident(IncidentStatus.IN_PROGRESS);
    onTime.setDueDate(LocalDateTime.now().plusDays(5));
    Incident closedOverdue = buildIncident(IncidentStatus.CLOSED);
    closedOverdue.setDueDate(LocalDateTime.now().minusDays(2));

    entityManager.persist(overdue);
    entityManager.persist(onTime);
    entityManager.persist(closedOverdue);
    entityManager.flush();

    long breach = repository.countSlaBreachNow(
      IncidentStatus.SLA_CLOCK_STOPPED_STATUSES,
      null,
      agencyId,
      null,
      LocalDateTime.now()
    );

    assertThat(breach).isEqualTo(1L);
  }

  @Test
  @DisplayName(
    "countClosedInPeriod - Counts incidents closed within the window (closed_at)"
  )
  void countClosedInPeriod_FiltersByClosedAt() {
    Incident closedIn = buildIncident(IncidentStatus.CLOSED);
    closedIn.setClosedAt(LocalDateTime.now().minusDays(1));
    Incident closedOut = buildIncident(IncidentStatus.CLOSED);
    closedOut.setClosedAt(LocalDateTime.now().minusDays(40));

    entityManager.persist(closedIn);
    entityManager.persist(closedOut);
    entityManager.flush();

    long count = repository.countClosedInPeriod(
      null,
      agencyId,
      null,
      LocalDateTime.now().minusDays(7),
      LocalDateTime.now()
    );

    assertThat(count).isEqualTo(1L);
  }

  @Test
  @DisplayName(
    "countResolvedReached - Uses the resolution history instead of the current entity state"
  )
  void countResolvedReached_UsesResolutionHistory() {
    Incident incident = buildIncident(IncidentStatus.RESOLVED);
    incident.setResolvedAt(null);
    entityManager.persist(incident);
    IncidentHistory resolution = new IncidentHistory();
    resolution.setIncident(incident);
    resolution.setUserId(assigneeId);
    resolution.setAction(ActionType.STATUS_CHANGE);
    resolution.setNewValue(IncidentStatus.RESOLVED.name());
    resolution.setCreatedAt(LocalDateTime.now().minusDays(2));
    entityManager.persist(resolution);
    setPersistedCreatedAt(incident, LocalDateTime.now().minusDays(90));

    long count = repository.countResolvedReached(
      null,
      agencyId,
      null,
      LocalDateTime.now().minusDays(7),
      LocalDateTime.now()
    );

    assertThat(count).isEqualTo(1L);
    assertThat(
      repository.countResolvedInPeriod(
        null,
        agencyId,
        null,
        LocalDateTime.now().minusDays(7),
        LocalDateTime.now()
      )
    ).isEqualTo(1L);
  }

  @Test
  @DisplayName(
    "countTreatedInPeriod - Counts incidents moved to TREATED within the window via history"
  )
  void countTreatedInPeriod_UsesTreatmentHistory() {
    Incident incident = buildIncident(IncidentStatus.TREATED);
    entityManager.persist(incident);
    IncidentHistory treatment = new IncidentHistory();
    treatment.setIncident(incident);
    treatment.setUserId(assigneeId);
    treatment.setAction(ActionType.STATUS_CHANGE);
    treatment.setNewValue(IncidentStatus.TREATED.name());
    treatment.setCreatedAt(LocalDateTime.now().minusDays(2));
    entityManager.persist(treatment);
    setPersistedCreatedAt(incident, LocalDateTime.now().minusDays(90));

    assertThat(
      repository.countTreatedInPeriod(
        null,
        agencyId,
        null,
        LocalDateTime.now().minusDays(7),
        LocalDateTime.now()
      )
    ).isEqualTo(1L);
    // Autre agence : hors perimetre, non compte.
    assertThat(
      repository.countTreatedInPeriod(
        null,
        UUID.randomUUID(),
        null,
        LocalDateTime.now().minusDays(7),
        LocalDateTime.now()
      )
    ).isEqualTo(0L);
  }

  @Test
  @DisplayName(
    "findClosedInPeriod - Returns the incidents closed within the window (closed_at)"
  )
  void findClosedInPeriod_ReturnsClosedIncidents() {
    Incident closedIn = buildIncident(IncidentStatus.CLOSED);
    closedIn.setClosedAt(LocalDateTime.now().minusDays(1));
    Incident closedOut = buildIncident(IncidentStatus.CLOSED);
    closedOut.setClosedAt(LocalDateTime.now().minusDays(40));
    entityManager.persist(closedIn);
    entityManager.persist(closedOut);
    entityManager.flush();

    List<Incident> closed = repository.findClosedInPeriod(
      null,
      agencyId,
      null,
      LocalDateTime.now().minusDays(7),
      LocalDateTime.now()
    );

    assertThat(closed).extracting(Incident::getId).containsExactly(closedIn.getId());
  }

  @Test
  @DisplayName(
    "findResolvedInPeriod - Returns the incidents moved to RESOLVED within the window via history"
  )
  void findResolvedInPeriod_UsesResolutionHistory() {
    Incident incident = buildIncident(IncidentStatus.RESOLVED);
    incident.setResolvedAt(null);
    entityManager.persist(incident);
    IncidentHistory resolution = new IncidentHistory();
    resolution.setIncident(incident);
    resolution.setUserId(assigneeId);
    resolution.setAction(ActionType.STATUS_CHANGE);
    resolution.setNewValue(IncidentStatus.RESOLVED.name());
    resolution.setCreatedAt(LocalDateTime.now().minusDays(2));
    entityManager.persist(resolution);
    setPersistedCreatedAt(incident, LocalDateTime.now().minusDays(90));

    List<Incident> resolved = repository.findResolvedInPeriod(
      null,
      agencyId,
      null,
      LocalDateTime.now().minusDays(7),
      LocalDateTime.now()
    );

    assertThat(resolved).extracting(Incident::getId).containsExactly(incident.getId());
  }

  @Test
  @DisplayName(
    "findTreatedInPeriod - Returns the incidents moved to TREATED within the window and respects scope"
  )
  void findTreatedInPeriod_UsesTreatmentHistoryAndScope() {
    Incident incident = buildIncident(IncidentStatus.TREATED);
    entityManager.persist(incident);
    IncidentHistory treatment = new IncidentHistory();
    treatment.setIncident(incident);
    treatment.setUserId(assigneeId);
    treatment.setAction(ActionType.STATUS_CHANGE);
    treatment.setNewValue(IncidentStatus.TREATED.name());
    treatment.setCreatedAt(LocalDateTime.now().minusDays(2));
    entityManager.persist(treatment);
    setPersistedCreatedAt(incident, LocalDateTime.now().minusDays(90));

    assertThat(
      repository.findTreatedInPeriod(
        null,
        agencyId,
        null,
        LocalDateTime.now().minusDays(7),
        LocalDateTime.now()
      )
    )
      .extracting(Incident::getId)
      .containsExactly(incident.getId());
    // Autre agence : hors perimetre, liste vide.
    assertThat(
      repository.findTreatedInPeriod(
        null,
        UUID.randomUUID(),
        null,
        LocalDateTime.now().minusDays(7),
        LocalDateTime.now()
      )
    ).isEmpty();
  }

  @Test
  @DisplayName(
    "findBreachedActiveIncidents - returns overdue active incidents, excludes terminal/blocked and not-yet-due"
  )
  void findBreachedActiveIncidents_selectsOverdueActive() {
    Incident overdue = buildIncident(IncidentStatus.IN_PROGRESS);
    overdue.setDueDate(LocalDateTime.now().minusDays(10));
    Incident notYetDue = buildIncident(IncidentStatus.IN_PROGRESS);
    notYetDue.setDueDate(LocalDateTime.now().plusDays(2));
    Incident treated = buildIncident(IncidentStatus.TREATED);
    treated.setDueDate(LocalDateTime.now().minusDays(10));
    // Tant qu'il attend sa validation, l'incident n'est traitable par personne :
    // le blocage SLA ne doit pas le saisir.
    Incident pendingValidation = buildIncident(
      IncidentStatus.PENDING_VALIDATION
    );
    pendingValidation.setDueDate(LocalDateTime.now().minusDays(10));
    entityManager.persist(overdue);
    entityManager.persist(notYetDue);
    entityManager.persist(treated);
    entityManager.persist(pendingValidation);
    entityManager.flush();

    List<Incident> result = repository.findBreachedActiveIncidents(
      IncidentAutoTransitionJob.AUTO_BLOCK_EXCLUDED_STATUSES,
      LocalDateTime.now()
    );

    assertThat(result)
      .extracting(Incident::getId)
      .containsExactly(overdue.getId());
  }

  @Test
  @DisplayName(
    "findBlockedIncidentsOlderThan - returns incidents blocked before the cutoff only"
  )
  void findBlockedIncidentsOlderThan_selectsStaleBlocked() {
    Incident staleBlocked = buildIncident(IncidentStatus.BLOCKED);
    staleBlocked.setBlockedAt(LocalDateTime.now().minusDays(40));
    Incident recentBlocked = buildIncident(IncidentStatus.BLOCKED);
    recentBlocked.setBlockedAt(LocalDateTime.now().minusDays(5));
    entityManager.persist(staleBlocked);
    entityManager.persist(recentBlocked);
    entityManager.flush();

    List<Incident> result = repository.findBlockedIncidentsOlderThan(
      IncidentStatus.BLOCKED,
      LocalDateTime.now().minusDays(30),
      null
    );

    assertThat(result)
      .extracting(Incident::getId)
      .containsExactly(staleBlocked.getId());
  }

  @Test
  @DisplayName(
    "findBlockedIncidentsOlderThan - ignores incidents declared before the cutoff"
  )
  void findBlockedIncidentsOlderThan_honoursDeclarationCutoff() {
    // Meme incident, meme anciennete de blocage : seule sa date de declaration decide.
    Incident staleBlocked = buildIncident(IncidentStatus.BLOCKED);
    staleBlocked.setBlockedAt(LocalDateTime.now().minusDays(40));
    entityManager.persist(staleBlocked);
    entityManager.flush();

    List<Incident> result = repository.findBlockedIncidentsOlderThan(
      IncidentStatus.BLOCKED,
      LocalDateTime.now().minusDays(30),
      LocalDateTime.now().plusDays(1)
    );

    assertThat(result).isEmpty();
  }



  @Test
  @DisplayName(
    "findTopServices - Ranks transferred service then falls back to creator service"
  )
  void findTopServices_UsesResponsibleServiceAndRanksDescending() {
    UUID transferredServiceId = UUID.randomUUID();
    LocalDateTime closedAt = LocalDateTime.now().minusDays(1);

    Incident creatorServiceIncident1 = buildIncident(IncidentStatus.CLOSED);
    creatorServiceIncident1.setCreatorServiceId(serviceId);
    creatorServiceIncident1.setClosedAt(closedAt);
    Incident creatorServiceIncident2 = buildIncident(IncidentStatus.CLOSED);
    creatorServiceIncident2.setCreatorServiceId(serviceId);
    creatorServiceIncident2.setClosedAt(closedAt);
    Incident transferredServiceIncident = buildIncident(IncidentStatus.CLOSED);
    transferredServiceIncident.setCreatorServiceId(serviceId);
    transferredServiceIncident.setTransferredToService(transferredServiceId);
    transferredServiceIncident.setClosedAt(closedAt);

    entityManager.persist(creatorServiceIncident1);
    entityManager.persist(creatorServiceIncident2);
    entityManager.persist(transferredServiceIncident);
    entityManager.flush();

    List<Object[]> rows = repository.findTopServices(
      closedAt.minusDays(1),
      closedAt.plusDays(1)
    );

    assertThat(rows).hasSize(2);
    assertThat(rows.get(0)[0]).isEqualTo(serviceId);
    assertThat(((Number) rows.get(0)[1]).longValue()).isEqualTo(2L);
    assertThat(rows.get(1)[0]).isEqualTo(transferredServiceId);
    assertThat(((Number) rows.get(1)[1]).longValue()).isEqualTo(1L);
  }

  @Test
  @DisplayName(
    "findTopResolvers - Attributes resolution to resolver, not closer"
  )
  void findTopResolvers_UsesResolverInsteadOfCloser() {
    LocalDateTime oldResolutionAt = LocalDateTime.now().minusDays(3);
    LocalDateTime resolutionAt = LocalDateTime.now().minusDays(2);
    LocalDateTime closureAt = LocalDateTime.now().minusDays(1);
    Incident incident = buildIncident(IncidentStatus.CLOSED);
    incident.setAssignedTo(assigneeId);
    incident.setResolvedAt(resolutionAt);
    incident.setClosedAt(closureAt);
    entityManager.persist(incident);
    UUID resolverId = UUID.randomUUID();
    UUID oldResolverId = UUID.randomUUID();
    UUID closerId = creatorId;
    IncidentHistory oldResolution = new IncidentHistory();
    oldResolution.setIncident(incident);
    oldResolution.setUserId(oldResolverId);
    oldResolution.setAction(ActionType.STATUS_CHANGE);
    oldResolution.setNewValue(IncidentStatus.RESOLVED.name());
    entityManager.persist(oldResolution);
    IncidentHistory resolution = new IncidentHistory();
    resolution.setIncident(incident);
    resolution.setUserId(resolverId);
    resolution.setAction(ActionType.STATUS_CHANGE);
    resolution.setNewValue(IncidentStatus.RESOLVED.name());
    entityManager.persist(resolution);
    IncidentHistory closure = new IncidentHistory();
    closure.setIncident(incident);
    closure.setUserId(closerId);
    closure.setAction(ActionType.STATUS_CHANGE);
    closure.setNewValue(IncidentStatus.CLOSED.name());
    entityManager.persist(closure);
    entityManager.flush();
    entityManager
      .getEntityManager()
      .createNativeQuery(
        "UPDATE incident_histories SET created_at = :createdAt WHERE id = :id"
      )
      .setParameter("createdAt", oldResolutionAt)
      .setParameter("id", oldResolution.getId())
      .executeUpdate();
    entityManager
      .getEntityManager()
      .createNativeQuery(
        "UPDATE incident_histories SET created_at = :createdAt WHERE id = :id"
      )
      .setParameter("createdAt", resolutionAt)
      .setParameter("id", resolution.getId())
      .executeUpdate();
    entityManager.clear();

    List<Object[]> rows = repository.findTopResolvers(
      LocalDateTime.now().minusDays(7),
      LocalDateTime.now()
    );

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)[0]).isEqualTo(resolverId);
    assertThat(((Number) rows.get(0)[1]).longValue()).isEqualTo(1L);
  }

  @Test
  @DisplayName("save - Persists incident and generates ID")
  void save_ValidIncident_PersistsAndGeneratesId() {
    Incident incident = buildIncident(IncidentStatus.OPEN);

    Incident saved = repository.save(incident);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTitle()).isEqualTo("Test incident");
    assertThat(saved.getTypeId()).isEqualTo(typeConfigId);
    assertThat(saved.getStatus()).isEqualTo(IncidentStatus.OPEN);
  }

  @Test
  @DisplayName(
    "subjectUserId filter - union of created, assigned and history-touched incidents"
  )
  void subjectUserFilter_UnionOfCreatedAssignedAndHistory() {
    UUID subject = UUID.randomUUID();
    UUID someoneElse = UUID.randomUUID();

    Incident created = buildIncident(IncidentStatus.OPEN);
    created.setCreatedBy(subject);

    Incident assigned = buildIncident(IncidentStatus.IN_PROGRESS);
    assigned.setCreatedBy(someoneElse);
    assigned.setAssignedTo(subject);

    // Ni createur ni assigne : seulement intervenu dans l'historique (resolu puis reassigne).
    Incident touched = buildIncident(IncidentStatus.IN_PROGRESS);
    touched.setCreatedBy(someoneElse);
    touched.setAssignedTo(someoneElse);

    Incident unrelated = buildIncident(IncidentStatus.OPEN);
    unrelated.setCreatedBy(someoneElse);
    unrelated.setAssignedTo(someoneElse);

    entityManager.persist(created);
    entityManager.persist(assigned);
    entityManager.persist(touched);
    entityManager.persist(unrelated);

    IncidentHistory action = new IncidentHistory();
    action.setIncident(touched);
    action.setUserId(subject);
    action.setAction(ActionType.STATUS_CHANGE);
    entityManager.persist(action);
    entityManager.flush();

    List<Incident> result = repository
      .findAll(
        IncidentSpecification.getFilterSpecification(
          IncidentSearchCriteria.builder()
            .view("all")
            .subjectUserId(subject)
            .build(),
          reportViewer()
        ),
        PageRequest.of(0, 100)
      )
      .getContent();

    assertThat(result)
      .extracting(Incident::getId)
      .containsExactlyInAnyOrder(
        created.getId(),
        assigned.getId(),
        touched.getId()
      );
  }

  @Test
  @DisplayName(
    "subjectUserId filter - period covers resolution/closure, not only creation"
  )
  void subjectUserFilter_PeriodCoversClosureNotOnlyCreation() {
    UUID subject = UUID.randomUUID();
    LocalDate from = LocalDate.now().minusDays(30);
    LocalDate to = LocalDate.now().plusDays(1);

    // Cree hors periode (il y a 60 jours) mais cloture dans la periode.
    Incident closedInPeriod = buildIncident(IncidentStatus.CLOSED);
    closedInPeriod.setAssignedTo(subject);
    closedInPeriod.setClosedAt(LocalDateTime.now());

    // Cree hors periode, sans resolution ni cloture : hors fenetre.
    Incident oldNoActivity = buildIncident(IncidentStatus.IN_PROGRESS);
    oldNoActivity.setAssignedTo(subject);

    entityManager.persist(closedInPeriod);
    entityManager.persist(oldNoActivity);
    entityManager.flush();

    setPersistedCreatedAt(closedInPeriod, LocalDateTime.now().minusDays(60));
    setPersistedCreatedAt(oldNoActivity, LocalDateTime.now().minusDays(60));

    List<Incident> result = repository
      .findAll(
        IncidentSpecification.getFilterSpecification(
          IncidentSearchCriteria.builder()
            .view("all")
            .subjectUserId(subject)
            .startDate(from)
            .endDate(to)
            .build(),
          reportViewer()
        ),
        PageRequest.of(0, 100)
      )
      .getContent();

    assertThat(result)
      .extracting(Incident::getId)
      .containsExactly(closedInPeriod.getId());
  }

  @Test
  @DisplayName(
    "terminal exits count every departure and reopenings count as re-entries"
  )
  void terminalExits_countEachDepartureAndReEntry() {
    // Rejete puis rouvert puis cloture sur la meme periode : l'incident quitte deux
    // fois le stock et y revient une fois. Dedoublonner les sorties compensait en
    // silence une reentree que rien ne comptait.
    Incident reopened = buildIncident(IncidentStatus.CLOSED);
    entityManager.persist(reopened);
    persistHistory(reopened, ActionType.STATUS_CHANGE, null, IncidentStatus.REJECTED);
    persistHistory(reopened, ActionType.REOPENING, IncidentStatus.REJECTED, IncidentStatus.REOPENED);
    persistHistory(reopened, ActionType.STATUS_CHANGE, null, IncidentStatus.CLOSED);

    Incident closedOnce = buildIncident(IncidentStatus.CLOSED);
    entityManager.persist(closedOnce);
    persistHistory(closedOnce, ActionType.STATUS_CHANGE, null, IncidentStatus.CLOSED);
    entityManager.flush();

    LocalDateTime from = LocalDateTime.now().minusDays(7);
    LocalDateTime to = LocalDateTime.now().plusDays(1);

    List<Object[]> exits = repository.findTerminalExitsByStatusInPeriod(
      IncidentStatus.namesOf(IncidentStatus.TERMINAL_STATUSES),
      null,
      agencyId,
      null,
      from,
      to
    );

    assertThat(exits)
      .extracting(row -> row[0], row -> ((Number) row[1]).longValue())
      .containsExactlyInAnyOrder(
        tuple(IncidentStatus.CLOSED.getName(), 2L),
        tuple(IncidentStatus.REJECTED.getName(), 1L)
      );

    assertThat(
      repository.countBacklogReEntriesInPeriod(
        IncidentStatus.namesOf(IncidentStatus.TERMINAL_STATUSES),
        null,
        agencyId,
        null,
        from,
        to
      )
    ).isEqualTo(1L);
  }

  private void persistHistory(
    Incident incident,
    ActionType action,
    IncidentStatus oldStatus,
    IncidentStatus newStatus
  ) {
    IncidentHistory history = new IncidentHistory();
    history.setIncident(incident);
    history.setUserId(assigneeId);
    history.setAction(action);
    if (oldStatus != null) history.setOldValue(oldStatus.getName());
    history.setNewValue(newStatus.getName());
    entityManager.persist(history);
  }

  private UserDetailsImpl reportViewer() {
    return new UserDetailsImpl(
      UUID.randomUUID(),
      "report-viewer",
      "pwd",
      true,
      List.of()
    );
  }
}
