package com.fintrack.document.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fintrack.document.exception.FileStorageException;
import com.fintrack.document.model.StoredFile;
import com.fintrack.document.model.entity.*;
import com.fintrack.document.repository.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachmentUploadServiceTest {
  @Spy AttachmentContentValidator validator = new AttachmentContentValidator();
  @Mock AttachmentScopeLock locks;
  @Mock AttachmentMetadataRepository attachments;
  @Mock AttachmentUploadReceiptRepository receipts;
  @Mock AttachmentTombstoneRepository tombstones;
  @Mock AttachmentStorageCleanupService cleanup;
  @Mock FileStorageService storage;
  @InjectMocks AttachmentUploadCommitService commitService;
  AttachmentUploadService service;
  @org.junit.jupiter.api.BeforeEach void setup() {
    service = new AttachmentUploadService(validator, cleanup, commitService);
  }
  final UUID incident = UUID.randomUUID(), user = UUID.randomUUID(), uploadId = UUID.randomUUID();

  AttachmentUpload upload(String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    return AttachmentUpload.builder().originalFilename("a.pdf").contentType("application/pdf").category("COMMENT")
      .content(new ByteArrayInputStream(bytes)).size(bytes.length).uploadedBy(user).incidentId(incident).uploadId(uploadId).build();
  }
  void enableStorage() {
    when(storage.store(any(), anyString())).thenAnswer(invocation -> {
      AttachmentUpload upload = invocation.getArgument(0);
      byte[] bytes = upload.getContent().readAllBytes();
      return StoredFile.builder().size(bytes.length).objectKey(invocation.getArgument(1))
        .checksum(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))).build();
    });
    when(attachments.saveAndFlush(any())).thenAnswer(invocation -> {
      AttachmentMetadata metadata = invocation.getArgument(0);
      metadata.setId(UUID.randomUUID());
      return metadata;
    });
  }
  @Test void lostResponseReplayReturnsSameAttachmentWithoutStoringAgain() {
    enableStorage();
    Map<UUID, AttachmentUploadReceipt> saved = new HashMap<>();
    when(receipts.findById(any())).thenAnswer(i -> Optional.ofNullable(saved.get(i.getArgument(0))));
    when(receipts.save(any())).thenAnswer(i -> {
      AttachmentUploadReceipt receipt = i.getArgument(0); saved.put(receipt.getId(), receipt); return receipt;
    });
    var first = service.upload(upload("%PDF-1.7"), false);
    when(attachments.findById(first.getId())).thenReturn(Optional.of(first));
    assertThat(service.upload(upload("%PDF-1.7"), false)).isSameAs(first);
    verify(storage, times(1)).store(any(), anyString());
    assertThatThrownBy(() -> service.upload(upload("%PDF-1.7 different"), false)).isInstanceOf(FileStorageException.class);
  }
  @Test void deletedTargetCannotReceiveDelayedUpload() {
    when(tombstones.existsById("incident:" + incident)).thenReturn(true);
    assertThatThrownBy(() -> service.upload(upload("%PDF-1.7"), false)).isInstanceOf(FileStorageException.class);
    verifyNoInteractions(storage);
    verify(cleanup, never()).completeUpload(any());
  }
  @Test void incompleteStorageRetainsCleanupJournalAndNoMetadata() {
    when(storage.store(any(), anyString())).thenReturn(StoredFile.builder().size(1).checksum("bad").build());
    assertThatThrownBy(() -> service.upload(upload("%PDF-1.7"), false)).isInstanceOf(FileStorageException.class);
    verify(cleanup).prepareUpload(anyString());
    verify(cleanup, never()).completeUpload(any());
    verify(attachments, never()).saveAndFlush(any());
  }
  @Test void databaseFailureRetainsCleanupJournal() {
    when(storage.store(any(), anyString())).thenAnswer(invocation -> {
      AttachmentUpload upload = invocation.getArgument(0);
      byte[] bytes = upload.getContent().readAllBytes();
      return StoredFile.builder().size(bytes.length)
        .checksum(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))).build();
    });
    when(attachments.saveAndFlush(any())).thenThrow(new IllegalStateException("DB unavailable"));
    assertThatThrownBy(() -> service.upload(upload("%PDF-1.7"), false)).isInstanceOf(IllegalStateException.class);
    verify(cleanup, never()).completeUpload(any());
    verify(receipts, never()).save(any());
  }
}
