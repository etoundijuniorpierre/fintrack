package com.fintrack.incident.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentComment;
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
class IncidentCommentRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private IncidentCommentRepository repository;

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
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    incident.setCreatedBy(userId);
    incident.setAgencyId(UUID.randomUUID());
    entityManager.persist(incident);

    entityManager.flush();
  }

  private IncidentComment buildComment(String content, boolean isInternal) {
    IncidentComment c = new IncidentComment();
    c.setIncident(incident);
    c.setUserId(userId);
    c.setContent(content);
    c.setInternal(isInternal);
    return c;
  }

  @Test
  @DisplayName(
    "findByIncidentIdOrderByCreatedAtAsc - Returns all comments ordered"
  )
  void findByIncidentIdOrderByCreatedAtAsc_ReturnsAllComments() {
    entityManager.persist(buildComment("First comment", false));
    entityManager.persist(buildComment("Internal note", true));
    entityManager.persist(buildComment("Second comment", false));
    entityManager.flush();

    List<IncidentComment> result =
      repository.findByIncidentIdOrderByCreatedAtAsc(incident.getId());

    assertThat(result).hasSize(3);
    assertThat(result).allMatch(c ->
      c.getIncident().getId().equals(incident.getId())
    );
  }

  @Test
  @DisplayName(
    "findByIncidentIdOrderByCreatedAtAsc - Returns empty for unknown incident"
  )
  void findByIncidentIdOrderByCreatedAtAsc_UnknownId_ReturnsEmpty() {
    List<IncidentComment> result =
      repository.findByIncidentIdOrderByCreatedAtAsc(UUID.randomUUID());
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName(
    "findByIncidentIdAndIsInternalOrderByCreatedAtAsc - Returns only public comments"
  )
  void findByIncidentIdAndIsInternal_False_ReturnsPublicOnly() {
    entityManager.persist(buildComment("Public 1", false));
    entityManager.persist(buildComment("Internal", true));
    entityManager.persist(buildComment("Public 2", false));
    entityManager.flush();

    List<IncidentComment> result =
      repository.findByIncidentIdAndIsInternalOrderByCreatedAtAsc(
        incident.getId(),
        false
      );

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(c -> !c.isInternal());
  }

  @Test
  @DisplayName(
    "findByIncidentIdAndIsInternalOrderByCreatedAtAsc - Returns only internal comments"
  )
  void findByIncidentIdAndIsInternal_True_ReturnsInternalOnly() {
    entityManager.persist(buildComment("Public", false));
    entityManager.persist(buildComment("Internal 1", true));
    entityManager.persist(buildComment("Internal 2", true));
    entityManager.flush();

    List<IncidentComment> result =
      repository.findByIncidentIdAndIsInternalOrderByCreatedAtAsc(
        incident.getId(),
        true
      );

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(IncidentComment::isInternal);
  }

  @Test
  @DisplayName(
    "findByIncidentIdAndIsInternalOrderByCreatedAtAsc - Returns empty when no match"
  )
  void findByIncidentIdAndIsInternal_NoMatch_ReturnsEmpty() {
    entityManager.persist(buildComment("Public", false));
    entityManager.flush();

    List<IncidentComment> result =
      repository.findByIncidentIdAndIsInternalOrderByCreatedAtAsc(
        incident.getId(),
        true
      );

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("save - Persists comment with all fields")
  void save_ValidComment_PersistsCorrectly() {
    IncidentComment comment = buildComment("Test content", false);

    IncidentComment saved = repository.save(comment);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getContent()).isEqualTo("Test content");
    assertThat(saved.isInternal()).isFalse();
    assertThat(saved.getUserId()).isEqualTo(userId);
  }
}
