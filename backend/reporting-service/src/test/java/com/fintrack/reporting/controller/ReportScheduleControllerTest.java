package com.fintrack.reporting.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintrack.reporting.config.TestSecurityConfig;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.dto.request.ReportScheduleRequest;
import com.fintrack.reporting.model.dto.response.ReportScheduleResponse;
import com.fintrack.reporting.model.dto.response.UserSummaryResponse;
import com.fintrack.reporting.model.entity.ReportSchedule;
import com.fintrack.reporting.model.mapper.ReportScheduleMapper;
import com.fintrack.reporting.security.UserDetailsImpl;
import com.fintrack.reporting.service.ReportScheduleService;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportScheduleController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ReportScheduleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(
    new JavaTimeModule()
  );

  @MockitoBean
  private ReportScheduleService reportScheduleService;

  @MockitoBean
  private ReportScheduleMapper reportScheduleMapper;

  private ReportSchedule testSchedule;
  private ReportScheduleResponse testResponse;
  private UUID testId;
  private UUID createdBy;

  private UserDetailsImpl admin() {
    Set<SimpleGrantedAuthority> auths = new HashSet<>();
    auths.add(new SimpleGrantedAuthority("REPORT_VIEW_ALL"));
    auths.add(new SimpleGrantedAuthority("REPORT_GENERATE"));
    auths.add(new SimpleGrantedAuthority("REPORT_DELETE"));
    return new UserDetailsImpl(UUID.randomUUID(), "admin", null, true, auths);
  }

  private UserDetailsImpl creator(String... extraAuthorities) {
    Set<SimpleGrantedAuthority> auths = new HashSet<>();
    for (String a : extraAuthorities) auths.add(new SimpleGrantedAuthority(a));
    return new UserDetailsImpl(createdBy, "testuser", null, true, auths);
  }

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    createdBy = UUID.randomUUID();

    testSchedule = new ReportSchedule();
    testSchedule.setId(testId);
    testSchedule.setName("Daily Report");
    testSchedule.setType(ReportType.DAILY);
    testSchedule.setFormat(ReportFormat.PDF);
    testSchedule.setCreatedBy(createdBy);
    testSchedule.setActive(true);

    testResponse = new ReportScheduleResponse();
    testResponse.setName("Daily Report");
    testResponse.setType(ReportType.DAILY);
    testResponse.setFormat(ReportFormat.PDF);
    testResponse.setRecipientEmails(
      List.of("recipient1@example.com", "recipient2@example.com")
    );
    testResponse.setActive(true);
    testResponse.setCreatedBy(
      UserSummaryResponse.builder().id(createdBy).username("jdoe").build()
    );
    testResponse.setId(testId);
  }

  @Test
  @DisplayName(
    "GET /api/v1/reportingService/report-schedules - Returns page of schedules"
  )
  void getAllSchedules_ReturnsPage() throws Exception {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<ReportSchedule> page = new PageImpl<>(
      List.of(testSchedule),
      pageable,
      1
    );

    when(reportScheduleService.findAll(any(PageRequest.class))).thenReturn(
      page
    );
    when(reportScheduleMapper.toResponse(any(ReportSchedule.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get("/api/v1/reportingService/report-schedules")
          .param("page", "0")
          .param("size", "10")
          .with(user(admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].name").value("Daily Report"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/reportingService/report-schedules/all - Returns list of schedules"
  )
  void getAllSchedulesList_ReturnsList() throws Exception {
    when(reportScheduleService.findAll()).thenReturn(List.of(testSchedule));
    when(reportScheduleMapper.toResponse(any(ReportSchedule.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get("/api/v1/reportingService/report-schedules/all").with(user(admin()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].name").value("Daily Report"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/reportingService/report-schedules/active - Returns active schedules"
  )
  void getActiveSchedules_ReturnsActiveList() throws Exception {
    when(reportScheduleService.findActive()).thenReturn(List.of(testSchedule));
    when(reportScheduleMapper.toResponse(any(ReportSchedule.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get("/api/v1/reportingService/report-schedules/active").with(
          user(admin())
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].isActive").value(true));
  }

  @Test
  @DisplayName(
    "GET /api/v1/reportingService/report-schedules/{id} - Returns schedule by ID"
  )
  void getScheduleById_ReturnsSchedule() throws Exception {
    when(reportScheduleService.findById(testId)).thenReturn(testSchedule);
    when(reportScheduleMapper.toResponse(any(ReportSchedule.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get("/api/v1/reportingService/report-schedules/{id}", testId).with(
          user(admin())
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("Daily Report"));
  }

  @Test
  @DisplayName(
    "GET /api/v1/reportingService/report-schedules/created-by/{userId} - Returns schedules by creator"
  )
  void getSchedulesByCreatedBy_ReturnsSchedules() throws Exception {
    when(reportScheduleService.findByCreatedBy(createdBy)).thenReturn(
      List.of(testSchedule)
    );
    when(reportScheduleMapper.toResponse(any(ReportSchedule.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get(
          "/api/v1/reportingService/report-schedules/created-by/{userId}",
          createdBy
        ).with(user(creator("REPORT_VIEW_OWN")))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].name").value("Daily Report"));
  }

  @Test
  @DisplayName(
    "POST /api/v1/reportingService/report-schedules - Creates schedule and returns 201"
  )
  void createSchedule_ReturnsCreated() throws Exception {
    ReportScheduleRequest request = new ReportScheduleRequest();
    request.setName("New Report");
    request.setType(ReportType.WEEKLY);
    request.setFormat(ReportFormat.EXCEL);
    request.setSendTime(LocalTime.of(8, 0));
    request.setScope("all");
    request.setRecipientEmails(
      List.of("recipient1@example.com", "recipient2@example.com")
    );

    when(reportScheduleMapper.toEntity(any())).thenReturn(testSchedule);
    when(
      reportScheduleService.create(any(ReportSchedule.class), eq(createdBy))
    ).thenReturn(testSchedule);
    when(reportScheduleMapper.toResponse(any(ReportSchedule.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        post("/api/v1/reportingService/report-schedules")
          .with(csrf())
          .with(user(creator("REPORT_GENERATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("Daily Report"))
      .andExpect(
        jsonPath("$.recipientEmails[0]").value("recipient1@example.com")
      )
      .andExpect(
        jsonPath("$.recipientEmails[1]").value("recipient2@example.com")
      );
  }

  @Test
  @DisplayName(
    "PUT /api/v1/reportingService/report-schedules/{id} - Updates schedule"
  )
  void updateSchedule_ReturnsUpdated() throws Exception {
    ReportScheduleRequest request = new ReportScheduleRequest();
    request.setName("Updated Report");
    request.setType(ReportType.MONTHLY);
    request.setFormat(ReportFormat.JSON);
    request.setSendTime(LocalTime.of(8, 0));
    request.setScope("all");

    when(reportScheduleService.findById(testId)).thenReturn(testSchedule);
    when(reportScheduleMapper.toEntity(any())).thenReturn(testSchedule);
    when(
      reportScheduleService.update(eq(testId), any(ReportSchedule.class))
    ).thenReturn(testSchedule);
    when(reportScheduleMapper.toResponse(any(ReportSchedule.class))).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        put("/api/v1/reportingService/report-schedules/{id}", testId)
          .with(csrf())
          .with(user(creator("REPORT_GENERATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("Daily Report"));
  }

  @Test
  @DisplayName(
    "PATCH /api/v1/reportingService/report-schedules/{id}/toggle - Toggles active status"
  )
  void toggleActive_ReturnsToggled() throws Exception {
    ReportScheduleResponse toggledResponse = new ReportScheduleResponse();
    toggledResponse.setName("Daily Report");
    toggledResponse.setActive(false);
    toggledResponse.setId(testId);

    when(reportScheduleService.findById(testId)).thenReturn(testSchedule);
    when(reportScheduleService.toggleActive(testId)).thenReturn(testSchedule);
    when(reportScheduleMapper.toResponse(any(ReportSchedule.class))).thenReturn(
      toggledResponse
    );

    mockMvc
      .perform(
        patch("/api/v1/reportingService/report-schedules/{id}/toggle", testId)
          .with(csrf())
          .with(user(creator("REPORT_GENERATE")))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isActive").value(false));
  }

  @Test
  @DisplayName(
    "DELETE /api/v1/reportingService/report-schedules/{id} - Deletes schedule and returns 204"
  )
  void deleteSchedule_ReturnsNoContent() throws Exception {
    when(reportScheduleService.findById(testId)).thenReturn(testSchedule);
    doNothing().when(reportScheduleService).delete(testId);

    mockMvc
      .perform(
        delete("/api/v1/reportingService/report-schedules/{id}", testId)
          .with(csrf())
          .with(user(creator("REPORT_DELETE")))
      )
      .andExpect(status().isNoContent());
  }
}
