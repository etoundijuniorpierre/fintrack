package com.fintrack.incident.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fintrack.incident.client.document.DocumentAttachmentClient;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.model.entity.AttachmentCleanupTask;
import com.fintrack.incident.repository.AttachmentCleanupTaskRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AttachmentCleanupServiceTest {
  final AttachmentCleanupTaskRepository tasks = mock(AttachmentCleanupTaskRepository.class);
  final DocumentAttachmentClient documents = mock(DocumentAttachmentClient.class);
  final AttachmentCleanupService service = new AttachmentCleanupService(tasks, documents);
  @Test void lostReplyKeepsOutboxTaskUntilConfirmed() {
    var task = new AttachmentCleanupTask(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().minusMinutes(1), 0);
    when(tasks.findLocked(task.getId())).thenReturn(Optional.of(task));
    ReflectionTestUtils.setField(service, "token", "secret");
    when(documents.cleanup(task.getIncidentId(), task.getCommentId(), false, "secret")).thenThrow(new IllegalStateException("timeout"));
    service.process(task.getId());
    verify(tasks, never()).delete(any());
    assertThat(task.getAttempts()).isEqualTo(1);
    task.setRetryAfter(LocalDateTime.now().minusMinutes(1));
    doReturn(true).when(documents).cleanup(task.getIncidentId(), task.getCommentId(), false, "secret");
    service.process(task.getId());
    verify(tasks).delete(task);
  }
  @Test void automaticCommentDeletionRequiresConfirmedAbsenceOfAttachments() {
    var incident = UUID.randomUUID(); var comment = UUID.randomUUID();
    assertThatThrownBy(() -> service.sealEmptyComment(incident, comment)).isInstanceOf(BusinessRuleViolationException.class);
    verifyNoInteractions(tasks);
  }
}
