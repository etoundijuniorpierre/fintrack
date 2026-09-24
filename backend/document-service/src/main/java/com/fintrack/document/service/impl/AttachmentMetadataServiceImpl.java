package com.fintrack.document.service.impl;

import com.fintrack.document.exception.EntityNotFoundException;
import com.fintrack.document.exception.ErrorCode;
import com.fintrack.document.exception.FileStorageException;
import com.fintrack.document.model.entity.AttachmentMetadata;
import com.fintrack.document.model.entity.AttachmentUpload;
import com.fintrack.document.repository.AttachmentMetadataRepository;
import com.fintrack.document.service.*;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentMetadataServiceImpl implements AttachmentMetadataService {
  private final AttachmentMetadataRepository attachmentMetadataRepository;
  private final FileStorageService fileStorageService;
  private final AttachmentUploadService uploads;
  private final AttachmentStorageCleanupService cleanup;
  private final AttachmentScopeLock locks;

  public Page<AttachmentMetadata> findAll(Pageable pageable) { return attachmentMetadataRepository.findAll(pageable); }
  public List<AttachmentMetadata> findAll() { return attachmentMetadataRepository.findAll(); }
  public AttachmentMetadata findById(UUID id) {
    return attachmentMetadataRepository.findById(id).orElseThrow(() ->
      new EntityNotFoundException(ErrorCode.ATTACHMENT_NOT_FOUND, "Piece jointe introuvable : " + id));
  }
  public List<AttachmentMetadata> findByIncidentId(UUID id) { return attachmentMetadataRepository.findByIncidentId(id); }
  public List<AttachmentMetadata> findByUploadedBy(UUID id) { return attachmentMetadataRepository.findByUploadedBy(id); }

  public AttachmentMetadata create(AttachmentMetadata attachment) {
    throw new FileStorageException(ErrorCode.PERMISSION_DENIED, "Utilisez le televersement de fichier");
  }
  public AttachmentMetadata update(UUID id, AttachmentMetadata details) {
    throw new FileStorageException(ErrorCode.PERMISSION_DENIED, "Les metadonnees de stockage sont immuables");
  }

  @Transactional
  public void delete(UUID id) {
    AttachmentMetadata attachment = findById(id);
    locks.lock("incident:" + attachment.getIncidentId());
    cleanup.scheduleDeletion(attachment.getStoragePath());
    attachmentMetadataRepository.deleteById(id);
  }

  public long deleteByIncidentId(UUID incidentId) {
    throw new FileStorageException(ErrorCode.PERMISSION_DENIED, "Utilisez le nettoyage interne authentifie");
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  public AttachmentMetadata upload(AttachmentUpload upload) {
    assertSize(upload);
    return uploads.upload(upload, false);
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  public AttachmentMetadata uploadUserAvatar(AttachmentUpload upload) {
    assertSize(upload);
    return uploads.upload(upload, true);
  }

  private void assertSize(AttachmentUpload upload) {
    if (upload == null || upload.getContent() == null || upload.getSize() <= 0)
      throw new FileStorageException(ErrorCode.INVALID_INPUT, "Le fichier ne doit pas etre vide");
    if (upload.getSize() > 150L * 1024 * 1024)
      throw new FileStorageException(ErrorCode.FILE_SIZE_EXCEEDED, "Fichier trop volumineux");
  }

  public Resource download(UUID id) { return fileStorageService.load(findById(id).getStoragePath()); }
}
