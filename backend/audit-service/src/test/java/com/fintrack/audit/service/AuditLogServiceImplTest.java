package com.fintrack.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.audit.exception.EntityNotFoundException;
import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.entity.AuditLog;
import com.fintrack.audit.repository.AuditLogRepository;
import com.fintrack.audit.service.impl.AuditLogServiceImpl;
import java.time.LocalDateTime;
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

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

  @Mock
  private AuditLogRepository auditLogRepository;

  @InjectMocks
  private AuditLogServiceImpl auditLogService;

  private AuditLog testLog;
  private String testId;
  private UUID userId;

  @BeforeEach
  void setUp() {
    testId = "507f1f77bcf86cd799439011";
    userId = UUID.randomUUID();

    testLog = new AuditLog();
    testLog.setId(testId);
    testLog.setUserId(userId);
    testLog.setUsername("jdoe");
    testLog.setAction(AuditAction.INCIDENT_CREATE);
    testLog.setResourceType("INCIDENT");
    testLog.setResourceId("incident-123");
    testLog.setStatus(AuditStatus.SUCCESS);
    testLog.setTimestamp(LocalDateTime.now());
  }

  // ── findAll ───────────────────────────────────────────────────────────────

  @Test
  @DisplayName("findAll(pageable) - Returns page of audit logs")
  void findAll_Pageable_ReturnsPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<AuditLog> page = new PageImpl<>(List.of(testLog), pageable, 1);
    when(auditLogRepository.findAll(pageable)).thenReturn(page);

    Page<AuditLog> result = auditLogService.findAll(pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(testId);
  }

  // ── findById ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("findById - Returns audit log when found")
  void findById_Found_ReturnsLog() {
    when(auditLogRepository.findById(testId)).thenReturn(Optional.of(testLog));

    AuditLog result = auditLogService.findById(testId);

    assertThat(result.getId()).isEqualTo(testId);
    assertThat(result.getAction()).isEqualTo(AuditAction.INCIDENT_CREATE);
  }

  @Test
  @DisplayName("findById - Throws EntityNotFoundException when not found")
  void findById_NotFound_ThrowsException() {
    when(auditLogRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> auditLogService.findById("unknown")).isInstanceOf(
      EntityNotFoundException.class
    );
  }

  // ── findByUserId ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("findByUserId - Returns logs for user")
  void findByUserId_ReturnsLogs() {
    when(auditLogRepository.findByUserId(userId)).thenReturn(List.of(testLog));

    List<AuditLog> result = auditLogService.findByUserId(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUserId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("findByUserId - Returns empty list when no logs")
  void findByUserId_NoMatch_ReturnsEmpty() {
    when(auditLogRepository.findByUserId(any())).thenReturn(List.of());

    assertThat(auditLogService.findByUserId(UUID.randomUUID())).isEmpty();
  }

  // ── findByAction ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("findByAction - Returns logs for action")
  void findByAction_ReturnsLogs() {
    when(
      auditLogRepository.findByAction(AuditAction.INCIDENT_CREATE)
    ).thenReturn(List.of(testLog));

    List<AuditLog> result = auditLogService.findByAction(
      AuditAction.INCIDENT_CREATE
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getAction()).isEqualTo(
      AuditAction.INCIDENT_CREATE
    );
  }

  // ── findByResourceType ────────────────────────────────────────────────────

  @Test
  @DisplayName("findByResourceType - Returns logs for resource type")
  void findByResourceType_ReturnsLogs() {
    when(auditLogRepository.findByResourceType("INCIDENT")).thenReturn(
      List.of(testLog)
    );

    List<AuditLog> result = auditLogService.findByResourceType("INCIDENT");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getResourceType()).isEqualTo("INCIDENT");
  }

  // ── findByResourceTypeAndResourceId ───────────────────────────────────────

  @Test
  @DisplayName(
    "findByResourceTypeAndResourceId - Returns logs for specific resource"
  )
  void findByResourceTypeAndResourceId_ReturnsLogs() {
    when(
      auditLogRepository.findByResourceTypeAndResourceId(
        "INCIDENT",
        "incident-123"
      )
    ).thenReturn(List.of(testLog));

    List<AuditLog> result = auditLogService.findByResourceTypeAndResourceId(
      "INCIDENT",
      "incident-123"
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getResourceId()).isEqualTo("incident-123");
  }

  // ── findByStatus ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("findByStatus - Returns logs with given status")
  void findByStatus_ReturnsLogs() {
    when(auditLogRepository.findByStatus(AuditStatus.SUCCESS)).thenReturn(
      List.of(testLog)
    );

    List<AuditLog> result = auditLogService.findByStatus(AuditStatus.SUCCESS);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(AuditStatus.SUCCESS);
  }

  // ── findByTimestampBetween ────────────────────────────────────────────────

  @Test
  @DisplayName("findByTimestampBetween - Returns logs within time range")
  void findByTimestampBetween_ReturnsLogs() {
    LocalDateTime from = LocalDateTime.now().minusHours(1);
    LocalDateTime to = LocalDateTime.now().plusHours(1);
    when(auditLogRepository.findByTimestampBetween(from, to)).thenReturn(
      List.of(testLog)
    );

    List<AuditLog> result = auditLogService.findByTimestampBetween(from, to);

    assertThat(result).hasSize(1);
  }

  // ── findByUserIdAndTimestampBetween ───────────────────────────────────────

  @Test
  @DisplayName(
    "findByUserIdAndTimestampBetween - Returns logs for user within time range"
  )
  void findByUserIdAndTimestampBetween_ReturnsLogs() {
    LocalDateTime from = LocalDateTime.now().minusHours(1);
    LocalDateTime to = LocalDateTime.now().plusHours(1);
    when(
      auditLogRepository.findByUserIdAndTimestampBetween(userId, from, to)
    ).thenReturn(List.of(testLog));

    List<AuditLog> result = auditLogService.findByUserIdAndTimestampBetween(
      userId,
      from,
      to
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUserId()).isEqualTo(userId);
  }

  // ── create ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("create - Sets timestamp when null and saves")
  void create_NullTimestamp_SetsTimestampAndSaves() {
    AuditLog input = new AuditLog();
    input.setAction(AuditAction.LOGIN_SUCCESS);
    input.setResourceType("USER");
    input.setStatus(AuditStatus.SUCCESS);
    input.setTimestamp(null);

    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv ->
      inv.getArgument(0)
    );

    AuditLog result = auditLogService.create(input);

    assertThat(result.getTimestamp()).isNotNull();
    verify(auditLogRepository).save(input);
  }

  @Test
  @DisplayName("create - Preserves existing timestamp")
  void create_ExistingTimestamp_PreservesTimestamp() {
    LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 15, 10, 0);
    testLog.setTimestamp(fixedTime);

    when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv ->
      inv.getArgument(0)
    );

    AuditLog result = auditLogService.create(testLog);

    assertThat(result.getTimestamp()).isEqualTo(fixedTime);
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("delete - Deletes audit log when found")
  void delete_ExistingLog_Deletes() {
    when(auditLogRepository.existsById(testId)).thenReturn(true);

    auditLogService.delete(testId);

    verify(auditLogRepository).deleteById(testId);
  }

  @Test
  @DisplayName("delete - Throws EntityNotFoundException when not found")
  void delete_NotFound_ThrowsException() {
    when(auditLogRepository.existsById("unknown")).thenReturn(false);

    assertThatThrownBy(() -> auditLogService.delete("unknown")).isInstanceOf(
      EntityNotFoundException.class
    );
  }
}
