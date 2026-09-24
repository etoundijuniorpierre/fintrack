package com.fintrack.incident.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.incident.config.SecurityConfig;
import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.IncidentValidatorRole;
import com.fintrack.incident.model.dto.request.IncidentAssignRequest;
import com.fintrack.incident.model.dto.request.IncidentCloseRequest;
import com.fintrack.incident.model.dto.request.IncidentRejectRequest;
import com.fintrack.incident.model.dto.request.IncidentRelevanceRequest;
import com.fintrack.incident.model.dto.request.IncidentReopenRequest;
import com.fintrack.incident.model.dto.request.IncidentRequest;
import com.fintrack.incident.model.dto.request.IncidentResolveRequest;
import com.fintrack.incident.model.dto.request.IncidentTransferRequest;
import com.fintrack.incident.model.dto.request.IncidentTreatRequest;
import com.fintrack.incident.model.dto.request.IncidentUnresolvedRequest;
import com.fintrack.incident.model.dto.request.IncidentUpdateRequest;
import com.fintrack.incident.model.dto.request.IncidentValidateRequest;
import com.fintrack.incident.model.dto.response.IncidentResponse;
import com.fintrack.incident.model.dto.response.IncidentSummaryResponse;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.mapper.IncidentMapper;
import com.fintrack.incident.model.readmodel.ExpectedValidator;
import com.fintrack.incident.report.IncidentReportGenerator;
import com.fintrack.incident.security.IncidentAccessGuard;
import com.fintrack.incident.security.IncidentWorkflowGuard;
import com.fintrack.incident.security.JwtUtils;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentService;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

@WebMvcTest(IncidentController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@EnableMethodSecurity
class IncidentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private IncidentService incidentService;

  @MockitoBean
  private IncidentMapper incidentMapper;

  @MockitoBean
  private IncidentAccessGuard incidentAccessGuard;

  @MockitoBean
  private IncidentWorkflowGuard incidentWorkflowGuard;

  @MockitoBean
  private IncidentReportGenerator incidentReportGenerator;

  @MockitoBean
  private JwtUtils jwtUtils;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private UUID incidentId;
  private UUID userId;
  private UUID typeId;
  private IncidentRequest incidentRequest;
  private IncidentResponse incidentResponse;
  private IncidentSummaryResponse summaryResponse;

  private UserDetailsImpl mockUser(String... authorities) {
    Set<SimpleGrantedAuthority> auths = new HashSet<>();
    for (String a : authorities) auths.add(new SimpleGrantedAuthority(a));
    return new UserDetailsImpl(userId, "testuser", null, true, auths);
  }

  @BeforeEach
  void setUp() {
    incidentId = UUID.randomUUID();
    userId = UUID.randomUUID();
    typeId = UUID.randomUUID();

    incidentRequest = new IncidentRequest();
    incidentRequest.setTitle("Test incident");
    incidentRequest.setDescription("Description of the incident");
    incidentRequest.setTypeId(typeId);
    incidentRequest.setCriticality(Criticality.MEDIUM);

    incidentResponse = new IncidentResponse();
    incidentResponse.setId(incidentId);
    incidentResponse.setTitle("Test incident");
    incidentResponse.setStatus(IncidentStatus.OPEN);

    summaryResponse = new IncidentSummaryResponse();
    summaryResponse.setId(incidentId);
    summaryResponse.setTitle("Test incident");
  }

  @Test
  @DisplayName("GET /incidents - Returns paginated list")
  void getAllIncidents_Authenticated_ReturnsPage() throws Exception {
    when(
      incidentService.findFiltered(any(), any(), any(Pageable.class))
    ).thenReturn(new PageImpl<>(List.of(new Incident())));
    when(incidentMapper.toSummaryResponseList(any())).thenReturn(
      List.of(summaryResponse)
    );

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.INCIDENTS)
          .param("view", "all")
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
      )
      .andExpect(status().isOk());

    verify(incidentService).findFiltered(any(), any(), any(Pageable.class));
  }

  @Test
  @DisplayName("GET /incidents/{id} - Returns incident by ID")
  void getIncidentById_Exists_ReturnsIncident() throws Exception {
    Incident incident = new Incident();
    when(incidentService.findById(incidentId)).thenReturn(incident);
    when(incidentMapper.toResponse(incident)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId).with(
          user(mockUser("INCIDENT_VIEW_ALL"))
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(incidentId.toString()));

    verify(incidentService).findById(incidentId);
  }

  @Test
  @DisplayName(
    "GET /incidents/{id} - Exposes the scope verdicts and the expected validator"
  )
  void getIncidentById_ExposesScopeVerdictsAndExpectedValidator()
    throws Exception {
    Incident incident = new Incident();
    incident.setStatus(IncidentStatus.PENDING_VALIDATION);
    when(incidentService.findById(incidentId)).thenReturn(incident);
    when(incidentMapper.toResponse(incident)).thenReturn(incidentResponse);
    when(incidentWorkflowGuard.canValidate(eq(incident), any())).thenReturn(
      true
    );
    when(incidentWorkflowGuard.canCancel(eq(incident), any())).thenReturn(true);
    when(
      incidentWorkflowGuard.resolveExpectedValidator(incident)
    ).thenReturn(
      ExpectedValidator.builder()
        .role(IncidentValidatorRole.SERVICE_MANAGER)
        .targetName("Comptabilité")
        .build()
    );

    // Ces champs ne sont calcules que sur le detail : le front en depend pour
    // afficher les boutons Valider / Annuler et nommer le valideur attendu.
    mockMvc
      .perform(
        get(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId).with(
          user(mockUser("INCIDENT_VIEW_ALL"))
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.canValidate").value(true))
      .andExpect(jsonPath("$.canCancel").value(true))
      .andExpect(jsonPath("$.expectedValidatorRole").value("SERVICE_MANAGER"))
      .andExpect(jsonPath("$.expectedValidatorTarget").value("Comptabilité"));
  }

  @Test
  @DisplayName("GET /incidents/by-reference/{reference} - Returns incident by business code")
  void getIncidentByReference_Exists_ReturnsIncident() throws Exception {
    Incident incident = new Incident();
    when(incidentService.findByReference("FT-I-2026-0001")).thenReturn(
      incident
    );
    when(incidentMapper.toResponse(incident)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        get(
          ApiConstants.Endpoints.INCIDENTS + "/by-reference/FT-I-2026-0001"
        ).with(user(mockUser("INCIDENT_VIEW_ALL")))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(incidentId.toString()));

    verify(incidentService).findByReference("FT-I-2026-0001");
  }

  @Test
  @DisplayName("GET /incidents/{id}/report - Uses the requested interface language and sets title filename")
  void downloadIncidentReport_WithFrenchLanguage_UsesFrenchLocale()
    throws Exception {
    Incident incident = new Incident();
    incident.setTitle("Retard de virement agence");
    byte[] pdf = "%PDF-test".getBytes();
    when(incidentService.findById(incidentId)).thenReturn(incident);
    when(incidentMapper.toResponse(incident)).thenReturn(incidentResponse);
    when(
      incidentReportGenerator.generate(incidentResponse, Locale.FRENCH)
    ).thenReturn(pdf);

    mockMvc
      .perform(
        get(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/report")
          .header("Accept-Language", "fr")
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
      )
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_PDF))
      .andExpect(
        header()
          .string(
            "Content-Disposition",
            "form-data; name=\"attachment\"; filename=\"retard-de-virement-agence.pdf\""
          )
      );

    verify(incidentReportGenerator).generate(incidentResponse, Locale.FRENCH);
  }

  @Test
  @DisplayName(
    "POST /incidents - Requires validation when INCIDENT_CREATE_OPEN is absent"
  )
  void createIncident_WithoutCreateOpen_RequiresValidation() throws Exception {
    Incident entity = new Incident();
    when(incidentMapper.toEntity(any(IncidentRequest.class))).thenReturn(
      entity
    );
    when(
      incidentService.create(any(), any(), any(), eq(true), any(), anyBoolean())
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS)
          .with(csrf())
          .with(user(mockUser("INCIDENT_CREATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(incidentRequest))
      )
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName(
    "POST /incidents - Allows direct opening with INCIDENT_CREATE_OPEN"
  )
  void createIncident_WithCreateOpen_AllowsDirectOpening() throws Exception {
    Incident entity = new Incident();
    when(incidentMapper.toEntity(any(IncidentRequest.class))).thenReturn(
      entity
    );
    when(
      incidentService.create(
        any(),
        any(),
        any(),
        eq(false),
        any(),
        anyBoolean()
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS)
          .with(csrf())
          .with(user(mockUser("INCIDENT_CREATE", "INCIDENT_CREATE_OPEN")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(incidentRequest))
      )
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("POST /incidents - Validation fails on blank title")
  void createIncident_BlankTitle_ReturnsBadRequest() throws Exception {
    incidentRequest.setTitle("");

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS)
          .with(csrf())
          .with(user(mockUser("INCIDENT_CREATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(incidentRequest))
      )
      .andExpect(status().isBadRequest());

    verify(incidentService, never()).create(
      any(),
      any(),
      any(),
      anyBoolean(),
      any(),
      anyBoolean()
    );
  }

  @Test
  @DisplayName("POST /incidents - Unauthenticated returns 401 or 403")
  void createIncident_Unauthenticated_Returns401() throws Exception {
    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(incidentRequest))
      )
      .andExpect(status().is4xxClientError());
  }

  @Test
  @DisplayName("POST /incidents/{id}/validate - Validates incident")
  void validateIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentValidateRequest req = new IncidentValidateRequest();
    req.setComment("ok");

    Incident entity = new Incident();
    when(incidentService.findById(incidentId)).thenReturn(new Incident());
    when(
      incidentService.validate(
        eq(incidentId),
        eq(userId),
        eq("ok"),
        any(),
        any(),
        any()
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/validate")
          .with(csrf())
          .with(user(mockUser("INCIDENT_VALIDATE", "INCIDENT_VIEW_ALL")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/validate - Delegates scope validation to workflow guard"
  )
  void validateIncident_WithoutGlobalView_UsesWorkflowGuardDecision()
    throws Exception {
    IncidentValidateRequest req = new IncidentValidateRequest();
    req.setComment("service validation");

    Incident entity = new Incident();
    when(incidentService.findById(incidentId)).thenReturn(entity);
    when(
      incidentService.validate(
        eq(incidentId),
        eq(userId),
        eq("service validation"),
        any(),
        any(),
        any()
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/validate")
          .with(csrf())
          .with(user(mockUser("INCIDENT_VALIDATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());

    verify(incidentWorkflowGuard).assertCanAct(
      eq(entity),
      any(UserDetailsImpl.class),
      eq(com.fintrack.incident.model.constant.IncidentAction.VALIDATE)
    );
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/validate - Forbidden without INCIDENT_VALIDATE"
  )
  void validateIncident_WithoutPermission_ReturnsForbidden() throws Exception {
    IncidentValidateRequest req = new IncidentValidateRequest();

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/validate")
          .with(csrf())
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /incidents/{id}/reject - Rejects incident")
  void rejectIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentRejectRequest req = new IncidentRejectRequest();
    req.setReason("Not valid");

    Incident entity = new Incident();
    when(incidentService.findById(incidentId)).thenReturn(new Incident());
    when(
      incidentService.reject(eq(incidentId), eq(userId), eq("Not valid"), any())
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/reject")
          .with(csrf())
          .with(user(mockUser("INCIDENT_REJECT", "INCIDENT_VIEW_ALL")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /incidents/{id}/transfer - Transfers incident")
  void transferIncident_WithPermission_ReturnsOk() throws Exception {
    UUID targetServiceId = UUID.randomUUID();
    IncidentTransferRequest req = new IncidentTransferRequest();
    req.setTargetServiceId(targetServiceId);

    Incident entity = new Incident();
    when(
      incidentService.transfer(
        eq(incidentId),
        eq(userId),
        eq(targetServiceId),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/transfer")
          .with(csrf())
          .with(user(mockUser("INCIDENT_TRANSFER")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /incidents/{id}/transfer - Transfers incident to creator agency")
  void transferIncident_ToAgency_ReturnsOk() throws Exception {
    UUID targetAgencyId = UUID.randomUUID();
    IncidentTransferRequest req = new IncidentTransferRequest();
    req.setTargetAgencyId(targetAgencyId);
    Incident entity = new Incident();
    when(
      incidentService.transfer(
        eq(incidentId),
        eq(userId),
        any(),
        eq(targetAgencyId),
        any(),
        any(),
        any(),
        any()
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/transfer")
          .with(csrf())
          .with(user(mockUser("INCIDENT_TRANSFER")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /incidents/{id}/transfer - Rejects ambiguous destination")
  void transferIncident_WithServiceAndAgency_ReturnsBadRequest()
    throws Exception {
    IncidentTransferRequest req = new IncidentTransferRequest();
    req.setTargetServiceId(UUID.randomUUID());
    req.setTargetAgencyId(UUID.randomUUID());

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/transfer")
          .with(csrf())
          .with(user(mockUser("INCIDENT_TRANSFER")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /incidents/{id}/assign - Assigns incident")
  void assignIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentAssignRequest req = new IncidentAssignRequest();
    req.setAssignedTo(UUID.randomUUID());

    Incident entity = new Incident();
    when(
      incidentService.assign(eq(incidentId), eq(userId), any(), any())
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/assign")
          .with(csrf())
          .with(user(mockUser("INCIDENT_ASSIGN")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /incidents/{id}/resolve - Resolves incident")
  void resolveIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentResolveRequest req = new IncidentResolveRequest();
    req.setResolutionNote("Fixed the issue");

    Incident entity = new Incident();
    when(
      incidentService.resolve(
        eq(incidentId),
        eq(userId),
        eq("Fixed the issue")
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/resolve")
          .with(csrf())
          .with(user(mockUser("INCIDENT_RESOLVE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /incidents/{id}/treat - Marks incident as treated")
  void treatIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentTreatRequest req = new IncidentTreatRequest();
    req.setTreatmentDescription("Applied the fix");

    Incident entity = new Incident();
    when(
      incidentService.treat(
        eq(incidentId),
        eq(userId),
        eq("Applied the fix"),
        any(),
        any()
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/treat")
          .with(csrf())
          .with(user(mockUser("INCIDENT_TREAT")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/mark-unresolved - Returns a treated incident to processing"
  )
  void markUnresolved_WithPermission_ReturnsOk() throws Exception {
    IncidentUnresolvedRequest req = new IncidentUnresolvedRequest();
    req.setReason("Not actually resolved");

    Incident entity = new Incident();
    when(
      incidentService.markUnresolved(
        eq(incidentId),
        eq(userId),
        eq("Not actually resolved")
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.INCIDENTS +
          "/" +
          incidentId +
          "/mark-unresolved"
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_RESOLVE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /incidents/{id}/close - Closes incident")
  void closeIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentCloseRequest req = new IncidentCloseRequest();
    req.setClosureDescription("Issue fixed and verified");
    req.setComment("verified");

    Incident entity = new Incident();
    when(
      incidentService.close(eq(incidentId), eq(userId), any(), eq("verified"))
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/close")
          .with(csrf())
          .with(user(mockUser("INCIDENT_CLOSE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("DELETE /incidents/{id} - Deletes incident")
  void deleteIncident_WithPermission_ReturnsNoContent() throws Exception {
    doNothing().when(incidentService).delete(eq(incidentId), any());

    mockMvc
      .perform(
        delete(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId)
          .with(csrf())
          .with(user(mockUser("INCIDENT_DELETE")))
      )
      .andExpect(status().isNoContent());

    verify(incidentService).delete(eq(incidentId), eq(userId));
  }

  @Test
  @DisplayName("PUT /incidents/{id} - Updates incident")
  void updateIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentUpdateRequest updateRequest = new IncidentUpdateRequest();
    updateRequest.setTitle("Updated title");
    updateRequest.setDescription("Updated description");
    updateRequest.setTypeId(typeId);
    updateRequest.setCriticality(Criticality.MEDIUM);

    Incident existing = new Incident();
    existing.setCreatedBy(userId);
    Incident entity = new Incident();
    when(incidentService.findById(incidentId)).thenReturn(existing);
    when(
      incidentService.update(eq(incidentId), any(), eq(userId))
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        put(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId)
          .with(csrf())
          .with(user(mockUser("INCIDENT_UPDATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(updateRequest))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("PUT /incidents/{id} - Rejects a future incident date")
  void updateIncident_FutureIncidentDate_ReturnsBadRequest() throws Exception {
    String requestBody =
      """
      {
        "title": "Updated title",
        "description": "Updated description",
        "typeId": "%s",
        "criticality": "MEDIUM",
        "incidentDate": "2099-01-01",
        "observationDate": "2099-01-01"
      }
      """.formatted(typeId);

    mockMvc
      .perform(
        put(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId)
          .with(csrf())
          .with(user(mockUser("INCIDENT_UPDATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
      )
      .andExpect(status().isBadRequest());

    verify(incidentService, never()).update(any(), any(), any());
  }

  @Test
  @DisplayName("PUT /incidents/{id} - Rejects inconsistent business dates")
  void updateIncident_InconsistentDates_ReturnsBadRequest() throws Exception {
    String requestBody =
      """
      {
        "title": "Updated title",
        "description": "Updated description",
        "typeId": "%s",
        "criticality": "MEDIUM",
        "incidentDate": "2026-01-15",
        "observationDate": "2026-01-14"
      }
      """.formatted(typeId);

    mockMvc
      .perform(
        put(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId)
          .with(csrf())
          .with(user(mockUser("INCIDENT_UPDATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
      )
      .andExpect(status().isBadRequest());

    verify(incidentService, never()).update(any(), any(), any());
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/reject - Forbidden without INCIDENT_REJECT"
  )
  void rejectIncident_WithoutPermission_ReturnsForbidden() throws Exception {
    IncidentRejectRequest req = new IncidentRejectRequest();
    req.setReason("reason");

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/reject")
          .with(csrf())
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /incidents/{id}/reopen - Reopens incident")
  void reopenIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentReopenRequest req = new IncidentReopenRequest();
    req.setReason("need more info");
    req.setComment("please provide logs");

    Incident entity = new Incident();
    when(
      incidentService.reopen(
        eq(incidentId),
        eq(userId),
        eq("need more info"),
        eq("please provide logs")
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/reopen")
          .with(csrf())
          .with(user(mockUser("INCIDENT_REOPEN")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /incidents/{id}/clone - Clones incident")
  void cloneIncident_WithPermission_ReturnsOk() throws Exception {
    Incident entity = new Incident();
    when(incidentService.cloneIncident(eq(incidentId), eq(userId))).thenReturn(
      entity
    );
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/clone")
          .with(csrf())
          .with(user(mockUser("INCIDENT_CREATE")))
      )
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("POST /incidents/{id}/resubmit - Resubmits incident")
  void resubmitIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentValidateRequest req = new IncidentValidateRequest();
    req.setComment("fixed");

    Incident entity = new Incident();
    when(
      incidentService.resubmit(eq(incidentId), eq(userId), eq("fixed"), eq(true))
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/resubmit")
          .with(csrf())
          .with(user(mockUser("INCIDENT_CREATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/request-confirmation - Asks the source entity for the incident's relevance"
  )
  void requestConfirmation_WithPermission_ReturnsOk() throws Exception {
    IncidentRelevanceRequest req = new IncidentRelevanceRequest();
    req.setComment("toujours d'actualite ?");

    Incident entity = new Incident();
    when(
      incidentService.requestConfirmation(
        eq(incidentId),
        eq(userId),
        eq("toujours d'actualite ?")
      )
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.INCIDENTS +
          "/" +
          incidentId +
          "/request-confirmation"
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_TREAT")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/confirm-relevance - Confirms the incident is still relevant"
  )
  void confirmRelevance_WithPermission_ReturnsOk() throws Exception {
    IncidentRelevanceRequest req = new IncidentRelevanceRequest();

    Incident entity = new Incident();
    when(
      incidentService.confirmRelevance(eq(incidentId), eq(userId), isNull())
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.INCIDENTS +
          "/" +
          incidentId +
          "/confirm-relevance"
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_VALIDATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /incidents/{id}/cancel - Cancels incident")
  void cancelIncident_WithPermission_ReturnsOk() throws Exception {
    IncidentRejectRequest req = new IncidentRejectRequest();
    req.setReason("mistake");

    Incident entity = new Incident();
    when(
      incidentService.cancel(eq(incidentId), eq(userId), eq("mistake"))
    ).thenReturn(entity);
    when(incidentMapper.toResponse(entity)).thenReturn(incidentResponse);

    mockMvc
      .perform(
        post(ApiConstants.Endpoints.INCIDENTS + "/" + incidentId + "/cancel")
          .with(csrf())
          .with(user(mockUser("INCIDENT_CANCEL")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(req))
      )
      .andExpect(status().isOk());
  }
}
