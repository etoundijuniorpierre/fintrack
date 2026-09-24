package com.fintrack.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.user.config.TestSecurityConfig;
import com.fintrack.user.model.dto.request.AgencyRequest;
import com.fintrack.user.model.dto.response.AgencyResponse;
import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.mapper.AgencyMapper;
import com.fintrack.user.security.UserAccessGuard;
import com.fintrack.user.service.AgencyService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgencyController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AgencyControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AgencyService agencyService;

  @MockitoBean
  private AgencyMapper agencyMapper;

  @MockitoBean(name = "userAccess")
  private UserAccessGuard userAccess;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private AgencyRequest agencyRequest;
  private AgencyResponse agencyResponse;
  private UUID agencyId;

  @BeforeEach
  void setUp() {
    agencyId = UUID.randomUUID();

    agencyRequest = new AgencyRequest();
    agencyRequest.setName("Paris Branch");
    agencyRequest.setCity("Paris");

    agencyResponse = new AgencyResponse();
    agencyResponse.setId(agencyId);
    agencyResponse.setName("Paris Branch");
    agencyResponse.setCode("PR-01");
  }

  @Test
  @WithMockUser(authorities = "USER_VIEW_ALL")
  @DisplayName("Get agency by ID - Success")
  void getAgencyById_agencyExists_returnsAgencyResponse() throws Exception {
    Agency agency = new Agency();
    agency.setId(agencyId);
    when(userAccess.canViewAgency(agencyId)).thenReturn(true);
    when(agencyService.findById(agencyId)).thenReturn(agency);
    when(agencyMapper.agencyToAgencyResponse(agency)).thenReturn(
      agencyResponse
    );

    mockMvc
      .perform(get("/api/v1/userService/agencies/" + agencyId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("Paris Branch"));

    verify(agencyService, times(1)).findById(agencyId);
    verify(agencyMapper, times(1)).agencyToAgencyResponse(agency);
  }

  @Test
  @WithMockUser(authorities = "SETTINGS_SYSTEM")
  @DisplayName("Create agency - Success")
  void createAgency_validRequest_returnsCreatedAgencyResponse()
    throws Exception {
    Agency agency = new Agency();
    when(
      agencyMapper.agencyRequestToAgency(any(AgencyRequest.class))
    ).thenReturn(agency);
    when(agencyService.create(any(Agency.class))).thenReturn(agency);
    when(agencyMapper.agencyToAgencyResponse(any(Agency.class))).thenReturn(
      agencyResponse
    );

    mockMvc
      .perform(
        post("/api/v1/userService/agencies")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(agencyRequest))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.code").value("PR-01"));

    verify(agencyMapper, times(1)).agencyRequestToAgency(
      any(AgencyRequest.class)
    );
    verify(agencyService, times(1)).create(any(Agency.class));
    verify(agencyMapper, times(1)).agencyToAgencyResponse(any(Agency.class));
  }
}
