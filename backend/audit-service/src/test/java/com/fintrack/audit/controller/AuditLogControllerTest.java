package com.fintrack.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintrack.audit.client.user.UserServiceClientService;
import com.fintrack.audit.config.TestSecurityConfig;
import com.fintrack.audit.model.constant.AuditAction;
import com.fintrack.audit.model.constant.AuditStatus;
import com.fintrack.audit.model.dto.request.AuditLogRequest;
import com.fintrack.audit.model.dto.response.AuditLogResponse;
import com.fintrack.audit.model.dto.response.UserSummaryResponse;
import com.fintrack.audit.model.entity.AuditLog;
import com.fintrack.audit.model.mapper.AuditLogMapper;
import com.fintrack.audit.service.AuditLogService;
import com.fintrack.audit.service.AuditPurgeService;
import com.fintrack.audit.service.AuditStatsService;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditLogController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@WithMockUser(username = "admin", authorities = { "AUDIT_VIEW" })
class AuditLogControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(
    new JavaTimeModule()
  );

  @MockitoBean
  private AuditLogService auditLogService;

  @MockitoBean
  private AuditLogMapper auditLogMapper;

  @MockitoBean
  private AuditStatsService auditStatsService;

  @MockitoBean
  private AuditPurgeService auditPurgeService;

  @MockitoBean
  private UserServiceClientService userServiceClientService;

  private AuditLog testLog;
  private AuditLogResponse testResponse;
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

    testResponse = AuditLogResponse.builder()
      .id(testId)
      .user(UserSummaryResponse.builder().id(userId).username("jdoe").build())
      .username("jdoe")
      .action(AuditAction.INCIDENT_CREATE)
      .resourceType("INCIDENT")
      .resourceId("incident-123")
      .status(AuditStatus.SUCCESS)
      .timestamp(LocalDateTime.now())
      .build();
  }

  @Test
  @DisplayName(
    "GET /api/v1/auditService/audit-logs - Returns page of audit logs"
  )
  void getAllAuditLogs_ReturnsPage() throws Exception {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<AuditLog> page = new PageImpl<>(List.of(testLog), pageable, 1);

    when(
      auditLogService.search(
        any(PageRequest.class),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    ).thenReturn(page);
    when(userServiceClientService.resolveUsers(any())).thenReturn(
      java.util.Map.of()
    );
    when(auditLogMapper.toResponse(any(AuditLog.class), any())).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get("/api/v1/auditService/audit-logs")
          .param("page", "0")
          .param("size", "10")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].resourceType").value("INCIDENT"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/auditService/audit-logs/{id} - Returns audit log by ID"
  )
  void getAuditLogById_ReturnsLog() throws Exception {
    when(auditLogService.findById(testId)).thenReturn(testLog);
    when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(get("/api/v1/auditService/audit-logs/{id}", testId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(testId));
  }

  @Test
  @DisplayName(
    "GET /api/v1/auditService/audit-logs/user/{userId} - Returns logs for user"
  )
  void getAuditLogsByUserId_ReturnsLogs() throws Exception {
    when(auditLogService.findByUserId(userId)).thenReturn(List.of(testLog));
    when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(get("/api/v1/auditService/audit-logs/user/{userId}", userId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].resourceType").value("INCIDENT"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/auditService/audit-logs/action/{action} - Returns logs by action"
  )
  void getAuditLogsByAction_ReturnsLogs() throws Exception {
    when(auditLogService.findByAction(AuditAction.INCIDENT_CREATE)).thenReturn(
      List.of(testLog)
    );
    when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get(
          "/api/v1/auditService/audit-logs/action/{action}",
          "INCIDENT_CREATE"
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].resourceType").value("INCIDENT"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/auditService/audit-logs/resource/{resourceType} - Returns logs by resource type"
  )
  void getAuditLogsByResourceType_ReturnsLogs() throws Exception {
    when(auditLogService.findByResourceType("INCIDENT")).thenReturn(
      List.of(testLog)
    );
    when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get(
          "/api/v1/auditService/audit-logs/resource/{resourceType}",
          "INCIDENT"
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].resourceType").value("INCIDENT"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/auditService/audit-logs/resource/{resourceType}/{resourceId} - Returns logs for specific resource"
  )
  void getAuditLogsByResource_ReturnsLogs() throws Exception {
    when(
      auditLogService.findByResourceTypeAndResourceId(
        "INCIDENT",
        "incident-123"
      )
    ).thenReturn(List.of(testLog));
    when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get(
          "/api/v1/auditService/audit-logs/resource/{resourceType}/{resourceId}",
          "INCIDENT",
          "incident-123"
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].resourceId").value("incident-123"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/auditService/audit-logs/status/{status} - Returns logs by status"
  )
  void getAuditLogsByStatus_ReturnsLogs() throws Exception {
    when(auditLogService.findByStatus(AuditStatus.SUCCESS)).thenReturn(
      List.of(testLog)
    );
    when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get("/api/v1/auditService/audit-logs/status/{status}", "SUCCESS")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].resourceType").value("INCIDENT"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/auditService/audit-logs/range - Returns logs within time range"
  )
  void getAuditLogsByTimeRange_ReturnsLogs() throws Exception {
    LocalDateTime from = LocalDateTime.now().minusHours(1);
    LocalDateTime to = LocalDateTime.now().plusHours(1);

    when(auditLogService.findByTimestampBetween(any(), any())).thenReturn(
      List.of(testLog)
    );
    when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get("/api/v1/auditService/audit-logs/range")
          .param("from", from.toString())
          .param("to", to.toString())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].resourceType").value("INCIDENT"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/auditService/audit-logs/user/{userId}/range - Returns logs for user within time range"
  )
  void getAuditLogsByUserAndTimeRange_ReturnsLogs() throws Exception {
    LocalDateTime from = LocalDateTime.now().minusHours(1);
    LocalDateTime to = LocalDateTime.now().plusHours(1);

    when(
      auditLogService.findByUserIdAndTimestampBetween(eq(userId), any(), any())
    ).thenReturn(List.of(testLog));
    when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get("/api/v1/auditService/audit-logs/user/{userId}/range", userId)
          .param("from", from.toString())
          .param("to", to.toString())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].resourceType").value("INCIDENT"));
  }

  @Test
  @DisplayName(
    "POST /api/v1/auditService/audit-logs - Creates audit log and returns 201"
  )
  void createAuditLog_ReturnsCreated() throws Exception {
    AuditLogRequest request = new AuditLogRequest();
    request.setUserId(userId);
    request.setUsername("jdoe");
    request.setAction(AuditAction.INCIDENT_CREATE);
    request.setResourceType("INCIDENT");
    request.setResourceId("incident-123");
    request.setStatus(AuditStatus.SUCCESS);

    when(auditLogMapper.toDocument(any())).thenReturn(testLog);
    when(auditLogService.create(any(AuditLog.class))).thenReturn(testLog);
    when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        post("/api/v1/auditService/audit-logs")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.resourceType").value("INCIDENT"));
  }

  @Test
  @DisplayName(
    "POST /api/v1/auditService/audit-logs - Validation fails on null action"
  )
  void createAuditLog_NullAction_ReturnsBadRequest() throws Exception {
    AuditLogRequest request = new AuditLogRequest();
    // Verifie que @NotNull declenche une erreur lorsque action vaut null.
    request.setResourceType("INCIDENT");
    request.setStatus(AuditStatus.SUCCESS);

    mockMvc
      .perform(
        post("/api/v1/auditService/audit-logs")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName(
    "DELETE /api/v1/auditService/audit-logs/{id} - Not supported (audit log is append-only)"
  )
  void deleteAuditLog_IsNotSupported() throws Exception {
    mockMvc
      .perform(
        delete("/api/v1/auditService/audit-logs/{id}", testId).with(csrf())
      )
      .andExpect(status().isMethodNotAllowed());
  }
}
