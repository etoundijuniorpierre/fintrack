package com.fintrack.document.service;
import com.fintrack.document.model.entity.AttachmentTombstone;
import com.fintrack.document.repository.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AttachmentDeletionService {
  private final AttachmentScopeLock locks;
  private final AttachmentTombstoneRepository tombstones;
  private final AttachmentMetadataRepository attachments;
  private final AttachmentStorageCleanupService cleanup;

  @Transactional
  public boolean deleteScope(UUID incidentId, UUID commentId, boolean onlyIfEmpty) {
    locks.lock("incident:" + incidentId);
    var files = commentId == null ? attachments.findByIncidentId(incidentId)
      : attachments.findByIncidentIdAndCommentId(incidentId, commentId);
    if (onlyIfEmpty && !files.isEmpty()) return false;
    tombstones.saveAndFlush(new AttachmentTombstone(commentId == null ?
      "incident:" + incidentId : "comment:" + commentId));
    for (var file : files) cleanup.scheduleDeletion(file.getStoragePath());
    attachments.deleteAll(files);
    return true;
  }
}
