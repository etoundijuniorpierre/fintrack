package com.fintrack.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.audit.TestcontainersConfiguration;
import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuditLogRepositoryTest {

  @Autowired
  private AuditLogRepository auditLogRepository;

  private UUID userId;
  private UUID otherUserId;
  private LocalDateTime baseTime;

  @BeforeEach
  void setUp() {
    auditLogRepository.deleteAll();
    userId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
    baseTime = LocalDateTime.now();
  }

  private AuditLog buildLog(
    UUID uid,
    AuditAction action,
    String resourceType,
    AuditStatus status
  ) {
    AuditLog log = new AuditLog();
    log.setUserId(uid);
    log.setUsername(
      uid != null ? "user-" + uid.toString().substring(0, 8) : "system"
    );
    log.setAction(action);
    log.setResourceType(resourceType);
    log.setResourceId("res-" + action.getName().toLowerCase());
    log.setStatus(status);
    log.setTimestamp(baseTime);
    return log;
  }

  // ── findByUserId ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("findByUserId - Returns logs for given user")
  void findByUserId_ReturnsMatchingLogs() {
    auditLogRepository.save(
      buildLog(
        userId,
        AuditAction.INCIDENT_CREATE,
        "INCIDENT",
        AuditStatus.SUCCESS
      )
    );
    auditLogRepository.save(
      buildLog(userId, AuditAction.USER_UPDATE, "USER", AuditStatus.SUCCESS)
    );
    auditLogRepository.save(
      buildLog(
        otherUserId,
        AuditAction.USER_DELETE,
        "USER",
        AuditStatus.SUCCESS
      )
    );

    List<AuditLog> result = auditLogRepository.findByUserId(userId);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(l -> userId.equals(l.getUserId()));
  }

  @Test
  @DisplayName("findByUserId - Returns empty when no logs for user")
  void findByUserId_NoMatch_ReturnsEmpty() {
    assertThat(auditLogRepository.findByUserId(UUID.randomUUID())).isEmpty();
  }

  // ── findByAction ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("findByAction - Returns logs with given action")
  void findByAction_ReturnsMatchingLogs() {
    auditLogRepository.save(
      buildLog(
        userId,
        AuditAction.INCIDENT_CREATE,
        "INCIDENT",
        AuditStatus.SUCCESS
      )
    );
    auditLogRepository.save(
      buildLog(
        userId,
        AuditAction.INCIDENT_CREATE,
        "INCIDENT",
        AuditStatus.SUCCESS
      )
    );
    auditLogRepository.save(
      buildLog(userId, AuditAction.USER_DELETE, "USER", AuditStatus.SUCCESS)
    );

    List<AuditLog> result = auditLogRepository.findByAction(
      AuditAction.INCIDENT_CREATE
    );

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(l ->
      AuditAction.INCIDENT_CREATE.equals(l.getAction())
    );
  }

  // ── findByResourceType ────────────────────────────────────────────────────

  @Test
  @DisplayName("findByResourceType - Returns logs for given resource type")
  void findByResourceType_ReturnsMatchingLogs() {
    auditLogRepository.save(
      buildLog(
        userId,
        AuditAction.INCIDENT_CREATE,
        "INCIDENT",
        AuditStatus.SUCCESS
      )
    );
    auditLogRepository.save(
      buildLog(
        userId,
        AuditAction.INCIDENT_VALIDATE,
        "INCIDENT",
        AuditStatus.SUCCESS
      )
    );
    auditLogRepository.save(
      buildLog(userId, AuditAction.USER_CREATE, "USER", AuditStatus.SUCCESS)
    );

    List<AuditLog> result = auditLogRepository.findByResourceType("INCIDENT");

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(l -> "INCIDENT".equals(l.getResourceType()));
  }

  // ── findByResourceTypeAndResourceId ───────────────────────────────────────

  @Test
  @DisplayName(
    "findByResourceTypeAndResourceId - Returns logs for specific resource"
  )
  void findByResourceTypeAndResourceId_ReturnsMatchingLogs() {
    AuditLog log1 = buildLog(
      userId,
      AuditAction.INCIDENT_CREATE,
      "INCIDENT",
      AuditStatus.SUCCESS
    );
    log1.setResourceId("incident-42");
    AuditLog log2 = buildLog(
      userId,
      AuditAction.INCIDENT_VALIDATE,
      "INCIDENT",
      AuditStatus.SUCCESS
    );
    log2.setResourceId("incident-42");
    AuditLog other = buildLog(
      userId,
      AuditAction.INCIDENT_CREATE,
      "INCIDENT",
      AuditStatus.SUCCESS
    );
    other.setResourceId("incident-99");

    auditLogRepository.save(log1);
    auditLogRepository.save(log2);
    auditLogRepository.save(other);

    List<AuditLog> result = auditLogRepository.findByResourceTypeAndResourceId(
      "INCIDENT",
      "incident-42"
    );

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(l -> "incident-42".equals(l.getResourceId()));
  }

  // ── findByStatus ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("findByStatus - Returns logs with given status")
  void findByStatus_ReturnsMatchingLogs() {
    auditLogRepository.save(
      buildLog(
        userId,
        AuditAction.INCIDENT_CREATE,
        "INCIDENT",
        AuditStatus.SUCCESS
      )
    );
    auditLogRepository.save(
      buildLog(userId, AuditAction.LOGIN_SUCCESS, "USER", AuditStatus.FAILURE)
    );
    auditLogRepository.save(
      buildLog(userId, AuditAction.USER_DELETE, "USER", AuditStatus.SUCCESS)
    );

    List<AuditLog> result = auditLogRepository.findByStatus(
      AuditStatus.SUCCESS
    );

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(l -> AuditStatus.SUCCESS.equals(l.getStatus()));
  }

  // ── findByTimestampBetween ────────────────────────────────────────────────

  @Test
  @DisplayName("findByTimestampBetween - Returns logs within time range")
  void findByTimestampBetween_ReturnsMatchingLogs() {
    auditLogRepository.save(
      buildLog(
        userId,
        AuditAction.INCIDENT_CREATE,
        "INCIDENT",
        AuditStatus.SUCCESS
      )
    );
    auditLogRepository.save(
      buildLog(userId, AuditAction.USER_DELETE, "USER", AuditStatus.SUCCESS)
    );

    // Tous les logs sauvegardés ont @CreatedDate = now ; on interroge donc une plage qui les inclut tous
    LocalDateTime from = LocalDateTime.now().minusMinutes(1);
    LocalDateTime to = LocalDateTime.now().plusMinutes(1);

    List<AuditLog> result = auditLogRepository.findByTimestampBetween(from, to);

    // Verifie que les deux journaux dans la periode font partie du resultat attendu.
    assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    assertThat(result).anyMatch(
      l -> l.getAction() == AuditAction.INCIDENT_CREATE
    );
    assertThat(result).anyMatch(l -> l.getAction() == AuditAction.USER_DELETE);
  }

  // ── findByUserIdAndTimestampBetween ───────────────────────────────────────

  @Test
  @DisplayName(
    "findByUserIdAndTimestampBetween - Returns logs for user within time range"
  )
  void findByUserIdAndTimestampBetween_ReturnsMatchingLogs() {
    auditLogRepository.save(
      buildLog(
        userId,
        AuditAction.INCIDENT_CREATE,
        "INCIDENT",
        AuditStatus.SUCCESS
      )
    );
    auditLogRepository.save(
      buildLog(
        otherUserId,
        AuditAction.INCIDENT_CREATE,
        "INCIDENT",
        AuditStatus.SUCCESS
      )
    );
    auditLogRepository.save(
      buildLog(userId, AuditAction.USER_DELETE, "USER", AuditStatus.SUCCESS)
    );

    LocalDateTime from = LocalDateTime.now().minusMinutes(1);
    LocalDateTime to = LocalDateTime.now().plusMinutes(1);

    List<AuditLog> result = auditLogRepository.findByUserIdAndTimestampBetween(
      userId,
      from,
      to
    );

    // Should return only logs for userId (2 logs), not otherUserId
    assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    assertThat(result).allMatch(l -> userId.equals(l.getUserId()));
  }

  // ── save ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("save - Persists audit log and generates ID")
  void save_ValidLog_PersistsAndGeneratesId() {
    AuditLog log = buildLog(
      userId,
      AuditAction.INCIDENT_CREATE,
      "INCIDENT",
      AuditStatus.SUCCESS
    );

    AuditLog saved = auditLogRepository.save(log);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getAction()).isEqualTo(AuditAction.INCIDENT_CREATE);
    assertThat(saved.getResourceType()).isEqualTo("INCIDENT");
    assertThat(saved.getStatus()).isEqualTo(AuditStatus.SUCCESS);
    assertThat(saved.getUserId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("save - Persists audit log with null userId (system action)")
  void save_NullUserId_PersistsSuccessfully() {
    AuditLog log = buildLog(
      null,
      AuditAction.SETTINGS_CHANGE,
      "AUDIT_LOG",
      AuditStatus.SUCCESS
    );

    AuditLog saved = auditLogRepository.save(log);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUserId()).isNull();
    assertThat(saved.getAction()).isEqualTo(AuditAction.SETTINGS_CHANGE);
  }
}
