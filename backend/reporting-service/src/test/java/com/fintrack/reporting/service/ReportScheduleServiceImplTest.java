package com.fintrack.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.exception.EntityNotFoundException;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.entity.ReportSchedule;
import com.fintrack.reporting.repository.ReportScheduleRepository;
import com.fintrack.reporting.service.impl.ReportScheduleServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class ReportScheduleServiceImplTest {

  @Mock
  private ReportScheduleRepository reportScheduleRepository;

  @Mock
  private AuditServiceClientService auditServiceClientService;

  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private ReportScheduleServiceImpl reportScheduleService;

  private ReportSchedule testSchedule;
  private UUID testId;
  private UUID createdBy;

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    createdBy = UUID.randomUUID();

    testSchedule = new ReportSchedule();
    testSchedule.setId(testId);
    testSchedule.setName("Daily Report");
    testSchedule.setType(ReportType.DAILY);
    testSchedule.setFormat(ReportFormat.PDF);
    testSchedule.setContentType(ReportContentType.INCIDENT_TYPE_ANALYSIS);
    testSchedule.setScope("own");
    testSchedule.setRecipientEmails("admin@example.com");
    testSchedule.setCreatedBy(createdBy);
    testSchedule.setActive(true);
  }

  // ── findAll (pageable) ────────────────────────────────────────────────────

  @Test
  @DisplayName("findAll(pageable) - Returns page of schedules")
  void findAll_Pageable_ReturnsPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<ReportSchedule> page = new PageImpl<>(
      List.of(testSchedule),
      pageable,
      1
    );
    when(reportScheduleRepository.findAll(pageable)).thenReturn(page);

    Page<ReportSchedule> result = reportScheduleService.findAll(pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(testId);
  }

  // ── findAll (list) ────────────────────────────────────────────────────────

  @Test
  @DisplayName("findAll() - Returns list of all schedules")
  void findAll_ReturnsList() {
    when(reportScheduleRepository.findAll()).thenReturn(List.of(testSchedule));

    List<ReportSchedule> result = reportScheduleService.findAll();

    assertThat(result).hasSize(1);
  }

  // ── findById ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("findById - Returns schedule when found")
  void findById_Found_ReturnsSchedule() {
    when(reportScheduleRepository.findById(testId)).thenReturn(
      Optional.of(testSchedule)
    );

    ReportSchedule result = reportScheduleService.findById(testId);

    assertThat(result.getId()).isEqualTo(testId);
    assertThat(result.getName()).isEqualTo("Daily Report");
  }

  @Test
  @DisplayName("findById - Throws EntityNotFoundException when not found")
  void findById_NotFound_ThrowsException() {
    UUID unknownId = UUID.randomUUID();
    when(reportScheduleRepository.findById(unknownId)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      reportScheduleService.findById(unknownId)
    ).isInstanceOf(EntityNotFoundException.class);
  }

  // ── findByCreatedBy ───────────────────────────────────────────────────────

  @Test
  @DisplayName("findByCreatedBy - Returns schedules for user")
  void findByCreatedBy_ReturnsSchedules() {
    when(reportScheduleRepository.findByCreatedBy(createdBy)).thenReturn(
      List.of(testSchedule)
    );

    List<ReportSchedule> result = reportScheduleService.findByCreatedBy(
      createdBy
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCreatedBy()).isEqualTo(createdBy);
  }

  @Test
  @DisplayName("findByCreatedBy - Returns empty list when no schedules")
  void findByCreatedBy_NoMatch_ReturnsEmpty() {
    when(reportScheduleRepository.findByCreatedBy(any())).thenReturn(List.of());

    assertThat(
      reportScheduleService.findByCreatedBy(UUID.randomUUID())
    ).isEmpty();
  }

  // ── findActive ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("findActive - Returns only active schedules")
  void findActive_ReturnsActiveSchedules() {
    when(reportScheduleRepository.findByIsActiveTrue()).thenReturn(
      List.of(testSchedule)
    );

    List<ReportSchedule> result = reportScheduleService.findActive();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isActive()).isTrue();
  }

  // ── create ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("create - Sets createdBy and isActive=true, then saves")
  void create_SetsCreatedByAndIsActive() {
    ReportSchedule input = new ReportSchedule();
    input.setName("New Report");
    input.setType(ReportType.WEEKLY);
    input.setFormat(ReportFormat.EXCEL);
    input.setScope("agency");
    input.setWeekDay(1);

    when(reportScheduleRepository.save(any(ReportSchedule.class))).thenAnswer(
      inv -> {
        ReportSchedule s = inv.getArgument(0);
        s.setId(UUID.randomUUID()); // simulate DB-generated id
        return s;
      }
    );

    ReportSchedule result = reportScheduleService.create(input, createdBy);

    assertThat(result.getCreatedBy()).isEqualTo(createdBy);
    assertThat(result.isActive()).isTrue();
    verify(reportScheduleRepository).save(input);
  }

  @Test
  @DisplayName("create - Accepts reports grouped by user")
  void create_UserScope_IsAccepted() {
    testSchedule.setScope("user");
    when(reportScheduleRepository.save(testSchedule)).thenReturn(testSchedule);

    ReportSchedule result = reportScheduleService.create(
      testSchedule,
      createdBy
    );

    assertThat(result.getScope()).isEqualTo("user");
  }

  // ── update ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("update - Updates fields and returns schedule")
  void update_ExistingSchedule_UpdatesAndReturns() {
    ReportSchedule details = new ReportSchedule();
    details.setName("Updated Report");
    details.setType(ReportType.MONTHLY);
    details.setContentType(null);
    details.setFormat(ReportFormat.EXCEL);
    details.setScope("all");
    details.setRecipientEmails("new@example.com");

    when(reportScheduleRepository.findById(testId)).thenReturn(
      Optional.of(testSchedule)
    );
    when(
      reportScheduleRepository.saveAndFlush(any(ReportSchedule.class))
    ).thenAnswer(inv -> inv.getArgument(0));

    ReportSchedule result = reportScheduleService.update(testId, details);

    assertThat(result.getName()).isEqualTo("Updated Report");
    assertThat(result.getType()).isEqualTo(ReportType.MONTHLY);
    assertThat(result.getFormat()).isEqualTo(ReportFormat.EXCEL);
    assertThat(result.getScope()).isEqualTo("all");
    assertThat(result.getRecipientEmails()).isEqualTo("new@example.com");
    assertThat(result.getContentType()).isEqualTo(
      ReportContentType.INCIDENT_TYPE_ANALYSIS
    );
  }

  @Test
  @DisplayName("update - Applies an explicitly selected content type")
  void update_ExplicitContentType_AppliesSelection() {
    ReportSchedule details = new ReportSchedule();
    details.setName("Updated Report");
    details.setType(ReportType.MONTHLY);
    details.setContentType(ReportContentType.OPERATIONAL);
    details.setFormat(ReportFormat.EXCEL);
    details.setScope("all");
    when(reportScheduleRepository.findById(testId)).thenReturn(
      Optional.of(testSchedule)
    );
    when(reportScheduleRepository.saveAndFlush(testSchedule)).thenReturn(
      testSchedule
    );

    ReportSchedule result = reportScheduleService.update(testId, details);

    assertThat(result.getContentType()).isEqualTo(ReportContentType.OPERATIONAL);
  }

  @Test
  @DisplayName("update - Throws EntityNotFoundException when not found")
  void update_NotFound_ThrowsException() {
    UUID unknownId = UUID.randomUUID();
    when(reportScheduleRepository.findById(unknownId)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      reportScheduleService.update(unknownId, testSchedule)
    ).isInstanceOf(EntityNotFoundException.class);
  }

  // ── toggleActive ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("toggleActive - Toggles from true to false")
  void toggleActive_TrueToFalse() {
    testSchedule.setActive(true);
    when(reportScheduleRepository.findById(testId)).thenReturn(
      Optional.of(testSchedule)
    );
    when(reportScheduleRepository.saveAndFlush(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );

    ReportSchedule result = reportScheduleService.toggleActive(testId);

    assertThat(result.isActive()).isFalse();
  }

  @Test
  @DisplayName("toggleActive - Toggles from false to true")
  void toggleActive_FalseToTrue() {
    testSchedule.setActive(false);
    when(reportScheduleRepository.findById(testId)).thenReturn(
      Optional.of(testSchedule)
    );
    when(reportScheduleRepository.saveAndFlush(any())).thenAnswer(inv ->
      inv.getArgument(0)
    );

    ReportSchedule result = reportScheduleService.toggleActive(testId);

    assertThat(result.isActive()).isTrue();
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("delete - Deletes schedule when found")
  void delete_ExistingSchedule_Deletes() {
    when(reportScheduleRepository.existsById(testId)).thenReturn(true);

    reportScheduleService.delete(testId);

    verify(reportScheduleRepository).deleteById(testId);
  }

  @Test
  @DisplayName("delete - Throws EntityNotFoundException when not found")
  void delete_NotFound_ThrowsException() {
    UUID unknownId = UUID.randomUUID();
    when(reportScheduleRepository.existsById(unknownId)).thenReturn(false);

    assertThatThrownBy(() ->
      reportScheduleService.delete(unknownId)
    ).isInstanceOf(EntityNotFoundException.class);
  }
}
