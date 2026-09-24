package com.fintrack.document.service;

import com.fintrack.document.exception.*;
import com.fintrack.document.model.entity.*;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class AttachmentUploadService {
  private final AttachmentContentValidator validator;
  private final AttachmentStorageCleanupService cleanup;
  private final AttachmentUploadCommitService commitService;

  // Le journal est cree avant la transaction principale : pas de seconde connexion
  // a obtenir alors que tous les transferts detiennent deja leur verrou.
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public AttachmentMetadata upload(AttachmentUpload upload, boolean avatar) {
    if (upload == null || upload.getUploadedBy() == null)
      throw new FileStorageException(ErrorCode.INVALID_INPUT, "Utilisateur requis");
    if (!avatar && upload.getIncidentId() == null)
      throw new FileStorageException(ErrorCode.INVALID_INPUT, "Incident requis");
    if (upload.getUploadId() == null) upload.setUploadId(UUID.randomUUID());
    try (var validated = validator.validate(upload, avatar)) {
      String filename = Objects.toString(upload.getOriginalFilename(), "file").replace('\\', '/');
      filename = filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._-]", "_");
      if (filename.isBlank() || filename.length() > 200)
        throw new FileStorageException(ErrorCode.INVALID_INPUT, "Nom de fichier invalide");
      String key = (avatar ? "avatars/" + upload.getUploadedBy() : "incidents/" + upload.getIncidentId())
        + "/" + UUID.randomUUID() + "_" + filename;
      UUID cleanupId = cleanup.prepareUpload(key);
      return commitService.commit(upload, avatar, validated, key, cleanupId);
    } catch (IOException ex) {
      throw new FileStorageException("Echec de preparation du fichier", ex);
    }
  }
}
