package com.fintrack.document.service;
import com.fintrack.document.model.entity.AttachmentStorageCleanup;
import com.fintrack.document.repository.AttachmentStorageCleanupRepository;
import com.fintrack.document.repository.AttachmentMetadataRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor @Slf4j
public class AttachmentStorageCleanupService {
  private final AttachmentStorageCleanupRepository jobs;
  private final AttachmentMetadataRepository attachments;
  private final FileStorageService storage;
  private final AttachmentScopeLock locks;

  // Ce journal survit au rollback du televersement, y compris en cas d'arret du processus.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public UUID prepareUpload(String objectKey) {
    var task = new AttachmentStorageCleanup(UUID.randomUUID(), objectKey, LocalDateTime.now().plusHours(1), 0);
    jobs.saveAndFlush(task);
    return task.getId();
  }

  @Transactional
  public void completeUpload(UUID jobId) { jobs.deleteById(jobId); }

  @Transactional(readOnly = true)
  public void assertUploadPending(UUID jobId) {
    if (!jobs.existsById(jobId)) {
      throw new com.fintrack.document.exception.FileStorageException("Envoi expire avant stockage : veuillez reessayer");
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void scheduleDeletion(String objectKey) {
    jobs.save(new AttachmentStorageCleanup(UUID.randomUUID(), objectKey, LocalDateTime.now(), 0));
  }

  @Transactional
  public void process(UUID jobId) {
    locks.lock("cleanup:" + jobId);
    var task = jobs.findById(jobId).orElse(null);
    if (task == null || task.getRetryAfter().isAfter(LocalDateTime.now())) return;
    // Ne jamais detruire un fichier reference, meme apres une reprise ou une migration.
    if (attachments.existsByStoragePath(task.getObjectKey())) {
      jobs.delete(task);
      return;
    }
    try {
      storage.delete(task.getObjectKey());
      jobs.delete(task);
    } catch (RuntimeException ex) {
      task.setAttempts(task.getAttempts() + 1);
      task.setRetryAfter(LocalDateTime.now().plusMinutes(Math.min(60, task.getAttempts() * 2L)));
      jobs.save(task);
      log.warn("Nettoyage de fichier reporte, tache {}", jobId, ex);
    }
  }
}
