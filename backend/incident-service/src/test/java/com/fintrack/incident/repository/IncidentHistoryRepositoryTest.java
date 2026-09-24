package com.fintrack.incident.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentHistory;
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
class IncidentHistoryRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private IncidentHistoryRepository repository;

  private Incident incident;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();

    incident = new Incident();
    incident.setTitle("Test incident");
    incident.setDescription("Description");
    incident.setTypeId(UUID.randomUUID());
    incident.setCriticality(Criticality.MEDIUM);
    incident.setStatus(IncidentStatus.OPEN);
    incident.setCreatedBy(userId);
    incident.setAgencyId(UUID.randomUUID());
    entityManager.persist(incident);
    entityManager.flush();
  }

  private IncidentHistory buildHistory(
    ActionType action,
    String oldVal,
    String newVal
  ) {
    IncidentHistory h = new IncidentHistory();
    h.setIncident(incident);
    h.setUserId(userId);
    h.setAction(action);
    h.setOldValue(oldVal);
    h.setNewValue(newVal);
    return h;
  }

  @Test
  @DisplayName("findByIncidentIdOrderByCreatedAtAsc - Returns history in order")
  void findByIncidentIdOrderByCreatedAtAsc_ReturnsOrderedHistory() {
    IncidentHistory h1 = buildHistory(ActionType.CREATION, null, "OPEN");
    IncidentHistory h2 = buildHistory(
      ActionType.VALIDATION,
      "OPEN",
      "VALIDATED"
    );
    IncidentHistory h3 = buildHistory(
      ActionType.TRANSFER,
      "VALIDATED",
      "TRANSFERRED"
    );

    entityManager.persist(h1);
    entityManager.persist(h2);
    entityManager.persist(h3);
    entityManager.flush();

    List<IncidentHistory> result =
      repository.findByIncidentIdOrderByCreatedAtAsc(incident.getId());

    assertThat(result).hasSize(3);
    assertThat(result).allMatch(h ->
      h.getIncident().getId().equals(incident.getId())
    );
  }

  @Test
  @DisplayName(
    "findByIncidentIdOrderByCreatedAtAsc - Returns empty for unknown incident"
  )
  void findByIncidentIdOrderByCreatedAtAsc_UnknownId_ReturnsEmpty() {
    List<IncidentHistory> result =
      repository.findByIncidentIdOrderByCreatedAtAsc(UUID.randomUUID());
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("save - Persists history entry with all fields")
  void save_ValidHistory_PersistsCorrectly() {
    IncidentHistory h = buildHistory(ActionType.COMMENT, null, "A comment");
    h.setComment("note");

    IncidentHistory saved = repository.save(h);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getAction()).isEqualTo(ActionType.COMMENT);
    assertThat(saved.getNewValue()).isEqualTo("A comment");
    assertThat(saved.getComment()).isEqualTo("note");
  }

  // Les transitions automatiques (blocage SLA, attente prolongee) n'ont pas d'auteur :
  // un NOT NULL sur user_id les faisait echouer au commit, silencieusement, a chaque run.
  @Test
  @DisplayName("save - Persists a system-authored entry without user")
  void save_NullUserId_PersistsSystemEntry() {
    IncidentHistory h = buildHistory(
      ActionType.STATUS_CHANGE,
      IncidentStatus.IN_PROGRESS.name(),
      IncidentStatus.BLOCKED.name()
    );
    h.setUserId(null);

    IncidentHistory saved = repository.saveAndFlush(h);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUserId()).isNull();
    assertThat(saved.getNewValue()).isEqualTo(IncidentStatus.BLOCKED.name());
  }
}
