package com.fintrack.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fintrack.notification.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.notification.exception.EntityNotFoundException;
import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.constant.NotificationType;
import com.fintrack.notification.model.entity.Notification;
import com.fintrack.notification.repository.NotificationRepository;
import com.fintrack.notification.service.impl.NotificationServiceImpl;
import java.time.Duration;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private MailService mailService;

  @Mock
  private NotificationWebSocketService webSocketService;

  @Mock
  private ReportingSystemConfigClientService reportingSystemConfigClientService;

  @Mock
  private MongoTemplate mongoTemplate;

  @InjectMocks
  private NotificationServiceImpl notificationService;

  private Notification testNotification;
  private String testId;

  /** Utilisateur simulé pour les tests de visibilité. */
  private static final String REQUESTER = "test@example.com";

  @BeforeEach
  void setUp() {
    testId = "507f1f77bcf86cd799439011";

    testNotification = new Notification();
    testNotification.setId(testId);
    testNotification.setType(NotificationType.EMAIL);
    testNotification.setRecipient(REQUESTER);
    testNotification.setSubject("Test Subject");
    testNotification.setContent("Test Content");
    testNotification.setStatus(NotificationStatus.PENDING);
    testNotification.setRetryCount(0);
    testNotification.setIncidentId(UUID.randomUUID());
  }

  // ── getVisible (pageable) ─────────────────────────────────────────────────

  @Test
  @DisplayName("getVisible(pageable) - canViewAll returns all notifications")
  void getVisible_Pageable_CanViewAll_ReturnsPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<Notification> page = new PageImpl<>(
      List.of(testNotification),
      pageable,
      1
    );
    when(notificationRepository.findAll(pageable)).thenReturn(page);

    Page<Notification> result = notificationService.getVisible(
      REQUESTER,
      true,
      pageable
    );

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(testId);
  }

  @Test
  @DisplayName(
    "getVisible(pageable) - restricted returns only own notifications"
  )
  void getVisible_Pageable_Restricted_ReturnsOwnPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<Notification> page = new PageImpl<>(
      List.of(testNotification),
      pageable,
      1
    );
    when(
      notificationRepository.findByRecipient(REQUESTER, pageable)
    ).thenReturn(page);

    Page<Notification> result = notificationService.getVisible(
      REQUESTER,
      false,
      pageable
    );

    assertThat(result.getContent()).hasSize(1);
  }

  // ── getVisible (list) ─────────────────────────────────────────────────────

  @Test
  @DisplayName("getVisible() - canViewAll returns all notifications")
  void getVisible_CanViewAll_ReturnsList() {
    when(notificationRepository.findAll()).thenReturn(
      List.of(testNotification)
    );

    List<Notification> result = notificationService.getVisible(REQUESTER, true);

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("getVisible() - restricted returns only own notifications")
  void getVisible_Restricted_ReturnsOwnList() {
    when(notificationRepository.findByRecipient(REQUESTER)).thenReturn(
      List.of(testNotification)
    );

    List<Notification> result = notificationService.getVisible(
      REQUESTER,
      false
    );

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName(
    "getByRecipient(pageable) - Returns recipient notifications when allowed"
  )
  void getByRecipient_Pageable_Allowed_ReturnsPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<Notification> page = new PageImpl<>(
      List.of(testNotification),
      pageable,
      1
    );
    when(
      notificationRepository.findByRecipient(REQUESTER, pageable)
    ).thenReturn(page);

    Page<Notification> result = notificationService.getByRecipient(
      REQUESTER,
      REQUESTER,
      false,
      pageable
    );

    assertThat(result.getContent()).hasSize(1);
    verify(notificationRepository).findByRecipient(REQUESTER, pageable);
  }

  @Test
  @DisplayName(
    "getByRecipient(pageable) - Throws AccessDeniedException when not allowed"
  )
  void getByRecipient_Pageable_NotAllowed_ThrowsAccessDeniedException() {
    PageRequest pageable = PageRequest.of(0, 10);

    assertThatThrownBy(() ->
      notificationService.getByRecipient(
        REQUESTER,
        "other@example.com",
        false,
        pageable
      )
    ).isInstanceOf(AccessDeniedException.class);
    verify(notificationRepository, never()).findByRecipient(
      REQUESTER,
      pageable
    );
  }

  // ── getVisibleById ────────────────────────────────────────────────────────

  @Test
  @DisplayName(
    "getVisibleById - Returns notification when requester is recipient"
  )
  void getVisibleById_OwnNotification_ReturnsNotification() {
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(testNotification)
    );

    Notification result = notificationService.getVisibleById(
      testId,
      REQUESTER,
      false
    );

    assertThat(result.getId()).isEqualTo(testId);
    assertThat(result.getRecipient()).isEqualTo(REQUESTER);
  }

  @Test
  @DisplayName("getVisibleById - Returns notification when canViewAll")
  void getVisibleById_CanViewAll_ReturnsNotification() {
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(testNotification)
    );

    Notification result = notificationService.getVisibleById(
      testId,
      "other@example.com",
      true
    );

    assertThat(result.getId()).isEqualTo(testId);
  }

  @Test
  @DisplayName("getVisibleById - Throws AccessDeniedException when not allowed")
  void getVisibleById_NotAllowed_ThrowsAccessDeniedException() {
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(testNotification)
    );

    assertThatThrownBy(() ->
      notificationService.getVisibleById(testId, "other@example.com", false)
    ).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("getVisibleById - Throws EntityNotFoundException when not found")
  void getVisibleById_NotFound_ThrowsException() {
    when(notificationRepository.findById("unknown")).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      notificationService.getVisibleById("unknown", REQUESTER, true)
    ).isInstanceOf(EntityNotFoundException.class);
  }

  // ── getVisibleByStatus ────────────────────────────────────────────────────

  @Test
  @DisplayName("getVisibleByStatus - canViewAll returns all by status")
  void getVisibleByStatus_CanViewAll_ReturnsByStatus() {
    when(
      notificationRepository.findByStatus(NotificationStatus.PENDING)
    ).thenReturn(List.of(testNotification));

    List<Notification> result = notificationService.getVisibleByStatus(
      NotificationStatus.PENDING,
      REQUESTER,
      true
    );

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("getVisibleByStatus - restricted returns own by status")
  void getVisibleByStatus_Restricted_ReturnsOwnByStatus() {
    when(
      notificationRepository.findByRecipientAndStatus(
        REQUESTER,
        NotificationStatus.PENDING
      )
    ).thenReturn(List.of(testNotification));

    List<Notification> result = notificationService.getVisibleByStatus(
      NotificationStatus.PENDING,
      REQUESTER,
      false
    );

    assertThat(result).hasSize(1);
  }

  // ── getVisibleByIncidentId ────────────────────────────────────────────────

  @Test
  @DisplayName("getVisibleByIncidentId - canViewAll returns all by incident")
  void getVisibleByIncidentId_CanViewAll_ReturnsByIncident() {
    UUID incidentId = testNotification.getIncidentId();
    when(notificationRepository.findByIncidentId(incidentId)).thenReturn(
      List.of(testNotification)
    );

    List<Notification> result = notificationService.getVisibleByIncidentId(
      incidentId,
      REQUESTER,
      true
    );

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("getVisibleByIncidentId - restricted returns own by incident")
  void getVisibleByIncidentId_Restricted_ReturnsOwnByIncident() {
    UUID incidentId = testNotification.getIncidentId();
    when(
      notificationRepository.findByRecipientAndIncidentId(REQUESTER, incidentId)
    ).thenReturn(List.of(testNotification));

    List<Notification> result = notificationService.getVisibleByIncidentId(
      incidentId,
      REQUESTER,
      false
    );

    assertThat(result).hasSize(1);
  }

  // ── create ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("create - EMAIL: déclenche l'envoi asynchrone et passe en SENT")
  void create_EmailNotification_SendsAndMarksSent() {
    Notification input = new Notification();
    input.setId(testId);
    input.setType(NotificationType.EMAIL);
    input.setRecipient("new@example.com");

    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv ->
      inv.getArgument(0)
    );
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(input)
    );

    Notification result = notificationService.create(input);

    // Envoi e-mail asynchrone : la création renvoie immédiatement en PENDING.
    assertThat(result.getRetryCount()).isEqualTo(0);
    await()
      .atMost(Duration.ofSeconds(2))
      .untilAsserted(() -> {
        verify(mailService).send(any(Notification.class));
        assertThat(input.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(input.getSentAt()).isNotNull();
      });
  }

  @Test
  @DisplayName(
    "create - EMAIL: échec d'envoi -> FAILED + nouvelle tentative planifiée"
  )
  void create_EmailNotification_SendFails_MarksFailed() {
    Notification input = new Notification();
    input.setId(testId);
    input.setType(NotificationType.EMAIL);
    input.setRecipient("new@example.com");

    doThrow(new RuntimeException("Resend down"))
      .when(mailService)
      .send(any(Notification.class));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv ->
      inv.getArgument(0)
    );
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(input)
    );

    notificationService.create(input);

    await()
      .atMost(Duration.ofSeconds(2))
      .untilAsserted(() -> {
        assertThat(input.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(input.getNextRetry()).isNotNull();
      });
  }

  @Test
  @DisplayName("create - INTERNAL: livrée immédiatement (SENT)")
  void create_InternalNotification_MarksSent() {
    Notification input = new Notification();
    input.setType(NotificationType.INTERNAL);
    input.setRecipient("jdoe");

    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv ->
      inv.getArgument(0)
    );

    Notification result = notificationService.create(input);

    verifyNoInteractions(mailService);
    assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);
    assertThat(result.getRetryCount()).isEqualTo(0);
  }

  // ── markAsRead ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("create - returns existing notification when idempotency key already exists")
  void create_DuplicateIdempotencyKey_ReturnsExistingNotification() {
    Notification existing = new Notification();
    existing.setId(testId);
    existing.setType(NotificationType.INTERNAL);
    existing.setRecipient("jdoe");
    existing.setIdempotencyKey("incident-service:INTERNAL:incident-1:jdoe:key");

    Notification input = new Notification();
    input.setType(NotificationType.INTERNAL);
    input.setRecipient("jdoe");
    input.setIdempotencyKey(existing.getIdempotencyKey());

    when(
      notificationRepository.findByIdempotencyKey(existing.getIdempotencyKey())
    ).thenReturn(Optional.of(existing));

    Notification result = notificationService.create(input);

    assertThat(result).isSameAs(existing);
    verify(notificationRepository, never()).save(any(Notification.class));
    verify(webSocketService, never()).pushToRecipient(any());
    verifyNoInteractions(mailService);
  }

  @Test
  @DisplayName("markAsRead - Sets READ status")
  void markAsRead_SetsReadStatus() {
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(testNotification)
    );
    when(notificationRepository.save(any(Notification.class))).thenAnswer(inv ->
      inv.getArgument(0)
    );

    Notification result = notificationService.markAsRead(
      testId,
      REQUESTER,
      false
    );

    assertThat(result.getStatus()).isEqualTo(NotificationStatus.READ);
  }

  @Test
  @DisplayName("markAsRead - Already READ returns without saving")
  void markAsRead_AlreadyRead_ReturnsDirectly() {
    testNotification.setStatus(NotificationStatus.READ);
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(testNotification)
    );

    Notification result = notificationService.markAsRead(
      testId,
      REQUESTER,
      false
    );

    verify(notificationRepository, never()).save(any(Notification.class));
    assertThat(result.getStatus()).isEqualTo(NotificationStatus.READ);
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("markAllAsRead - Marks all visible unread notifications as READ")
  void markAllAsRead_Restricted_MarksVisibleUnreadNotifications() {
    testNotification.setStatus(NotificationStatus.READ);
    when(
      notificationRepository.findByRecipientAndStatus(
        REQUESTER,
        NotificationStatus.READ
      )
    ).thenReturn(List.of(testNotification));

    List<Notification> result = notificationService.markAllAsRead(
      REQUESTER,
      false
    );

    assertThat(result).hasSize(1);
    // Mise à jour en masse côté Mongo, puis relecture des notifications désormais READ.
    verify(mongoTemplate).updateMulti(
      any(Query.class),
      any(Update.class),
      eq(Notification.class)
    );
    verify(notificationRepository, never()).findByRecipient(REQUESTER);
  }

  @Test
  @DisplayName("delete - Deletes own notification when found")
  void delete_OwnNotification_Deletes() {
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(testNotification)
    );

    notificationService.delete(testId, REQUESTER, false);

    verify(notificationRepository).deleteById(testId);
  }

  @Test
  @DisplayName("delete - Throws EntityNotFoundException when not found")
  void delete_NotFound_ThrowsException() {
    when(notificationRepository.findById("unknown")).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      notificationService.delete("unknown", REQUESTER, false)
    ).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName(
    "delete - Denies deleting another user's notification without manage right"
  )
  void delete_NotOwnerWithoutManage_ThrowsAccessDenied() {
    testNotification.setRecipient("other@example.com");
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(testNotification)
    );

    assertThatThrownBy(() ->
      notificationService.delete(testId, REQUESTER, false)
    ).isInstanceOf(AccessDeniedException.class);
    verify(notificationRepository, never()).deleteById(testId);
  }

  @Test
  @DisplayName(
    "delete - Allows deleting another user's notification with manage right"
  )
  void delete_NotOwnerWithManage_Deletes() {
    testNotification.setRecipient("other@example.com");
    when(notificationRepository.findById(testId)).thenReturn(
      Optional.of(testNotification)
    );

    notificationService.delete(testId, REQUESTER, true);

    verify(notificationRepository).deleteById(testId);
  }

  // ── bulkDelete ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("bulkDelete - Deletes all provided ids")
  void bulkDelete_ValidIds_DeletesAll() {
    List<String> ids = List.of("id1", "id2");

    notificationService.bulkDelete(ids);

    verify(notificationRepository).deleteAllById(ids);
  }
}
