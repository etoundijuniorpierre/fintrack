package com.fintrack.incident.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.exception.EntityNotFoundException;
import com.fintrack.incident.exception.ErrorCode;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentComment;
import com.fintrack.incident.repository.IncidentCommentRepository;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.service.IncidentHistoryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentCommentServiceImplTest {
  @Test
  void automaticDeletionStopsWhenAttachmentsMayExist() {
    UUID commentId = UUID.randomUUID();
    IncidentComment comment = new IncidentComment();
    comment.setId(commentId); comment.setIncident(incident); comment.setUserId(userId);
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    org.mockito.Mockito.doThrow(new BusinessRuleViolationException(ErrorCode.BUSINESS_RULE_VIOLATION, "not empty"))
      .when(attachmentCleanup).sealEmptyComment(incidentId, commentId);
    assertThrows(BusinessRuleViolationException.class, () -> commentService.deleteCommentIfEmpty(incidentId, commentId, userId));
    verify(commentRepository, never()).delete(any());
    verify(attachmentCleanup, never()).enqueue(any(), any());
  }

  @Mock
  private IncidentCommentRepository commentRepository;

  @Mock
  private IncidentRepository incidentRepository;

  @Mock
  private IncidentHistoryService historyService;

  @Mock
  private NotificationClientService notificationClientService;
  @Mock private com.fintrack.incident.service.AttachmentCleanupService attachmentCleanup;

  @InjectMocks
  private IncidentCommentServiceImpl commentService;

  private UUID incidentId;
  private UUID userId;
  private Incident incident;

  @BeforeEach
  void setUp() {
    incidentId = UUID.randomUUID();
    userId = UUID.randomUUID();

    incident = new Incident();
    incident.setId(incidentId);
    incident.setTitle("Test");
    incident.setDescription("Desc");
    incident.setTypeId(UUID.randomUUID());
    incident.setCriticality(Criticality.MEDIUM);
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    incident.setCreatedBy(userId);
    incident.setAgencyId(UUID.randomUUID());
  }

  @Test
  @DisplayName("addComment - Saves comment and records history")
  void addComment_ValidData_SavesAndRecordsHistory() {
    IncidentComment saved = new IncidentComment();
    saved.setId(UUID.randomUUID());
    saved.setIncident(incident);
    saved.setUserId(userId);
    saved.setContent("Test comment");
    saved.setInternal(false);

    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(commentRepository.save(any())).thenReturn(saved);
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    IncidentComment result = commentService.addComment(
      incidentId,
      userId,
      "Test comment",
      false
    );

    assertThat(result.getContent()).isEqualTo("Test comment");
    assertThat(result.isInternal()).isFalse();
    verify(historyService).record(
      eq(incident),
      eq(userId),
      any(),
      any(),
      any(),
      any()
    );
  }

  @Test
  @DisplayName(
    "addComment - Internal comment records [internal] marker in history"
  )
  void addComment_Internal_RecordsInternalMarker() {
    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    commentService.addComment(incidentId, userId, "Internal note", true);

    verify(historyService).record(
      any(),
      any(),
      any(),
      any(),
      any(),
      eq("[internal]")
    );
  }

  @Test
  @DisplayName(
    "addComment - Reply links parent comment and records reply marker"
  )
  void addComment_Reply_LinksParentComment() {
    UUID parentId = UUID.randomUUID();
    IncidentComment parent = new IncidentComment();
    parent.setId(parentId);
    parent.setIncident(incident);
    parent.setUserId(UUID.randomUUID());

    when(incidentRepository.findById(incidentId)).thenReturn(
      Optional.of(incident)
    );
    when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));
    when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(
      historyService.record(any(), any(), any(), any(), any(), any())
    ).thenReturn(null);

    IncidentComment result = commentService.addComment(
      incidentId,
      userId,
      "Reply",
      false,
      parentId
    );

    assertThat(result.getParentComment()).isEqualTo(parent);
    verify(historyService).record(
      any(),
      any(),
      any(),
      any(),
      any(),
      eq("[reply]")
    );
    verify(notificationClientService).notifyIncidentCommentReply(
      incident,
      userId,
      parent.getUserId()
    );
    verify(notificationClientService, never()).notifyIncidentComment(
      any(),
      any()
    );
  }


  @Test
  @DisplayName(
    "addComment - Throws EntityNotFoundException when incident not found"
  )
  void addComment_IncidentNotFound_ThrowsEntityNotFoundException() {
    when(incidentRepository.findById(incidentId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
      commentService.addComment(incidentId, userId, "comment", false)
    );
    verify(commentRepository, never()).save(any());
  }

  @Test
  @DisplayName("findByIncidentId - Returns all comments ordered by createdAt")
  void findByIncidentId_ReturnsComments() {
    IncidentComment c1 = new IncidentComment();
    IncidentComment c2 = new IncidentComment();
    when(
      commentRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId)
    ).thenReturn(List.of(c1, c2));

    List<IncidentComment> result = commentService.findByIncidentId(incidentId);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("findPublicByIncidentId - Returns only non-internal comments")
  void findPublicByIncidentId_ReturnsPublicOnly() {
    IncidentComment pub = new IncidentComment();
    pub.setInternal(false);
    when(
      commentRepository.findByIncidentIdAndIsInternalOrderByCreatedAtAsc(
        incidentId,
        false
      )
    ).thenReturn(List.of(pub));

    List<IncidentComment> result = commentService.findPublicByIncidentId(
      incidentId
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isInternal()).isFalse();
  }

  @Test
  @DisplayName("findById - Returns comment when found")
  void findById_Found_ReturnsComment() {
    UUID commentId = UUID.randomUUID();
    IncidentComment comment = new IncidentComment();
    comment.setId(commentId);
    when(commentRepository.findById(commentId)).thenReturn(
      Optional.of(comment)
    );

    IncidentComment result = commentService.findById(commentId);

    assertThat(result.getId()).isEqualTo(commentId);
  }

  @Test
  @DisplayName("findById - Throws EntityNotFoundException when not found")
  void findById_NotFound_ThrowsEntityNotFoundException() {
    UUID commentId = UUID.randomUUID();
    when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
      commentService.findById(commentId)
    );
  }

  // Prepare un commentaire de l'utilisateur courant, editable par defaut.
  private IncidentComment ownComment(UUID commentId) {
    IncidentComment comment = new IncidentComment();
    comment.setId(commentId);
    comment.setIncident(incident);
    comment.setUserId(userId);
    comment.setContent("Old content");
    comment.setCreatedAt(LocalDateTime.now());
    return comment;
  }

  @Test
  @DisplayName("updateComment - Valid request updates comment and records history")
  void updateComment_ValidRequest_UpdatesAndRecordsHistory() {
    UUID commentId = UUID.randomUUID();
    IncidentComment comment = ownComment(commentId);

    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentRepository.existsByParentCommentId(commentId)).thenReturn(false);
    when(
      commentRepository.existsByIncidentIdAndCreatedAtAfterAndUserIdNot(
        eq(incidentId),
        any(),
        eq(userId)
      )
    ).thenReturn(false);
    when(commentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    IncidentComment result = commentService.updateComment(
      incidentId,
      commentId,
      userId,
      "New content"
    );

    assertThat(result.getContent()).isEqualTo("New content");
    verify(historyService).record(
      eq(incident),
      eq(userId),
      any(),
      any(),
      any(),
      eq("[edited]")
    );
  }

  @Test
  @DisplayName("deleteComment - Valid request deletes comment and records history")
  void deleteComment_ValidRequest_DeletesAndRecordsHistory() {
    UUID commentId = UUID.randomUUID();
    IncidentComment comment = ownComment(commentId);

    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentRepository.existsByParentCommentId(commentId)).thenReturn(false);
    when(
      commentRepository.existsByIncidentIdAndCreatedAtAfterAndUserIdNot(
        eq(incidentId),
        any(),
        eq(userId)
      )
    ).thenReturn(false);

    commentService.deleteComment(incidentId, commentId, userId);
    verify(attachmentCleanup).enqueue(incidentId, commentId);

    verify(commentRepository).delete(comment);
    verify(historyService).record(
      eq(incident),
      eq(userId),
      any(),
      any(),
      any(),
      eq("[deleted]")
    );
  }

  @Test
  @DisplayName("updateComment - Rejects a comment authored by someone else")
  void updateComment_NotAuthor_ThrowsBusinessRule() {
    UUID commentId = UUID.randomUUID();
    IncidentComment comment = ownComment(commentId);
    comment.setUserId(UUID.randomUUID());
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> commentService.updateComment(incidentId, commentId, userId, "x")
    );
    assertThat(ex.getErrorCode()).isEqualTo(
      ErrorCode.INCIDENT_COMMENT_NOT_AUTHOR
    );
    verify(commentRepository, never()).save(any());
  }

  @Test
  @DisplayName("deleteComment - Rejects a comment that already has a reply")
  void deleteComment_HasReply_ThrowsBusinessRule() {
    UUID commentId = UUID.randomUUID();
    IncidentComment comment = ownComment(commentId);
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentRepository.existsByParentCommentId(commentId)).thenReturn(true);

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> commentService.deleteComment(incidentId, commentId, userId)
    );
    assertThat(ex.getErrorCode()).isEqualTo(
      ErrorCode.INCIDENT_COMMENT_HAS_REPLY
    );
    verify(commentRepository, never()).delete(any());
  }

  @Test
  @DisplayName("updateComment - Rejects a comment superseded by another user")
  void updateComment_Superseded_ThrowsBusinessRule() {
    UUID commentId = UUID.randomUUID();
    IncidentComment comment = ownComment(commentId);
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentRepository.existsByParentCommentId(commentId)).thenReturn(false);
    when(
      commentRepository.existsByIncidentIdAndCreatedAtAfterAndUserIdNot(
        eq(incidentId),
        any(),
        eq(userId)
      )
    ).thenReturn(true);

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () -> commentService.updateComment(incidentId, commentId, userId, "x")
    );
    assertThat(ex.getErrorCode()).isEqualTo(
      ErrorCode.INCIDENT_COMMENT_SUPERSEDED
    );
  }

  @Test
  @DisplayName("updateComment - Rejects a comment from another incident")
  void updateComment_WrongIncident_ThrowsBusinessRule() {
    UUID commentId = UUID.randomUUID();
    IncidentComment comment = ownComment(commentId);
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

    BusinessRuleViolationException ex = assertThrows(
      BusinessRuleViolationException.class,
      () ->
        commentService.updateComment(
          UUID.randomUUID(),
          commentId,
          userId,
          "x"
        )
    );
    assertThat(ex.getErrorCode()).isEqualTo(
      ErrorCode.INCIDENT_COMMENT_WRONG_INCIDENT
    );
  }

  @Test
  @DisplayName("deleteComment - Throws when the comment does not exist")
  void deleteComment_NotFound_ThrowsEntityNotFound() {
    UUID commentId = UUID.randomUUID();
    when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
      commentService.deleteComment(incidentId, commentId, userId)
    );
  }
}
