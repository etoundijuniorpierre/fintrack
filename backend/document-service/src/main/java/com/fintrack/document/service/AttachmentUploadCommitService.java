package com.fintrack.document.service;
import com.fintrack.document.exception.*;
import com.fintrack.document.model.entity.*;
import com.fintrack.document.repository.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AttachmentUploadCommitService {
  private final AttachmentScopeLock locks;
  private final AttachmentMetadataRepository attachments;
  private final AttachmentUploadReceiptRepository receipts;
  private final AttachmentTombstoneRepository tombstones;
  private final AttachmentStorageCleanupService cleanup;
  private final FileStorageService storage;

  @Transactional
  public AttachmentMetadata commit(AttachmentUpload upload, boolean avatar,
      com.fintrack.document.model.readmodel.ValidatedAttachment validated, String key, UUID cleanupId) {
    UUID receiptId = UUID.nameUUIDFromBytes((upload.getUploadedBy() + ":" + upload.getUploadId()).getBytes(StandardCharsets.UTF_8));
    try {
      locks.lock("receipt:" + receiptId);
      if (!avatar) locks.lock("incident:" + upload.getIncidentId());
      String context = upload.getIncidentId() + ":" + upload.getCommentId() + ":" +
        (avatar ? "USER_AVATAR" : upload.getCategory()) + ":" + upload.getOriginalFilename() + ":" + validated.getChecksum();
      String fingerprint = java.util.HexFormat.of().formatHex(
        java.security.MessageDigest.getInstance("SHA-256").digest(context.getBytes(StandardCharsets.UTF_8)));
      var receipt = receipts.findById(receiptId).orElse(null);
      if (receipt != null) {
        if (!fingerprint.equals(receipt.getFingerprint())) {
          throw new FileStorageException(ErrorCode.DUPLICATE_RESOURCE, "Identifiant d'envoi reutilise pour un autre fichier");
        }
        cleanup.completeUpload(cleanupId);
        return attachments.findById(receipt.getAttachmentId()).orElseThrow(() ->
          new FileStorageException(ErrorCode.DUPLICATE_RESOURCE, "Cet envoi a deja ete supprime"));
      }
      if (!avatar && (tombstones.existsById("incident:" + upload.getIncidentId()) ||
          (upload.getCommentId() != null && tombstones.existsById("comment:" + upload.getCommentId())))) {
        throw new FileStorageException(ErrorCode.DUPLICATE_RESOURCE, "La cible de cet envoi a ete supprimee");
      }
      String filename = Objects.toString(upload.getOriginalFilename(), "file")
        .replace('\\', '/');
      filename = filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._-]", "_");
      if (filename.isBlank() || filename.length() > 200) throw new FileStorageException(ErrorCode.INVALID_INPUT, "Nom de fichier invalide");
      locks.lock("cleanup:" + cleanupId);
      cleanup.assertUploadPending(cleanupId);
      upload.setSize(validated.getSize());
      upload.setContentType(validated.getMimeType());
      try (var input = Files.newInputStream(validated.getPath())) {
        upload.setContent(input);
        var stored = storage.store(upload, key);
        if (stored.getSize() != validated.getSize() || !validated.getChecksum().equals(stored.getChecksum())) {
          throw new FileStorageException("Le stockage du fichier est incomplet");
        }
      }
      var metadata = new AttachmentMetadata();
      metadata.setIncidentId(avatar ? null : upload.getIncidentId());
      metadata.setCommentId(avatar ? null : upload.getCommentId());
      metadata.setCategory(avatar ? "USER_AVATAR" : upload.getCategory());
      metadata.setFilename(filename);
      metadata.setStoragePath(key);
      metadata.setFileSize(validated.getSize());
      metadata.setMimeType(validated.getMimeType());
      metadata.setUploadedBy(upload.getUploadedBy());
      metadata.setUploadedAt(LocalDateTime.now());
      metadata = attachments.saveAndFlush(metadata);
      receipts.save(new AttachmentUploadReceipt(receiptId, metadata.getId(), fingerprint));
      cleanup.completeUpload(cleanupId);
      return metadata;
    } catch (java.io.IOException | java.security.NoSuchAlgorithmException ex) {
      throw new FileStorageException("Echec de preparation du fichier", ex);
    }
  }
}
