package com.fintrack.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.notification.TestcontainersConfiguration;
import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.constant.NotificationType;
import com.fintrack.notification.model.entity.Notification;
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
class NotificationRepositoryTest {

  @Autowired
  private NotificationRepository notificationRepository;

  private UUID incidentId;
  private String recipient;
  private LocalDateTime retryTime;
  private LocalDateTime pastTime;

  @BeforeEach
  void setUp() {
    notificationRepository.deleteAll();
    incidentId = UUID.randomUUID();
    recipient = "test@example.com";
    retryTime = LocalDateTime.now().plusHours(1);
    pastTime = LocalDateTime.now().minusHours(1);
  }

  private Notification buildNotification(NotificationStatus status) {
    Notification notification = new Notification();
    notification.setType(NotificationType.EMAIL);
    notification.setRecipient(recipient);
    notification.setSubject("Test Subject");
    notification.setContent("Test Content");
    notification.setIncidentId(incidentId);
    notification.setStatus(status);
    notification.setRetryCount(0);
    return notification;
  }

  private Notification buildNotificationWithRetry(
    NotificationStatus status,
    LocalDateTime nextRetry
  ) {
    Notification notification = buildNotification(status);
    notification.setRetryCount(1);
    notification.setRetryAt(LocalDateTime.now().minusMinutes(30));
    notification.setNextRetry(nextRetry);
    return notification;
  }

  @Test
  @DisplayName("findByStatus - Returns notifications with given status")
  void findByStatus_ReturnsMatchingNotifications() {
    Notification n1 = buildNotification(NotificationStatus.PENDING);
    Notification n2 = buildNotification(NotificationStatus.PENDING);
    Notification sent = buildNotification(NotificationStatus.SENT);

    notificationRepository.save(n1);
    notificationRepository.save(n2);
    notificationRepository.save(sent);

    List<Notification> result = notificationRepository.findByStatus(
      NotificationStatus.PENDING
    );

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(
      n -> n.getStatus() == NotificationStatus.PENDING
    );
  }

  @Test
  @DisplayName("findByStatus - Returns empty when no notifications with status")
  void findByStatus_NoMatch_ReturnsEmpty() {
    assertThat(
      notificationRepository.findByStatus(NotificationStatus.FAILED)
    ).isEmpty();
  }

  @Test
  @DisplayName("findByIncidentId - Returns notifications for given incident")
  void findByIncidentId_ReturnsMatchingNotifications() {
    Notification n1 = buildNotification(NotificationStatus.PENDING);
    Notification n2 = buildNotification(NotificationStatus.SENT);
    Notification other = buildNotification(NotificationStatus.PENDING);
    other.setIncidentId(UUID.randomUUID());

    notificationRepository.save(n1);
    notificationRepository.save(n2);
    notificationRepository.save(other);

    List<Notification> result = notificationRepository.findByIncidentId(
      incidentId
    );

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(n -> n.getIncidentId().equals(incidentId));
  }

  @Test
  @DisplayName(
    "findByIncidentId - Returns empty when no notifications for incident"
  )
  void findByIncidentId_NoMatch_ReturnsEmpty() {
    assertThat(
      notificationRepository.findByIncidentId(UUID.randomUUID())
    ).isEmpty();
  }

  @Test
  @DisplayName("findByRecipient - Returns notifications for given recipient")
  void findByRecipient_ReturnsMatchingNotifications() {
    Notification n1 = buildNotification(NotificationStatus.PENDING);
    Notification n2 = buildNotification(NotificationStatus.SENT);
    Notification other = buildNotification(NotificationStatus.PENDING);
    other.setRecipient("other@example.com");

    notificationRepository.save(n1);
    notificationRepository.save(n2);
    notificationRepository.save(other);

    List<Notification> result = notificationRepository.findByRecipient(
      recipient
    );

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(n -> n.getRecipient().equals(recipient));
  }

  @Test
  @DisplayName(
    "findByRecipient - Returns empty when no notifications for recipient"
  )
  void findByRecipient_NoMatch_ReturnsEmpty() {
    assertThat(
      notificationRepository.findByRecipient("nonexistent@example.com")
    ).isEmpty();
  }

  @Test
  @DisplayName(
    "findByStatusAndNextRetryBefore - Returns notifications pending retry before time"
  )
  void findByStatusAndNextRetryBefore_ReturnsMatchingNotifications() {
    Notification ready1 = buildNotificationWithRetry(
      NotificationStatus.PENDING,
      pastTime
    );
    Notification ready2 = buildNotificationWithRetry(
      NotificationStatus.FAILED,
      pastTime
    );
    Notification future = buildNotificationWithRetry(
      NotificationStatus.PENDING,
      retryTime
    );
    Notification noRetry = buildNotification(NotificationStatus.PENDING);

    notificationRepository.save(ready1);
    notificationRepository.save(ready2);
    notificationRepository.save(future);
    notificationRepository.save(noRetry);

    List<Notification> result =
      notificationRepository.findByStatusAndNextRetryBefore(
        NotificationStatus.PENDING,
        LocalDateTime.now()
      );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getNextRetry()).isBefore(LocalDateTime.now());
  }

  @Test
  @DisplayName(
    "findByStatusAndNextRetryBefore - Returns empty when no notifications ready for retry"
  )
  void findByStatusAndNextRetryBefore_NoMatch_ReturnsEmpty() {
    assertThat(
      notificationRepository.findByStatusAndNextRetryBefore(
        NotificationStatus.PENDING,
        LocalDateTime.now()
      )
    ).isEmpty();
  }

  @Test
  @DisplayName("save - Persists notification and generates ID")
  void save_ValidNotification_PersistsAndGeneratesId() {
    Notification notification = buildNotification(NotificationStatus.PENDING);

    Notification saved = notificationRepository.save(notification);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getType()).isEqualTo(NotificationType.EMAIL);
    assertThat(saved.getRecipient()).isEqualTo(recipient);
    assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
    assertThat(saved.getRetryCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("save - Persists notification with audit fields")
  void save_ValidNotification_PersistsWithAuditFields() {
    Notification notification = buildNotification(NotificationStatus.PENDING);

    Notification saved = notificationRepository.save(notification);

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }
}
