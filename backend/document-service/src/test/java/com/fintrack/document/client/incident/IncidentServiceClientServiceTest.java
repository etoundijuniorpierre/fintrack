package com.fintrack.document.client.incident;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fintrack.document.client.incident.dto.IncidentClientResponse;
import com.fintrack.document.exception.BusinessRuleViolationException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentServiceClientServiceTest {
  @Test
  void attachmentAuthorizationFailsClosedAndRequiresExplicitApproval() {
    assertThrows(BusinessRuleViolationException.class, () -> service.assertAttachmentAllowed(incidentId, "COMMENT", creatorId));
    when(incidentServiceClient.authorizeAttachment(incidentId, "COMMENT", creatorId)).thenReturn(true);
    assertDoesNotThrow(() -> service.assertAttachmentAllowed(incidentId, "COMMENT", creatorId));
    when(incidentServiceClient.authorizeAttachment(incidentId, "COMMENT", creatorId)).thenReturn(false);
    assertThrows(BusinessRuleViolationException.class, () -> service.assertAttachmentAllowed(incidentId, "COMMENT", creatorId));
  }

  @Mock
  private IncidentServiceClient incidentServiceClient;

  private IncidentServiceClientService service;
  private UUID incidentId;
  private UUID creatorId;
  private UUID assigneeId;

  @BeforeEach
  void setUp() {
    service = new IncidentServiceClientService(incidentServiceClient);
    incidentId = UUID.randomUUID();
    creatorId = UUID.randomUUID();
    assigneeId = UUID.randomUUID();
  }

  // ----- PJ de declaration : createur + statuts editables -----

  @Test
  @DisplayName(
    "assertCanModifyAttachments - allows the creator while the incident is editable"
  )
  void assertCanModifyAttachments_CreatorEditable_Allows() {
    when(incidentServiceClient.getIncidentById(incidentId)).thenReturn(
      incident("PENDING_VALIDATION", creatorId, assigneeId)
    );

    assertDoesNotThrow(() ->
      service.assertCanModifyAttachments(incidentId, creatorId)
    );
  }

  @Test
  @DisplayName(
    "assertCanModifyAttachments - rejects the creator once handling has started"
  )
  void assertCanModifyAttachments_CreatorInProgress_Rejects() {
    when(incidentServiceClient.getIncidentById(incidentId)).thenReturn(
      incident("IN_PROGRESS", creatorId, assigneeId)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      service.assertCanModifyAttachments(incidentId, creatorId)
    );
  }

  @Test
  @DisplayName(
    "assertCanModifyAttachments - rejects a non-creator (declaration is creator-only)"
  )
  void assertCanModifyAttachments_NonCreator_Rejects() {
    when(incidentServiceClient.getIncidentById(incidentId)).thenReturn(
      incident("OPEN", creatorId, assigneeId)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      service.assertCanModifyAttachments(incidentId, assigneeId)
    );
  }

  // ----- PJ de commentaire : tout acces, tant que non terminal -----

  @Test
  @DisplayName(
    "assertCanAttachToComment - allows while the incident is not terminal"
  )
  void assertCanAttachToComment_InProgress_Allows() {
    when(incidentServiceClient.getIncidentById(incidentId)).thenReturn(
      incident("IN_PROGRESS", creatorId, assigneeId)
    );

    assertDoesNotThrow(() ->
      service.assertCanAttachToComment(incidentId)
    );
  }

  @Test
  @DisplayName("assertCanAttachToComment - rejects once the incident is closed")
  void assertCanAttachToComment_Closed_Rejects() {
    when(incidentServiceClient.getIncidentById(incidentId)).thenReturn(
      incident("CLOSED", creatorId, assigneeId)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      service.assertCanAttachToComment(incidentId)
    );
  }

  // ----- PJ de resolution / cloture : assigne courant -----

  @Test
  @DisplayName(
    "assertCanAttachToResolution - allows the assignee while resolving/closing"
  )
  void assertCanAttachToResolution_Assignee_Allows() {
    when(incidentServiceClient.getIncidentById(incidentId)).thenReturn(
      incident("RESOLVED", creatorId, assigneeId)
    );

    assertDoesNotThrow(() ->
      service.assertCanAttachToResolution(incidentId, assigneeId)
    );
  }

  @Test
  @DisplayName(
    "assertCanAttachToResolution - allows a non-assignee actor on a non-terminal incident (treat/resolve/close performed by different actors)"
  )
  void assertCanAttachToResolution_NonAssignee_NonTerminal_Allows() {
    when(incidentServiceClient.getIncidentById(incidentId)).thenReturn(
      incident("TREATED", creatorId, assigneeId)
    );

    assertDoesNotThrow(() ->
      service.assertCanAttachToResolution(incidentId, creatorId)
    );
  }

  @Test
  @DisplayName(
    "assertCanAttachToResolution - rejects once the incident is terminal (closed/rejected/cancelled)"
  )
  void assertCanAttachToResolution_Terminal_Rejects() {
    when(incidentServiceClient.getIncidentById(incidentId)).thenReturn(
      incident("CANCELLED", creatorId, assigneeId)
    );

    assertThrows(BusinessRuleViolationException.class, () ->
      service.assertCanAttachToResolution(incidentId, assigneeId)
    );
  }

  private IncidentClientResponse incident(
    String status,
    UUID createdById,
    UUID assignedToId
  ) {
    IncidentClientResponse response = new IncidentClientResponse();
    response.setId(incidentId);
    response.setStatus(status);
    response.setCreatedById(createdById);
    response.setAssignedToId(assignedToId);
    return response;
  }
}
