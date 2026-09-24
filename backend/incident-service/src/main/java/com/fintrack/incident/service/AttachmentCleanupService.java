package com.fintrack.incident.service;

import com.fintrack.incident.client.document.DocumentAttachmentClient;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.model.entity.AttachmentCleanupTask;
import com.fintrack.incident.repository.AttachmentCleanupTaskRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor @Slf4j
public class AttachmentCleanupService {
  private final AttachmentCleanupTaskRepository tasks;
  private final DocumentAttachmentClient documents;
  @Value("${fintrack.internal-service.token:}") private String token;

  @Transactional(propagation = Propagation.MANDATORY)
  public void enqueue(UUID incidentId, UUID commentId) {
    tasks.save(new AttachmentCleanupTask(UUID.randomUUID(), incidentId, commentId, LocalDateTime.now(), 0));
  }

  public void sealEmptyComment(UUID incidentId, UUID commentId) {
    if (!Boolean.TRUE.equals(documents.cleanup(incidentId, commentId, true, token))) {
      throw new BusinessRuleViolationException(com.fintrack.incident.exception.ErrorCode.BUSINESS_RULE_VIOLATION,
        "Ce commentaire contient des pieces jointes : suppression automatique annulee");
    }
  }

  @Transactional
  public void process(UUID id) {
    var task = tasks.findLocked(id).orElse(null);
    if (task == null || task.getRetryAfter().isAfter(LocalDateTime.now())) return;
    try {
      if (!Boolean.TRUE.equals(documents.cleanup(task.getIncidentId(), task.getCommentId(), false, token))) {
        throw new IllegalStateException("Nettoyage non confirme");
      }
      tasks.delete(task);
    } catch (RuntimeException ex) {
      task.setAttempts(task.getAttempts() + 1);
      task.setRetryAfter(LocalDateTime.now().plusMinutes(Math.min(60L, 2L * task.getAttempts())));
      tasks.save(task);
      log.warn("Nettoyage des pieces jointes a reprendre : {}", id);
    }
  }
}
