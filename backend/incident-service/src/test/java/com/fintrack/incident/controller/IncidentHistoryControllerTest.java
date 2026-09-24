package com.fintrack.incident.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fintrack.incident.config.SecurityConfig;
import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.model.dto.response.IncidentHistoryResponse;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.model.mapper.IncidentHistoryMapper;
import com.fintrack.incident.security.IncidentAccessGuard;
import com.fintrack.incident.security.JwtUtils;
import com.fintrack.incident.service.IncidentHistoryService;
import com.fintrack.incident.service.IncidentService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IncidentHistoryController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@EnableMethodSecurity
class IncidentHistoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private IncidentHistoryService historyService;

  @MockitoBean
  private IncidentHistoryMapper historyMapper;

  @MockitoBean
  private IncidentService incidentService;

  @MockitoBean
  private IncidentAccessGuard incidentAccessGuard;

  @MockitoBean
  private JwtUtils jwtUtils;

  private UUID incidentId;

  @BeforeEach
  void setUp() {
    incidentId = UUID.randomUUID();
  }

  @Test
  @WithMockUser(authorities = "INCIDENT_VIEW_ALL")
  @DisplayName("GET /incidents/{id}/history - Returns history list")
  void getIncidentHistory_WithPermission_ReturnsHistory() throws Exception {
    IncidentHistory h = new IncidentHistory();
    IncidentHistoryResponse resp = new IncidentHistoryResponse();
    when(historyService.findByIncidentId(incidentId)).thenReturn(List.of(h));
    when(historyMapper.toResponseList(List.of(h))).thenReturn(List.of(resp));

    mockMvc
      .perform(
        get(
          ApiConstants.Endpoints.INCIDENT_HISTORY +
            "/" +
            incidentId +
            "/history"
        )
      )
      .andExpect(status().isOk());

    verify(historyService).findByIncidentId(incidentId);
  }

  @Test
  @WithMockUser(authorities = "INCIDENT_VIEW_AGENCY")
  @DisplayName(
    "GET /incidents/{id}/history - INCIDENT_VIEW_AGENCY also allowed"
  )
  void getIncidentHistory_ViewAgency_ReturnsHistory() throws Exception {
    when(historyService.findByIncidentId(incidentId)).thenReturn(List.of());
    when(historyMapper.toResponseList(List.of())).thenReturn(List.of());

    mockMvc
      .perform(
        get(
          ApiConstants.Endpoints.INCIDENT_HISTORY +
            "/" +
            incidentId +
            "/history"
        )
      )
      .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(authorities = "INCIDENT_CREATE")
  @DisplayName(
    "GET /incidents/{id}/history - Forbidden without view permission"
  )
  void getIncidentHistory_WithoutViewPermission_ReturnsForbidden()
    throws Exception {
    mockMvc
      .perform(
        get(
          ApiConstants.Endpoints.INCIDENT_HISTORY +
            "/" +
            incidentId +
            "/history"
        )
      )
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName(
    "GET /incidents/{id}/history - Unauthenticated returns 401 or 403"
  )
  void getIncidentHistory_Unauthenticated_Returns401() throws Exception {
    mockMvc
      .perform(
        get(
          ApiConstants.Endpoints.INCIDENT_HISTORY +
            "/" +
            incidentId +
            "/history"
        )
      )
      .andExpect(status().is4xxClientError());
  }
}
