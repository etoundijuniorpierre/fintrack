// Mapper : convertit les metadonnees de piece jointe entre entite, requete et reponse.

package com.fintrack.document.model.mapper;

import com.fintrack.document.client.incident.IncidentServiceClientService;
import com.fintrack.document.client.user.UserServiceClientService;
import com.fintrack.document.model.dto.request.AttachmentMetadataRequest;
import com.fintrack.document.model.dto.response.AttachmentMetadataResponse;
import com.fintrack.document.model.dto.response.IncidentSummaryResponse;
import com.fintrack.document.model.dto.response.UserSummaryResponse;
import com.fintrack.document.model.entity.AttachmentMetadata;
import com.fintrack.document.model.entity.AttachmentUpload;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

// Assure les conversions des metadonnees de piece jointe.
@Component
@RequiredArgsConstructor
public class AttachmentMetadataMapper {

  private final UserServiceClientService userServiceClientService;

  private final IncidentServiceClientService incidentServiceClientService;

  // Prepare une entite persistable depuis la requete de metadonnees.
  public AttachmentMetadata toEntity(AttachmentMetadataRequest request) {
    if (request == null) {
      return null;
    }
    AttachmentMetadata entity = new AttachmentMetadata();
    entity.setIncidentId(request.getIncidentId());
    entity.setFilename(request.getFilename());
    entity.setStoragePath(request.getStoragePath());
    entity.setFileSize(request.getFileSize());
    entity.setMimeType(request.getMimeType());
    entity.setUploadedBy(request.getUploadedBy());
    entity.setUploadedAt(request.getUploadedAt());
    return entity;
  }

  // Compose la reponse enrichie exposee par l'API documentaire.
  public AttachmentMetadataResponse toResponse(AttachmentMetadata entity) {
    if (entity == null) {
      return null;
    }
    AttachmentMetadataResponse response = new AttachmentMetadataResponse();
    response.setId(entity.getId());
    response.setCreatedAt(entity.getCreatedAt());
    response.setUpdatedAt(entity.getUpdatedAt());
    response.setModifiedBy(entity.getModifiedBy());
    response.setIncidentId(entity.getIncidentId());
    response.setCommentId(entity.getCommentId());
    response.setIncident(uuidToIncidentSummary(entity.getIncidentId()));
    response.setCategory(entity.getCategory());
    response.setFilename(entity.getFilename());
    response.setStoragePath(entity.getStoragePath());
    response.setFileSize(entity.getFileSize());
    response.setMimeType(entity.getMimeType());
    response.setUploadedById(entity.getUploadedBy());
    response.setUploadedBy(uuidToUserSummary(entity.getUploadedBy()));
    response.setUploadedAt(entity.getUploadedAt());
    return response;
  }

  // Prepare les donnees de televersement a partir du fichier recu.
  public AttachmentUpload toUpload(
    MultipartFile file,
    UUID incidentId,
    UUID commentId,
    String category,
    UUID uploadedBy
  ) throws IOException {
    if (file == null) {
      return AttachmentUpload.builder()
        .incidentId(incidentId)
        .commentId(commentId)
        .category(category)
        .uploadedBy(uploadedBy)
        .build();
    }

    return AttachmentUpload.builder()
      .originalFilename(file.getOriginalFilename())
      .contentType(file.getContentType())
      .size(file.getSize())
      .content(file.getInputStream())
      .incidentId(incidentId)
      .commentId(commentId)
      .category(category)
      .uploadedBy(uploadedBy)
      .build();
  }

  // Convertit un identifiant en resume utilisateur via le service utilisateur.
  protected UserSummaryResponse uuidToUserSummary(UUID id) {
    if (id == null) return null;
    return userServiceClientService.resolveUser(id);
  }

  // Convertit un identifiant en resume d'incident via le service incident.
  protected IncidentSummaryResponse uuidToIncidentSummary(UUID id) {
    if (id == null) return null;
    return incidentServiceClientService.resolveIncident(id);
  }
}
