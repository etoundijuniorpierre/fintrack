package com.fintrack.document.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fintrack.document.model.entity.*;
import com.fintrack.document.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;

class AttachmentCleanupTest {
  final AttachmentScopeLock locks = mock(AttachmentScopeLock.class);
  final AttachmentMetadataRepository metadata = mock(AttachmentMetadataRepository.class);
  final AttachmentStorageCleanupRepository jobs = mock(AttachmentStorageCleanupRepository.class);
  final AttachmentTombstoneRepository tombstones = mock(AttachmentTombstoneRepository.class);
  final FileStorageService storage = mock(FileStorageService.class);
  final AttachmentStorageCleanupService cleanup = new AttachmentStorageCleanupService(jobs, metadata, storage, locks);

  @Test void referencedObjectIsNeverPhysicallyDeleted() {
    var job = new AttachmentStorageCleanup(UUID.randomUUID(), "key", LocalDateTime.now().minusMinutes(1), 0);
    when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
    when(metadata.existsByStoragePath("key")).thenReturn(true);
    cleanup.process(job.getId());
    verifyNoInteractions(storage);
    verify(jobs).delete(job);
  }
  @Test void storageFailureSchedulesRetry() {
    var job = new AttachmentStorageCleanup(UUID.randomUUID(), "key", LocalDateTime.now().minusMinutes(1), 0);
    when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
    doThrow(new IllegalStateException("offline")).when(storage).delete("key");
    cleanup.process(job.getId());
    assertThat(job.getAttempts()).isEqualTo(1);
    assertThat(job.getRetryAfter()).isAfter(LocalDateTime.now());
    verify(jobs, never()).delete(any());
  }
  @Test void automaticCleanupDoesNotDeleteAnAttachmentWhoseResponseWasLost() {
    var incident = UUID.randomUUID(); var comment = UUID.randomUUID();
    when(metadata.findByIncidentIdAndCommentId(incident, comment)).thenReturn(List.of(new AttachmentMetadata()));
    var deletion = new AttachmentDeletionService(locks, tombstones, metadata, cleanup);
    assertThat(deletion.deleteScope(incident, comment, true)).isFalse();
    verifyNoInteractions(tombstones, jobs, storage);
    verify(metadata, never()).deleteAll(anyIterable());
  }
  @Test void emptyCommentIsSealedBeforeDeletion() {
    var incident = UUID.randomUUID(); var comment = UUID.randomUUID();
    var deletion = new AttachmentDeletionService(locks, tombstones, metadata, cleanup);
    assertThat(deletion.deleteScope(incident, comment, true)).isTrue();
    verify(tombstones).saveAndFlush(new AttachmentTombstone("comment:" + comment));
  }
}
