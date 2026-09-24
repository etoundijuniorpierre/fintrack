package com.fintrack.incident.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fintrack.incident.model.constant.*;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.security.*;
import com.fintrack.incident.service.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.access.AccessDeniedException;

class IncidentAttachmentAccessControllerTest {
  final IncidentService incidents = mock(IncidentService.class);
  final IncidentCommentService comments = mock(IncidentCommentService.class);
  final IncidentAccessGuard access = mock(IncidentAccessGuard.class);
  final IncidentWorkflowGuard workflow = mock(IncidentWorkflowGuard.class);
  final IncidentAttachmentAccessController controller = new IncidentAttachmentAccessController(incidents, comments, access, workflow);
  final UUID id = UUID.randomUUID(), author = UUID.randomUUID();
  final UserDetailsImpl user = new UserDetailsImpl(author, "author", "", true, List.of());
  final Incident incident = new Incident();
  @BeforeEach void setup() {
    incident.setId(id); incident.setStatus(IncidentStatus.IN_PROGRESS);
    when(incidents.findById(id)).thenReturn(incident);
  }
  @ParameterizedTest
  @CsvSource({"INCIDENT,INCIDENT_UPDATE,UPDATE", "SOLUTION,INCIDENT_TREAT,SUBMIT_SOLUTION",
    "TREATMENT,INCIDENT_TREAT,TREAT", "RESOLUTION,INCIDENT_RESOLVE,RESOLVE",
    "UNRESOLVED,INCIDENT_RESOLVE,MARK_UNRESOLVED", "CLOSURE,INCIDENT_CLOSE,CLOSE",
    "CANCELLATION,INCIDENT_CANCEL,CANCEL"})
  void requiresPermissionAndWorkflowDecision(String category, String permission, IncidentAction action) {
    assertThatThrownBy(() -> controller.authorize(id, category, null, user)).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(workflow);
    when(access.hasAuthority(user, permission)).thenReturn(true);
    assertThat(controller.authorize(id, category, null, user).getBody()).isTrue();
    verify(workflow).assertCanAct(incident, user, action);
    doThrow(new AccessDeniedException("wrong actor or status")).when(workflow).assertCanAct(incident, user, action);
    assertThatThrownBy(() -> controller.authorize(id, category, null, user)).isInstanceOf(AccessDeniedException.class);
  }
  @Test void commentsMustExistAndBelongToTheEditableAuthor() {
    UUID comment = UUID.randomUUID();
    controller.authorize(id, "COMMENT", comment, user);
    verify(comments).assertCanModifyAttachments(id, comment, author);
    doThrow(new AccessDeniedException("wrong comment")).when(comments).assertCanModifyAttachments(id, comment, author);
    assertThatThrownBy(() -> controller.authorize(id, "COMMENT", comment, user)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.authorize(id, "COMMENT", null, user)).isInstanceOf(AccessDeniedException.class);
  }
  @Test void rejectsTerminalForeignAndUnknownTargets() {
    for (var status : IncidentStatus.TERMINAL_STATUSES) {
      incident.setStatus(status);
      assertThatThrownBy(() -> controller.authorize(id, "COMMENT", UUID.randomUUID(), user)).isInstanceOf(AccessDeniedException.class);
    }
    incident.setStatus(IncidentStatus.OPEN);
    assertThatThrownBy(() -> controller.authorize(id, "OTHER", null, user)).isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> controller.authorize(id, "INCIDENT", UUID.randomUUID(), user)).isInstanceOf(AccessDeniedException.class);
    doThrow(new AccessDeniedException("foreign agency")).when(access).assertCanView(incident, user);
    assertThatThrownBy(() -> controller.authorize(id, "COMMENT", UUID.randomUUID(), user)).isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(comments, workflow);
  }
}
