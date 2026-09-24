package com.fintrack.incident.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fintrack.incident.config.SecurityConfig;
import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.model.constant.PeriodType;
import com.fintrack.incident.model.dto.response.ComparisonResponse;
import com.fintrack.incident.model.dto.response.DashboardMetricsResponse;
import com.fintrack.incident.model.dto.response.IncidentSummaryResponse;
import com.fintrack.incident.model.mapper.IncidentMapper;
import com.fintrack.incident.model.readmodel.DashboardMetrics;
import com.fintrack.incident.model.readmodel.PeriodActivityIncidents;
import com.fintrack.incident.security.JwtUtils;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.DashboardService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@EnableMethodSecurity
class DashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DashboardService dashboardService;

  @MockitoBean
  private IncidentMapper incidentMapper;

  @MockitoBean
  private JwtUtils jwtUtils;

  private UUID userId;

  private UserDetailsImpl mockUser(String authority) {
    return new UserDetailsImpl(
      userId,
      "testuser",
      null,
      true,
      Set.of(new SimpleGrantedAuthority(authority))
    );
  }

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Test
  @DisplayName("GET /dashboard/metrics - Returns dashboard metrics")
  void getMetrics_WithPermission_ReturnsMetrics() throws Exception {
    DashboardMetrics metrics = DashboardMetrics.builder()
      .activeIncidents(2)
      .closedIncidents(4)
      .rejectedIncidents(1)
      .blockedIncidents(0)
      .totalIncidents(7)
      .distributionByType(Map.of())
      .distributionByCriticality(Map.of())
      .recentActivities(List.of())
      .build();
    DashboardMetricsResponse response = DashboardMetricsResponse.builder()
      .activeIncidents(2)
      .closedIncidents(4)
      .rejectedIncidents(1)
      .blockedIncidents(0)
      .totalIncidents(7)
      .distributionByType(Map.of())
      .distributionByCriticality(Map.of())
      .recentActivities(List.of())
      .build();

    when(
      dashboardService.getMetrics(
        eq("all"),
        any(UserDetailsImpl.class),
        any(),
        any(),
        any(),
        anyInt(),
        any(),
        any(),
        any()
      )
    ).thenReturn(metrics);
    when(incidentMapper.toDashboardResponse(metrics)).thenReturn(response);

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.DASHBOARD + "/metrics")
          .param("view", "all")
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.activeIncidents").value(2))
      .andExpect(jsonPath("$.totalIncidents").value(7));

    verify(dashboardService).getMetrics(
      eq("all"),
      any(UserDetailsImpl.class),
      any(),
      any(),
      any(),
      anyInt(),
      eq(PeriodType.LAST_30_DAYS),
      eq(null),
      eq(null)
    );
  }

  @Test
  @DisplayName("GET /dashboard/synthesis - Returns synthesis metrics")
  void getSynthesisMetrics_WithPermission_ReturnsMetrics() throws Exception {
    DashboardMetrics metrics = DashboardMetrics.builder()
      .inflow(10)
      .outflow(8)
      .netBacklog(2)
      .ageDistribution(Map.of("0-3", 2L))
      .build();
    DashboardMetricsResponse response = DashboardMetricsResponse.builder()
      .inflow(10)
      .outflow(8)
      .netBacklog(2)
      .ageDistribution(Map.of("0-3", 2L))
      .build();

    when(
      dashboardService.getMetrics(
        eq("all"),
        any(UserDetailsImpl.class),
        eq(null),
        eq(null),
        eq(null),
        anyInt(),
        any(),
        any(),
        any()
      )
    ).thenReturn(metrics);
    when(incidentMapper.toDashboardResponse(metrics)).thenReturn(response);

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.DASHBOARD + "/synthesis").with(
          user(mockUser("INCIDENT_VIEW_ALL"))
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.inflow").value(10))
      .andExpect(jsonPath("$.outflow").value(8));

    verify(dashboardService).getMetrics(
      eq("all"),
      any(UserDetailsImpl.class),
      eq(null),
      eq(null),
      eq(null),
      anyInt(),
      eq(PeriodType.LAST_30_DAYS),
      eq(null),
      eq(null)
    );
  }

  @Test
  @DisplayName(
    "GET /dashboard/metrics/comparison - Compares entities of the same type"
  )
  void getComparison_WithGlobalView_ReturnsComparison() throws Exception {
    ComparisonResponse response = ComparisonResponse.builder()
      .entityType("SERVICE")
      .entries(List.of())
      .build();

    when(
      dashboardService.getComparison(
        eq("SERVICE"),
        any(),
        any(UserDetailsImpl.class),
        anyInt(),
        any(),
        any(),
        any()
      )
    ).thenReturn(List.of());
    when(incidentMapper.toComparisonResponse(eq("SERVICE"), any())).thenReturn(
      response
    );

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.DASHBOARD + "/metrics/comparison")
          .param("entityType", "SERVICE")
          .param("ids", UUID.randomUUID().toString())
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.entityType").value("SERVICE"));

    verify(dashboardService).getComparison(
      eq("SERVICE"),
      any(),
      any(UserDetailsImpl.class),
      anyInt(),
      any(),
      any(),
      any()
    );
  }

  @Test
  @DisplayName(
    "GET /dashboard/period-activity - Returns the treated/resolved/closed flow lists"
  )
  void getPeriodActivity_WithPermission_ReturnsFlowLists() throws Exception {
    IncidentSummaryResponse treatedRow = new IncidentSummaryResponse();
    treatedRow.setReference("FT-I-2026-0043");
    IncidentSummaryResponse resolvedRow = new IncidentSummaryResponse();
    resolvedRow.setReference("FT-I-2026-0042");

    when(
      dashboardService.getPeriodActivity(
        eq("all"),
        any(UserDetailsImpl.class),
        eq(null),
        eq(null),
        eq(null),
        any(),
        any(),
        any()
      )
    ).thenReturn(PeriodActivityIncidents.builder().build());
    when(incidentMapper.toSummaryResponseList(any()))
      .thenReturn(List.of(treatedRow, resolvedRow));

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.DASHBOARD + "/period-activity")
          .param("view", "all")
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.treated[0].reference").value("FT-I-2026-0043"))
      .andExpect(jsonPath("$.resolved[1].reference").value("FT-I-2026-0042"))
      .andExpect(jsonPath("$.closed").isArray());

    verify(dashboardService).getPeriodActivity(
      eq("all"),
      any(UserDetailsImpl.class),
      eq(null),
      eq(null),
      eq(null),
      eq(null),
      eq(null),
      eq(null)
    );
  }

  @Test
  @DisplayName(
    "GET /dashboard/period-activity denies the agency filter without global view"
  )
  void getPeriodActivity_WithScopeFilterWithoutGlobal_ReturnsForbidden()
    throws Exception {
    mockMvc
      .perform(
        get(ApiConstants.Endpoints.DASHBOARD + "/period-activity")
          .param("view", "agency")
          .param("agencyId", UUID.randomUUID().toString())
          .with(user(mockUser("INCIDENT_VIEW_AGENCY")))
      )
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("GET /dashboard/metrics rejects an incomplete custom period")
  void getMetrics_WithIncompleteCustomPeriod_ReturnsBadRequest()
    throws Exception {
    mockMvc
      .perform(
        get(ApiConstants.Endpoints.DASHBOARD + "/metrics")
          .param("view", "all")
          .param("period", "CUSTOM")
          .param("dateFrom", "2026-06-01T00:00:00")
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /dashboard/metrics forwards a complete custom period")
  void getMetrics_WithCompleteCustomPeriod_ForwardsBounds() throws Exception {
    DashboardMetrics metrics = DashboardMetrics.builder().build();
    DashboardMetricsResponse response =
      DashboardMetricsResponse.builder().build();
    when(
      dashboardService.getMetrics(
        eq("all"),
        any(UserDetailsImpl.class),
        eq(null),
        eq(null),
        eq(null),
        anyInt(),
        eq(PeriodType.CUSTOM),
        any(),
        any()
      )
    ).thenReturn(metrics);
    when(incidentMapper.toDashboardResponse(metrics)).thenReturn(response);

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.DASHBOARD + "/metrics")
          .param("view", "all")
          .param("period", "CUSTOM")
          .param("dateFrom", "2026-06-01T00:00:00")
          .param("dateTo", "2026-06-10T23:59:59")
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
      )
      .andExpect(status().isOk());

    verify(dashboardService).getMetrics(
      eq("all"),
      any(UserDetailsImpl.class),
      eq(null),
      eq(null),
      eq(null),
      anyInt(),
      eq(PeriodType.CUSTOM),
      any(),
      any()
    );
  }
}
