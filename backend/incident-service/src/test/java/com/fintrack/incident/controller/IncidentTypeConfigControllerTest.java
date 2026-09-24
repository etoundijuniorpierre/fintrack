package com.fintrack.incident.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.incident.config.SecurityConfig;
import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.model.dto.request.IncidentTypeConfigRequest;
import com.fintrack.incident.model.dto.response.IncidentTypeConfigResponse;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.mapper.IncidentTypeConfigMapper;
import com.fintrack.incident.security.JwtUtils;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentTypeConfigService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IncidentTypeConfigController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@EnableMethodSecurity
class IncidentTypeConfigControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private IncidentTypeConfigService incidentTypeConfigService;

  @MockitoBean
  private IncidentTypeConfigMapper incidentTypeConfigMapper;

  @MockitoBean
  private JwtUtils jwtUtils;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private UUID configId;
  private IncidentTypeConfig testConfig;
  private IncidentTypeConfigResponse testResponse;
  private IncidentTypeConfigRequest testRequest;

  private UserDetailsImpl mockUser(String... authorities) {
    Set<SimpleGrantedAuthority> auths = new HashSet<>();
    for (String a : authorities) auths.add(new SimpleGrantedAuthority(a));
    return new UserDetailsImpl(UUID.randomUUID(), "admin", null, true, auths);
  }

  @BeforeEach
  void setUp() {
    configId = UUID.randomUUID();

    testConfig = new IncidentTypeConfig();
    testConfig.setId(configId);
    testConfig.setName("informatique");
    testConfig.setDisplayName("Informatique");
    testConfig.setActive(true);
    testConfig.setSlaHours(24);

    testResponse = new IncidentTypeConfigResponse();
    testResponse.setId(configId);
    testResponse.setName("informatique");
    testResponse.setDisplayName("Informatique");
    testResponse.setActive(true);
    testResponse.setSlaHours(24);

    testRequest = new IncidentTypeConfigRequest();
    testRequest.setName("informatique");
    testRequest.setDisplayName("Informatique");
    testRequest.setActive(true);
    testRequest.setSlaHours(24);
  }

  // ── GET /all ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName(
    "GET /incident-type-configs/all - Returns list with SETTINGS_INCIDENT_TYPES"
  )
  void findAll_WithPermission_ReturnsList() throws Exception {
    when(incidentTypeConfigService.findAll()).thenReturn(List.of(testConfig));
    when(incidentTypeConfigMapper.toResponse(any())).thenReturn(testResponse);

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/all").with(
          user(mockUser("SETTINGS_INCIDENT_TYPES"))
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].name").value("informatique"))
      .andExpect(jsonPath("$[0].displayName").value("Informatique"));

    verify(incidentTypeConfigService).findAll();
  }

  @Test
  @DisplayName(
    "GET /incident-type-configs/all - Accessible with INCIDENT_CREATE"
  )
  void findAll_WithIncidentCreate_ReturnsList() throws Exception {
    when(incidentTypeConfigService.findAll()).thenReturn(List.of(testConfig));
    when(incidentTypeConfigMapper.toResponse(any())).thenReturn(testResponse);

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/all").with(
          user(mockUser("INCIDENT_CREATE"))
        )
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /incident-type-configs/all - Forbidden without permission")
  void findAll_WithoutPermission_ReturnsForbidden() throws Exception {
    mockMvc
      .perform(
        get(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/all").with(
          user(mockUser("SOME_OTHER_PERMISSION"))
        )
      )
      .andExpect(status().isForbidden());

    verify(incidentTypeConfigService, never()).findAll();
  }

  // ── GET paginated ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /incident-type-configs - Returns paginated list")
  void findAllPaginated_WithPermission_ReturnsPage() throws Exception {
    when(incidentTypeConfigService.findAll(any(Pageable.class))).thenReturn(
      new PageImpl<>(List.of(testConfig))
    );
    when(incidentTypeConfigMapper.toResponse(any())).thenReturn(testResponse);

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS).with(
          user(mockUser("SETTINGS_INCIDENT_TYPES"))
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0].name").value("informatique"));
  }

  // ── GET /{id} ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /incident-type-configs/{id} - Returns config by ID")
  void findById_Exists_ReturnsConfig() throws Exception {
    when(incidentTypeConfigService.findById(configId)).thenReturn(testConfig);
    when(incidentTypeConfigMapper.toResponse(testConfig)).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/" + configId).with(
          user(mockUser("SETTINGS_INCIDENT_TYPES"))
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(configId.toString()))
      .andExpect(jsonPath("$.name").value("informatique"));

    verify(incidentTypeConfigService).findById(configId);
  }

  // ── GET /name/{name} ──────────────────────────────────────────────────────

  @Test
  @DisplayName(
    "GET /incident-type-configs/name/{name} - Returns config by name"
  )
  void findByName_Exists_ReturnsConfig() throws Exception {
    when(incidentTypeConfigService.findByName("informatique")).thenReturn(
      testConfig
    );
    when(incidentTypeConfigMapper.toResponse(testConfig)).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        get(
          ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/name/informatique"
        ).with(user(mockUser("SETTINGS_INCIDENT_TYPES")))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("informatique"));

    verify(incidentTypeConfigService).findByName("informatique");
  }

  // ── POST ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName(
    "POST /incident-type-configs - Creates config with SETTINGS_INCIDENT_TYPES"
  )
  void create_WithPermission_Returns201() throws Exception {
    when(incidentTypeConfigMapper.toEntity(any())).thenReturn(testConfig);
    when(incidentTypeConfigService.create(any(), any())).thenReturn(testConfig);
    when(incidentTypeConfigMapper.toResponse(testConfig)).thenReturn(
      testResponse
    );

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS)
          .with(csrf())
          .with(user(mockUser("SETTINGS_INCIDENT_TYPES")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(testRequest))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("informatique"));

    verify(incidentTypeConfigService).create(any(), any());
  }

  @Test
  @DisplayName(
    "POST /incident-type-configs - Forbidden without SETTINGS_INCIDENT_TYPES"
  )
  void create_WithoutPermission_ReturnsForbidden() throws Exception {
    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS)
          .with(csrf())
          .with(user(mockUser("INCIDENT_CREATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(testRequest))
      )
      .andExpect(status().isForbidden());

    verify(incidentTypeConfigService, never()).create(any(), any());
  }

  @Test
  @DisplayName("POST /incident-type-configs - Validation fails on blank name")
  void create_BlankName_ReturnsBadRequest() throws Exception {
    testRequest.setName("");

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS)
          .with(csrf())
          .with(user(mockUser("SETTINGS_INCIDENT_TYPES")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(testRequest))
      )
      .andExpect(status().isBadRequest());

    verify(incidentTypeConfigService, never()).create(any(), any());
  }

  // ── PUT /{id} ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName(
    "PUT /incident-type-configs/{id} - Updates config with SETTINGS_INCIDENT_TYPES"
  )
  void update_WithPermission_ReturnsOk() throws Exception {
    testRequest.setDisplayName("Informatique Updated");

    IncidentTypeConfigResponse updatedResponse =
      new IncidentTypeConfigResponse();
    updatedResponse.setId(configId);
    updatedResponse.setName("informatique");
    updatedResponse.setDisplayName("Informatique Updated");

    when(incidentTypeConfigService.findById(configId)).thenReturn(testConfig);
    when(
      incidentTypeConfigService.update(eq(configId), any(), any())
    ).thenReturn(testConfig);
    when(incidentTypeConfigMapper.toResponse(testConfig)).thenReturn(
      updatedResponse
    );

    mockMvc
      .perform(
        put(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/" + configId)
          .with(csrf())
          .with(user(mockUser("SETTINGS_INCIDENT_TYPES")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(testRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.displayName").value("Informatique Updated"));
  }

  @Test
  @DisplayName(
    "PUT /incident-type-configs/{id} - Forbidden without SETTINGS_INCIDENT_TYPES"
  )
  void update_WithoutPermission_ReturnsForbidden() throws Exception {
    mockMvc
      .perform(
        put(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/" + configId)
          .with(csrf())
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(testRequest))
      )
      .andExpect(status().isForbidden());

    verify(incidentTypeConfigService, never()).update(any(), any(), any());
  }

  // ── DELETE /{id} ──────────────────────────────────────────────────────────

  @Test
  @DisplayName(
    "DELETE /incident-type-configs/{id} - Deletes config with SETTINGS_INCIDENT_TYPES"
  )
  void delete_WithPermission_ReturnsNoContent() throws Exception {
    doNothing().when(incidentTypeConfigService).delete(eq(configId), any());

    mockMvc
      .perform(
        delete(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/" + configId)
          .with(csrf())
          .with(user(mockUser("SETTINGS_INCIDENT_TYPES")))
      )
      .andExpect(status().isNoContent());

    verify(incidentTypeConfigService).delete(eq(configId), any());
  }

  @Test
  @DisplayName(
    "DELETE /incident-type-configs/{id} - Forbidden without SETTINGS_INCIDENT_TYPES"
  )
  void delete_WithoutPermission_ReturnsForbidden() throws Exception {
    mockMvc
      .perform(
        delete(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/" + configId)
          .with(csrf())
          .with(user(mockUser("INCIDENT_DELETE")))
      )
      .andExpect(status().isForbidden());

    verify(incidentTypeConfigService, never()).delete(any(), any());
  }

  // ── Unauthenticated ───────────────────────────────────────────────────────

  @Test
  @DisplayName("GET /incident-type-configs/all - Unauthenticated returns 401")
  void findAll_Unauthenticated_Returns401() throws Exception {
    mockMvc
      .perform(get(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS + "/all"))
      .andExpect(status().isUnauthorized());
  }
}
