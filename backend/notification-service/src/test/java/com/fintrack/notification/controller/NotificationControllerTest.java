package com.fintrack.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintrack.notification.config.TestSecurityConfig;
import com.fintrack.notification.model.constant.NotificationStatus;
import com.fintrack.notification.model.constant.NotificationType;
import com.fintrack.notification.model.dto.request.NotificationRequest;
import com.fintrack.notification.model.dto.response.IncidentSummaryResponse;
import com.fintrack.notification.model.dto.response.NotificationResponse;
import com.fintrack.notification.model.entity.Notification;
import com.fintrack.notification.model.mapper.NotificationMapper;
import com.fintrack.notification.service.NotificationPurgeService;
import com.fintrack.notification.service.NotificationService;
import com.fintrack.notification.service.NotificationStatsService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@WithMockUser(
  username = "test@example.com",
  authorities = { "NOTIFICATION_VIEW_ALL", "NOTIFICATION_MANAGE" }
)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(
    new JavaTimeModule()
  );

  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private NotificationMapper notificationMapper;

  @MockitoBean
  private NotificationStatsService notificationStatsService;

  @MockitoBean
  private NotificationPurgeService notificationPurgeService;

  private Notification testNotification;
  private NotificationResponse testResponse;
  private String testId;
  private UUID incidentId;

  @BeforeEach
  void setUp() {
    testId = "507f1f77bcf86cd799439011";
    incidentId = UUID.randomUUID();

    testNotification = new Notification();
    testNotification.setId(testId);
    testNotification.setType(NotificationType.EMAIL);
    testNotification.setRecipient("test@example.com");
    testNotification.setSubject("Test Subject");
    testNotification.setContent("Test Content");
    testNotification.setStatus(NotificationStatus.PENDING);
    testNotification.setIncidentId(incidentId);

    testResponse = NotificationResponse.builder()
      .id(testId)
      .type(NotificationType.EMAIL)
      .recipient("test@example.com")
      .incidentId(
        IncidentSummaryResponse.builder()
          .id(incidentId)
          .title("Test Incident")
          .status("OPEN")
          .build()
      )
      .subject("Test Subject")
      .content("Test Content")
      .status(NotificationStatus.PENDING)
      .build();
  }

  @Test
  @DisplayName(
    "GET /api/v1/notificationService/notifications - Returns page of notifications"
  )
  void getAllNotifications_ReturnsPage() throws Exception {
    Page<Notification> page = new PageImpl<>(List.of(testNotification));

    when(
      notificationService.getVisible(
        anyString(),
        anyBoolean(),
        any(Pageable.class)
      )
    ).thenReturn(page);
    when(notificationMapper.toResponse(any(Notification.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get("/api/v1/notificationService/notifications")
          .param("page", "0")
          .param("size", "10")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].recipient").value("test@example.com"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/notificationService/notifications/all - Returns list of notifications"
  )
  void getAllNotificationsList_ReturnsList() throws Exception {
    when(notificationService.getVisible(anyString(), anyBoolean())).thenReturn(
      List.of(testNotification)
    );
    when(notificationMapper.toResponses(any())).thenReturn(
      List.of(testResponse)
    );

    mockMvc
      .perform(get("/api/v1/notificationService/notifications/all"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].recipient").value("test@example.com"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/notificationService/notifications/recipient/{recipient}/page - Returns paginated recipient notifications"
  )
  void getNotificationsByRecipientPage_ReturnsPage() throws Exception {
    Page<Notification> page = new PageImpl<>(List.of(testNotification));

    when(
      notificationService.getByRecipient(
        eq("test@example.com"),
        anyString(),
        anyBoolean(),
        any(Pageable.class)
      )
    ).thenReturn(page);
    when(notificationMapper.toResponse(any(Notification.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get(
          "/api/v1/notificationService/notifications/recipient/test@example.com/page"
        )
          .param("page", "0")
          .param("size", "10")
          .param("sort", "createdAt,desc")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].recipient").value("test@example.com"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/notificationService/notifications/{id} - Returns notification by ID"
  )
  void getNotificationById_ReturnsNotification() throws Exception {
    when(
      notificationService.getVisibleById(eq(testId), anyString(), anyBoolean())
    ).thenReturn(testNotification);
    when(notificationMapper.toResponse(any(Notification.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(get("/api/v1/notificationService/notifications/{id}", testId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(testId));
  }

  @Test
  @DisplayName(
    "GET /api/v1/notificationService/notifications/status/{status} - Returns notifications by status"
  )
  void getNotificationsByStatus_ReturnsNotifications() throws Exception {
    when(
      notificationService.getVisibleByStatus(
        eq(NotificationStatus.PENDING),
        anyString(),
        anyBoolean()
      )
    ).thenReturn(List.of(testNotification));
    when(notificationMapper.toResponses(any())).thenReturn(
      List.of(testResponse)
    );

    mockMvc
      .perform(
        get(
          "/api/v1/notificationService/notifications/status/{status}",
          "PENDING"
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/notificationService/notifications/incident/{incidentId} - Returns notifications by incident"
  )
  void getNotificationsByIncidentId_ReturnsNotifications() throws Exception {
    when(
      notificationService.getVisibleByIncidentId(
        eq(incidentId),
        anyString(),
        anyBoolean()
      )
    ).thenReturn(List.of(testNotification));
    when(notificationMapper.toResponses(any())).thenReturn(
      List.of(testResponse)
    );

    mockMvc
      .perform(
        get(
          "/api/v1/notificationService/notifications/incident/{incidentId}",
          incidentId
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].recipient").value("test@example.com"));
  }

  @Test
  @DisplayName(
    "POST /api/v1/notificationService/notifications - Creates notification and returns 201"
  )
  void createNotification_ReturnsCreated() throws Exception {
    NotificationRequest request = new NotificationRequest();
    request.setType(NotificationType.EMAIL);
    request.setRecipient("new@example.com");
    request.setSubject("New Subject");
    request.setContent("New Content");

    when(notificationMapper.toDocument(any())).thenReturn(testNotification);
    when(notificationService.create(any(Notification.class))).thenReturn(
      testNotification
    );
    when(notificationMapper.toResponse(any(Notification.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        post("/api/v1/notificationService/notifications")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.recipient").value("test@example.com"));
  }

  @Test
  @DisplayName(
    "PATCH /api/v1/notificationService/notifications/{id}/read - Marks notification as read"
  )
  void markAsRead_ReturnsRead() throws Exception {
    NotificationResponse readResponse = NotificationResponse.builder()
      .id(testId)
      .status(NotificationStatus.READ)
      .recipient("test@example.com")
      .build();

    when(
      notificationService.markAsRead(eq(testId), anyString(), anyBoolean())
    ).thenReturn(testNotification);
    when(notificationMapper.toResponse(any(Notification.class))).thenReturn(
      readResponse
    );

    mockMvc
      .perform(
        patch(
          "/api/v1/notificationService/notifications/{id}/read",
          testId
        ).with(csrf())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("READ"));
  }

  @Test
  @DisplayName(
    "DELETE /api/v1/notificationService/notifications/{id} - Deletes notification and returns 204"
  )
  void deleteNotification_ReturnsNoContent() throws Exception {
    doNothing()
      .when(notificationService)
      .delete(eq(testId), anyString(), anyBoolean());

    mockMvc
      .perform(
        delete("/api/v1/notificationService/notifications/{id}", testId).with(
          csrf()
        )
      )
      .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName(
    "DELETE /api/v1/notificationService/notifications/bulk - Bulk deletes notifications and returns 204"
  )
  void bulkDeleteNotifications_ReturnsNoContent() throws Exception {
    List<String> ids = List.of("id1", "id2");
    doNothing().when(notificationService).bulkDelete(ids);

    mockMvc
      .perform(
        delete("/api/v1/notificationService/notifications/bulk")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(ids))
      )
      .andExpect(status().isNoContent());
  }
}
