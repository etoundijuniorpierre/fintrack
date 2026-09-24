// Service metier : coordonne les operations du domaine incident comment.

package com.fintrack.incident.service.impl;

import com.fintrack.incident.client.notification.NotificationClientService;
import com.fintrack.incident.exception.EntityNotFoundException;
import com.fintrack.incident.exception.ErrorCode;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentComment;
import com.fintrack.incident.repository.IncidentCommentRepository;
import com.fintrack.incident.repository.IncidentRepository;
import com.fintrack.incident.service.IncidentCommentService;
import com.fintrack.incident.service.IncidentHistoryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// Implementation du service de gestion des commentaires d'incidents.

@Service
@RequiredArgsConstructor
public class IncidentCommentServiceImpl implements IncidentCommentService {

  private final IncidentCommentRepository commentRepository;
  private final IncidentRepository incidentRepository;
  private final IncidentHistoryService historyService;
  private final NotificationClientService notificationClientService;
  private final com.fintrack.incident.service.AttachmentCleanupService attachmentCleanup;

  @Override
  @Transactional
  public void deleteCommentIfEmpty(UUID incidentId, UUID commentId, UUID userId) {
    loadEditableComment(incidentId, commentId, userId);
    attachmentCleanup.sealEmptyComment(incidentId, commentId);
    deleteComment(incidentId, commentId, userId);
  }

  @Override
  @Transactional(readOnly = true)
  public void assertCanModifyAttachments(UUID incidentId, UUID commentId, UUID userId) {
    loadEditableComment(incidentId, commentId, userId);
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les commentaires incident par incident identifiant.
  public List<IncidentComment> findByIncidentId(UUID incidentId) {
    return commentRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les commentaires incident pour public by incident identifiant.
  public List<IncidentComment> findPublicByIncidentId(UUID incidentId) {
    return commentRepository.findByIncidentIdAndIsInternalOrderByCreatedAtAsc(
      incidentId,
      false
    );
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les commentaires incident par identifiant.
  public IncidentComment findById(UUID id) {
    return commentRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Commentaire introuvable avec l'identifiant : " + id
        )
      );
  }

  @Override
  @Transactional
  // Cree un element du domaine incident comment apres validation metier.
  public IncidentComment addComment(
    UUID incidentId,
    UUID userId,
    String content,
    boolean isInternal
  ) {
    return addComment(incidentId, userId, content, isInternal, null);
  }

  // Cree un element du domaine incident comment apres validation metier.

  @Override
  @Transactional
  public IncidentComment addComment(
    UUID incidentId,
    UUID userId,
    String content,
    boolean isInternal,
    UUID parentCommentId
  ) {
    Incident incident = incidentRepository
      .findById(incidentId)
      .orElseThrow(() ->
        new EntityNotFoundException(
          "Incident introuvable avec l'identifiant : " + incidentId
        )
      );

    IncidentComment comment = new IncidentComment();
    comment.setIncident(incident);
    comment.setUserId(userId);
    comment.setContent(content);
    comment.setInternal(isInternal);
    if (parentCommentId != null) {
      IncidentComment parent = commentRepository
        .findById(parentCommentId)
        .orElseThrow(() ->
          new EntityNotFoundException(
            "Commentaire parent introuvable avec l'identifiant : " +
              parentCommentId
          )
        );
      if (!incidentId.equals(parent.getIncident().getId())) {
        throw new EntityNotFoundException(
          "Commentaire parent introuvable pour l incident : " + incidentId
        );
      }
      comment.setParentComment(parent);
    }

    IncidentComment saved = commentRepository.save(comment);

    historyService.record(
      incident,
      userId,
      ActionType.COMMENT,
      null,
      content,
      isInternal ? "[internal]" : parentCommentId != null ? "[reply]" : null
    );

    if (saved.getParentComment() != null) {
      notifyAfterCommit(
        () ->
          notificationClientService.notifyIncidentCommentReply(
            incident,
            userId,
            saved.getParentComment().getUserId()
          )
      );
    } else {
      notifyAfterCommit(
        () -> notificationClientService.notifyIncidentComment(incident, userId)
      );
    }

    return saved;
  }

  @Override
  @Transactional
  public IncidentComment updateComment(
    UUID incidentId,
    UUID commentId,
    UUID userId,
    String content
  ) {
    IncidentComment comment = loadEditableComment(incidentId, commentId, userId);

    comment.setContent(content);
    IncidentComment saved = commentRepository.save(comment);

    historyService.record(
      comment.getIncident(),
      userId,
      ActionType.COMMENT,
      null,
      content,
      "[edited]"
    );

    return saved;
  }

  @Override
  @Transactional
  public void deleteComment(UUID incidentId, UUID commentId, UUID userId) {
    IncidentComment comment = loadEditableComment(incidentId, commentId, userId);
    String previousContent = comment.getContent();

    commentRepository.delete(comment);
    attachmentCleanup.enqueue(incidentId, commentId);

    historyService.record(
      comment.getIncident(),
      userId,
      ActionType.COMMENT,
      previousContent,
      null,
      "[deleted]"
    );
  }

  // Charge un commentaire et verifie que l'appelant peut encore le modifier.
  private IncidentComment loadEditableComment(
    UUID incidentId,
    UUID commentId,
    UUID userId
  ) {
    IncidentComment comment = commentRepository
      .findById(commentId)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.INCIDENT_COMMENT_NOT_FOUND,
          ErrorCode.INCIDENT_COMMENT_NOT_FOUND.getMessageKey()
        )
      );

    if (!incidentId.equals(comment.getIncident().getId())) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_COMMENT_WRONG_INCIDENT,
        ErrorCode.INCIDENT_COMMENT_WRONG_INCIDENT.getMessageKey()
      );
    }

    if (!userId.equals(comment.getUserId())) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_COMMENT_NOT_AUTHOR,
        ErrorCode.INCIDENT_COMMENT_NOT_AUTHOR.getMessageKey()
      );
    }

    if (commentRepository.existsByParentCommentId(comment.getId())) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_COMMENT_HAS_REPLY,
        ErrorCode.INCIDENT_COMMENT_HAS_REPLY.getMessageKey()
      );
    }

    if (
      commentRepository.existsByIncidentIdAndCreatedAtAfterAndUserIdNot(
        comment.getIncident().getId(),
        comment.getCreatedAt(),
        userId
      )
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_COMMENT_SUPERSEDED,
        ErrorCode.INCIDENT_COMMENT_SUPERSEDED.getMessageKey()
      );
    }

    return comment;
  }

  // Diffuse la notification seulement apres le commit : le push WebSocket ne doit
  // partir qu'une fois le commentaire persiste, sinon le refetch declenche cote
  // frontend lit une liste de commentaires encore incomplete (course notify/commit).
  private void notifyAfterCommit(Runnable notification) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      notification.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
      new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          notification.run();
        }
      }
    );
  }
}
