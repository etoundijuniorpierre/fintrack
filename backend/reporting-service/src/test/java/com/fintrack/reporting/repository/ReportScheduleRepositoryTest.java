package com.fintrack.reporting.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.entity.ReportSchedule;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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
class ReportScheduleRepositoryTest {

  @Autowired
  private ReportScheduleRepository reportScheduleRepository;

  @Autowired
  private TestEntityManager entityManager;

  private UUID createdBy;
  private UUID otherUserId;

  @BeforeEach
  void setUp() {
    createdBy = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
  }

  private ReportSchedule buildReportSchedule(UUID createdBy) {
    ReportSchedule schedule = new ReportSchedule();
    schedule.setName("Daily Report");
    schedule.setType(ReportType.DAILY);
    schedule.setFormat(ReportFormat.PDF);
    schedule.setRecipientEmails("test@example.com,admin@example.com");
    schedule.setSendTime(LocalTime.of(8, 0));
    schedule.setScope("all");
    schedule.setCreatedBy(createdBy);
    return schedule;
  }

  @Test
  @DisplayName("findByCreatedBy - Returns schedules created by given user")
  void findByCreatedBy_ReturnsMatchingSchedules() {
    ReportSchedule s1 = buildReportSchedule(createdBy);
    ReportSchedule s2 = buildReportSchedule(createdBy);
    s2.setName("Weekly Report");
    s2.setType(ReportType.WEEKLY);
    ReportSchedule other = buildReportSchedule(otherUserId);

    entityManager.persist(s1);
    entityManager.persist(s2);
    entityManager.persist(other);
    entityManager.flush();

    List<ReportSchedule> result = reportScheduleRepository.findByCreatedBy(
      createdBy
    );

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(s -> s.getCreatedBy().equals(createdBy));
  }

  @Test
  @DisplayName(
    "findByCreatedBy - Returns empty when no schedules created by user"
  )
  void findByCreatedBy_NoMatch_ReturnsEmpty() {
    assertThat(
      reportScheduleRepository.findByCreatedBy(UUID.randomUUID())
    ).isEmpty();
  }

  @Test
  @DisplayName("findByIsActiveTrue - Returns only active schedules")
  void findByIsActiveTrue_ReturnsOnlyActiveSchedules() {
    ReportSchedule active1 = buildReportSchedule(createdBy);
    ReportSchedule active2 = buildReportSchedule(createdBy);
    active2.setName("Weekly Report");
    ReportSchedule inactive = buildReportSchedule(createdBy);
    inactive.setName("Inactive Report");
    inactive.setActive(false);

    entityManager.persist(active1);
    entityManager.persist(active2);
    entityManager.persist(inactive);
    entityManager.flush();

    List<ReportSchedule> result = reportScheduleRepository.findByIsActiveTrue();

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(s -> s.isActive());
  }

  @Test
  @DisplayName("findByIsActiveTrue - Returns empty when no active schedules")
  void findByIsActiveTrue_NoMatch_ReturnsEmpty() {
    ReportSchedule inactive = buildReportSchedule(createdBy);
    inactive.setActive(false);
    entityManager.persist(inactive);
    entityManager.flush();

    assertThat(reportScheduleRepository.findByIsActiveTrue()).isEmpty();
  }

  @Test
  @DisplayName("save - Persists schedule and generates ID")
  void save_ValidSchedule_PersistsAndGeneratesId() {
    ReportSchedule schedule = buildReportSchedule(createdBy);

    ReportSchedule saved = reportScheduleRepository.save(schedule);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("Daily Report");
    assertThat(saved.getType()).isEqualTo(ReportType.DAILY);
    assertThat(saved.getFormat()).isEqualTo(ReportFormat.PDF);
    assertThat(saved.getCreatedBy()).isEqualTo(createdBy);
    assertThat(saved.isActive()).isTrue();
  }

  @Test
  @DisplayName("save - Persists schedule with audit fields")
  void save_ValidSchedule_PersistsWithAuditFields() {
    ReportSchedule schedule = buildReportSchedule(createdBy);

    ReportSchedule saved = reportScheduleRepository.save(schedule);

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    // createdAt et updatedAt sont positionnés par l'auditing Spring Data lors du même
    // appel save() ; on tolère un écart sub-milliseconde car les deux instants peuvent
    // être pris à des nanosecondes différentes par l'auditor.
    assertThat(saved.getCreatedAt()).isCloseTo(
      saved.getUpdatedAt(),
      within(1, ChronoUnit.MILLIS)
    );
  }

  @Test
  @DisplayName("update - Updates audit fields on modification")
  void update_ExistingSchedule_UpdatesAuditFields() {
    ReportSchedule schedule = buildReportSchedule(createdBy);
    ReportSchedule saved = reportScheduleRepository.saveAndFlush(schedule);
    entityManager.clear();

    // Decale legerement l'horodatage pour verifier le tri temporel.
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    ReportSchedule toUpdate = reportScheduleRepository
      .findById(saved.getId())
      .orElseThrow();
    toUpdate.setName("Updated Report");
    ReportSchedule updated = reportScheduleRepository.saveAndFlush(toUpdate);

    assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
  }

  @Test
  @DisplayName("save - Persists schedule with default values")
  void save_ValidSchedule_PersistsWithDefaultValues() {
    ReportSchedule schedule = buildReportSchedule(createdBy);

    ReportSchedule saved = reportScheduleRepository.save(schedule);

    assertThat(saved.isActive()).isTrue();
    assertThat(saved.getLastGeneratedAt()).isNull();
  }
}
