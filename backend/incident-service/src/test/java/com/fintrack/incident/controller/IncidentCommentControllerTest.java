package com.fintrack.incident.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.incident.config.SecurityConfig;
import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.dto.request.IncidentCommentRequest;
import com.fintrack.incident.model.dto.response.IncidentCommentResponse;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentComment;
import com.fintrack.incident.model.mapper.IncidentCommentMapper;
import com.fintrack.incident.security.IncidentAccessGuard;
import com.fintrack.incident.security.JwtUtils;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentCommentService;
import com.fintrack.incident.service.IncidentService;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IncidentCommentController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
@EnableMethodSecurity
class IncidentCommentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private IncidentCommentService commentService;

  @MockitoBean
  private IncidentCommentMapper commentMapper;

  @MockitoBean
  private IncidentService incidentService;

  @MockitoBean
  private IncidentAccessGuard incidentAccessGuard;

  @MockitoBean
  private JwtUtils jwtUtils;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private UUID incidentId;
  private UUID userId;
  private IncidentCommentRequest commentRequest;
  private IncidentCommentResponse commentResponse;

  private UserDetailsImpl mockUser(String... authorities) {
    Set<SimpleGrantedAuthority> auths = new HashSet<>();
    for (String a : authorities) auths.add(new SimpleGrantedAuthority(a));
    return new UserDetailsImpl(userId, "testuser", null, true, auths);
  }

  @BeforeEach
  void setUp() {
    incidentId = UUID.randomUUID();
    userId = UUID.randomUUID();

    commentRequest = new IncidentCommentRequest();
    commentRequest.setContent("Test comment");
    commentRequest.setInternal(false);

    commentResponse = new IncidentCommentResponse();
    commentResponse.setId(UUID.randomUUID());
    commentResponse.setContent("Test comment");
  }

  @Test
  @DisplayName("GET /incidents/{id}/comments - Returns all comments")
  void getIncidentComments_WithPermission_ReturnsComments() throws Exception {
    IncidentComment comment = new IncidentComment();
    when(commentService.findByIncidentId(incidentId)).thenReturn(
      List.of(comment)
    );
    when(commentMapper.toResponseList(List.of(comment))).thenReturn(
      List.of(commentResponse)
    );

    mockMvc
      .perform(
        get(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments"
        ).with(user(mockUser("INCIDENT_VIEW_ALL")))
      )
      .andExpect(status().isOk());

    verify(commentService).findByIncidentId(incidentId);
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/comments - Adds comment with INCIDENT_TREAT"
  )
  void addComment_WithTreatPermission_Returns201() throws Exception {
    Incident mockIncident = new Incident();
    mockIncident.setStatus(IncidentStatus.OPEN);
    when(incidentService.findById(incidentId)).thenReturn(mockIncident);

    IncidentComment saved = new IncidentComment();
    when(
      commentService.addComment(
        eq(incidentId),
        eq(userId),
        eq("Test comment"),
        eq(false)
      )
    ).thenReturn(saved);
    when(commentMapper.toResponse(saved)).thenReturn(commentResponse);

    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments"
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_TREAT")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(commentRequest))
      )
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/comments - Refuses a comment on a cancelled incident"
  )
  void addComment_OnCancelledIncident_IsRefused() throws Exception {
    // ANNULE est terminal au meme titre que CLOTURE et REJETE : ses echanges sont
    // figes. La liste ecrite a la main ici l'avait oublie.
    Incident mockIncident = new Incident();
    mockIncident.setStatus(IncidentStatus.CANCELLED);
    when(incidentService.findById(incidentId)).thenReturn(mockIncident);

    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments"
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_TREAT")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(commentRequest))
      )
      .andExpect(status().is4xxClientError());

    verify(commentService, never()).addComment(any(), any(), any(), anyBoolean());
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/comments - Binds the isInternal flag from the JSON body"
  )
  void addComment_InternalFlag_IsBoundFromJson() throws Exception {
    Incident mockIncident = new Incident();
    mockIncident.setStatus(IncidentStatus.OPEN);
    when(incidentService.findById(incidentId)).thenReturn(mockIncident);

    IncidentComment saved = new IncidentComment();
    when(
      commentService.addComment(
        eq(incidentId),
        eq(userId),
        eq("Internal note"),
        eq(true)
      )
    ).thenReturn(saved);
    when(commentMapper.toResponse(saved)).thenReturn(commentResponse);

    // JSON brut avec la clé "isInternal" telle qu'envoyée par le frontend.
    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments"
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_TREAT")))
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"content\":\"Internal note\",\"isInternal\":true}")
      )
      .andExpect(status().isCreated());

    verify(commentService).addComment(
      eq(incidentId),
      eq(userId),
      eq("Internal note"),
      eq(true)
    );
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/comments - Adds comment with INCIDENT_VIEW_OWN"
  )
  void addComment_WithViewOwnPermission_Returns201() throws Exception {
    Incident mockIncident = new Incident();
    mockIncident.setStatus(IncidentStatus.OPEN);
    when(incidentService.findById(incidentId)).thenReturn(mockIncident);

    IncidentComment saved = new IncidentComment();
    when(
      commentService.addComment(any(), any(), any(), anyBoolean())
    ).thenReturn(saved);
    when(commentMapper.toResponse(saved)).thenReturn(commentResponse);

    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments"
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_VIEW_OWN")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(commentRequest))
      )
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/comments - Forbidden without view/treat permission"
  )
  void addComment_WithoutPermission_ReturnsForbidden() throws Exception {
    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments"
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_CREATE")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(commentRequest))
      )
      .andExpect(status().isForbidden());

    verify(commentService, never()).addComment(
      any(),
      any(),
      any(),
      anyBoolean()
    );
  }

  @Test
  @DisplayName(
    "POST /incidents/{id}/comments - Validation fails on blank content"
  )
  void addComment_BlankContent_ReturnsBadRequest() throws Exception {
    commentRequest.setContent("");

    mockMvc
      .perform(
        post(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments"
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_VIEW_ALL")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(commentRequest))
      )
      .andExpect(status().isBadRequest());

    verify(commentService, never()).addComment(
      any(),
      any(),
      any(),
      anyBoolean()
    );
  }

  @Test
  @DisplayName(
    "GET /incidents/{id}/comments - Unauthenticated returns 401 or 403"
  )
  void getComments_Unauthenticated_Returns401() throws Exception {
    mockMvc
      .perform(
        get(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments"
        )
      )
      .andExpect(status().is4xxClientError());
  }
  @Test
  @DisplayName("PUT /incidents/{id}/comments/{commentId} - Updates a comment")
  void updateComment_WithPermission_Returns200() throws Exception {
    Incident mockIncident = new Incident();
    mockIncident.setStatus(IncidentStatus.OPEN);
    when(incidentService.findById(incidentId)).thenReturn(mockIncident);

    UUID commentId = UUID.randomUUID();
    IncidentComment updated = new IncidentComment();
    when(
      commentService.updateComment(
        eq(incidentId),
        eq(commentId),
        eq(userId),
        eq("Test comment")
      )
    ).thenReturn(updated);
    when(commentMapper.toResponse(updated)).thenReturn(commentResponse);

    mockMvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments/" +
            commentId
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_TREAT")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(commentRequest))
      )
      .andExpect(status().isOk());

    verify(commentService).updateComment(incidentId, commentId, userId, "Test comment");
  }

  @Test
  @DisplayName("DELETE /incidents/{id}/comments/{commentId} - Deletes a comment")
  void deleteComment_WithPermission_Returns204() throws Exception {
    Incident mockIncident = new Incident();
    mockIncident.setStatus(IncidentStatus.OPEN);
    when(incidentService.findById(incidentId)).thenReturn(mockIncident);

    UUID commentId = UUID.randomUUID();

    mockMvc
      .perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
          ApiConstants.Endpoints.INCIDENT_COMMENTS +
            "/" +
            incidentId +
            "/comments/" +
            commentId
        )
          .with(csrf())
          .with(user(mockUser("INCIDENT_TREAT")))
      )
      .andExpect(status().isNoContent());

    verify(commentService).deleteComment(incidentId, commentId, userId);
  }

  @Test
  void automaticDeletionUsesTheEmptyOnlyProtocol() throws Exception {
    Incident incident = new Incident();
    incident.setStatus(IncidentStatus.OPEN);
    when(incidentService.findById(incidentId)).thenReturn(incident);
    UUID commentId = UUID.randomUUID();
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
      ApiConstants.Endpoints.INCIDENT_COMMENTS + "/" + incidentId + "/comments/" + commentId)
      .param("onlyIfEmpty", "true").with(csrf()).with(user(mockUser("INCIDENT_TREAT"))))
      .andExpect(status().isNoContent());
    verify(commentService).deleteCommentIfEmpty(incidentId, commentId, userId);
    org.mockito.Mockito.verify(commentService, org.mockito.Mockito.never()).deleteComment(any(), any(), any());
  }
}
