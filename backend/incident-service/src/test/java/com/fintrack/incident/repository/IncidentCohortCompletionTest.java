package com.fintrack.incident.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

// Convention cohorte : les encore-ouverts comptent au denominateur, et un percentile
// que la cohorte n'a pas atteint doit rester declare comme tel.
@DataJpaTest
@ActiveProfiles("test")
class IncidentCohortCompletionTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private IncidentRepository repository;

  private UUID agencyId;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    agencyId = UUID.randomUUID();
    now = LocalDateTime.now();
  }

  @Test
  @DisplayName(
    "findCohortCompletionScoped - Reaches p50 when most of the cohort is closed"
  )
  void cohortCompletion_ReachesPercentileWhenEnoughIsClosed() {
    // 4 clotures (1h, 2h, 3h, 4h) et 1 encore ouvert depuis 100h.
    persistClosed(1);
    persistClosed(2);
    persistClosed(3);
    persistClosed(4);
    persistOpen(100);

    Object[] row = repository
      .findCohortCompletionScoped(null, agencyId, null, null, null, now)
      .get(0);

    assertThat(((Number) row[0]).longValue()).isEqualTo(5L);
    assertThat(((Number) row[1]).longValue()).isEqualTo(4L);
    // 50 % de 5 = rang 3 parmi les clotures triees : 3h.
    assertThat(((Number) row[2]).doubleValue()).isCloseTo(3.0, within(0.05));
    // 90 % de 5 = rang 5 : seules 4 clotures existent, le p90 n'est pas atteint.
    assertThat(row[3]).isNull();
    // L'anciennete des encore-ouverts est rendue visible, pas ecartee.
    assertThat(((Number) row[4]).doubleValue()).isCloseTo(100.0, within(0.05));
    assertThat(((Number) row[5]).doubleValue()).isCloseTo(100.0, within(0.05));
  }

  @Test
  @DisplayName(
    "findCohortCompletionScoped - Leaves p50 unreached when the cohort is mostly open"
  )
  void cohortCompletion_LeavesPercentileUnreachedWhenMostlyOpen() {
    // Un seul incident cloture sur quatre : la moitie de la cohorte ne l'est pas.
    persistClosed(2);
    persistOpen(50);
    persistOpen(60);
    persistOpen(70);
    // Un rejete : il ne peut pas se cloturer, il sort du denominateur.
    persistRejected();

    Object[] row = repository
      .findCohortCompletionScoped(null, agencyId, null, null, null, now)
      .get(0);

    assertThat(((Number) row[0]).longValue()).isEqualTo(4L);
    assertThat(((Number) row[1]).longValue()).isEqualTo(1L);
    assertThat(row[2]).isNull();
    assertThat(row[3]).isNull();
  }

  private void persistClosed(long hours) {
    Incident incident = persist(IncidentStatus.CLOSED, hours);
    setTimestamps(incident, hours, now);
  }

  private void persistOpen(long ageHours) {
    Incident incident = persist(IncidentStatus.IN_PROGRESS, ageHours);
    setTimestamps(incident, ageHours, null);
  }

  private void persistRejected() {
    Incident incident = persist(IncidentStatus.REJECTED, 5);
    setTimestamps(incident, 5, null);
  }

  private Incident persist(IncidentStatus status, long ageHours) {
    Incident incident = new Incident();
    incident.setTitle("Test incident");
    incident.setDescription("Description");
    incident.setTypeId(UUID.randomUUID());
    incident.setCriticality(Criticality.MEDIUM);
    incident.setStatus(status);
    incident.setCreatedBy(UUID.randomUUID());
    incident.setAgencyId(agencyId);
    return entityManager.persist(incident);
  }

  // created_at est pose par le callback JPA : on le repositionne en natif.
  private void setTimestamps(
    Incident incident,
    long ageHours,
    LocalDateTime closedAt
  ) {
    entityManager.flush();
    entityManager
      .getEntityManager()
      .createNativeQuery(
        "UPDATE incidents SET created_at = :createdAt, closed_at = :closedAt WHERE id = :id"
      )
      .setParameter("createdAt", now.minusHours(ageHours))
      .setParameter("closedAt", closedAt)
      .setParameter("id", incident.getId())
      .executeUpdate();
    entityManager.clear();
  }
}
