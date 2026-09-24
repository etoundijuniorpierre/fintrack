package com.fintrack.notification.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.notification.model.dto.response.NotificationResponse;
import com.fintrack.notification.model.entity.Notification;
import com.fintrack.notification.model.mapper.NotificationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationWebSocketServiceTest {

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private NotificationMapper notificationMapper;

  @InjectMocks
  private NotificationWebSocketService webSocketService;

  @Test
  void pushToRecipient_ShouldSendMessageToCorrectTopic() {
    // Arrange
    Notification notification = new Notification();
    notification.setRecipient("test@finstar.local");
    notification.setId("test-id");

    NotificationResponse response = new NotificationResponse();
    response.setId("test-id");
    response.setRecipient("test@finstar.local");

    when(notificationMapper.toResponse(notification)).thenReturn(response);

    // Act
    webSocketService.pushToRecipient(notification);

    // Assert
    verify(messagingTemplate).convertAndSend(
      "/topic/notifications/test@finstar.local",
      response
    );
  }
}
