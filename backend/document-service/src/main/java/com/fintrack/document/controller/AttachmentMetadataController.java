// Controleur REST : expose les operations HTTP liees a attachment metadata.

package com.fintrack.document.controller;

import com.fintrack.document.client.incident.IncidentServiceClientService;
import com.fintrack.document.constant.ApiConstants;
import com.fintrack.document.exception.FileStorageException;
import com.fintrack.document.model.dto.request.AttachmentMetadataRequest;
import com.fintrack.document.model.dto.response.AttachmentMetadataResponse;
import com.fintrack.document.model.mapper.AttachmentMetadataMapper;
import com.fintrack.document.security.JwtUtils;
import com.fintrack.document.service.AttachmentMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// Controleur REST exposant les endpoints de gestion des pieces jointes et de leurs metadonnees.
@RestController
@RequestMapping(ApiConstants.Endpoints.ATTACHMENTS)
@Tag(
  name = "Attachment Metadata",
  description = "Endpoints for managing attachment metadata"
)
@RequiredArgsConstructor
public class AttachmentMetadataController {

  private final AttachmentMetadataService attachmentMetadataService;
  private final AttachmentMetadataMapper attachmentMetadataMapper;
  private final IncidentServiceClientService incidentServiceClientService;
  private final JwtUtils jwtUtils;

  // Liste paginee de toutes les pieces jointes.
  @GetMapping
  @PreAuthorize("hasAuthority('INCIDENT_VIEW_ALL')")
  @Operation(summary = "Get all attachments with pagination")
  public ResponseEntity<Page<AttachmentMetadataResponse>> getAllAttachments(
    Pageable pageable
  ) {
    return ResponseEntity.ok(
      attachmentMetadataService
        .findAll(pageable)
        .map(attachmentMetadataMapper::toResponse)
    );
  }

  // Liste complete de toutes les pieces jointes sans pagination.
  @GetMapping("/all")
  @PreAuthorize("hasAuthority('INCIDENT_VIEW_ALL')")
  @Operation(summary = "Get all attachments as a list")
  public ResponseEntity<
    List<AttachmentMetadataResponse>
  > getAllAttachmentsList() {
    return ResponseEntity.ok(
      attachmentMetadataService
        .findAll()
        .stream()
        .map(attachmentMetadataMapper::toResponse)
        .toList()
    );
  }

  // Recupere une piece jointe par son identifiant, apres verification d'acces a l'incident.
  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get attachment by ID")
  public ResponseEntity<AttachmentMetadataResponse> getAttachmentById(
    @PathVariable UUID id
  ) {
    var attachment = attachmentMetadataService.findById(id);
    incidentServiceClientService.assertIncidentAccessible(
      attachment.getIncidentId()
    );
    return ResponseEntity.ok(attachmentMetadataMapper.toResponse(attachment));
  }

  // Liste les pieces jointes d'un incident, apres verification d'acces a celui-ci.
  @GetMapping("/incident/{incidentId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get all attachments for a given incident")
  public ResponseEntity<
    List<AttachmentMetadataResponse>
  > getAttachmentsByIncidentId(@PathVariable UUID incidentId) {
    incidentServiceClientService.assertIncidentAccessible(incidentId);
    return ResponseEntity.ok(
      attachmentMetadataService
        .findByIncidentId(incidentId)
        .stream()
        .map(attachmentMetadataMapper::toResponse)
        .toList()
    );
  }

  // Liste les pieces jointes televersees par un utilisateur donne.
  @GetMapping("/uploaded-by/{uploadedBy}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get all attachments uploaded by a given user")
  public ResponseEntity<
    List<AttachmentMetadataResponse>
  > getAttachmentsByUploadedBy(
    @PathVariable UUID uploadedBy,
    HttpServletRequest request
  ) {
    UUID currentUserId = extractUserIdFromRequest(request);
    assertCanAccessUploader(uploadedBy, currentUserId);
    return ResponseEntity.ok(
      attachmentMetadataService
        .findByUploadedBy(uploadedBy)
        .stream()
        .filter(attachment -> isIncidentAccessible(attachment.getIncidentId()))
        .map(attachmentMetadataMapper::toResponse)
        .toList()
    );
  }

  // Enregistre les metadonnees d'une piece jointe.
  @PostMapping
  @PreAuthorize("denyAll()")
  @Operation(summary = "Register attachment metadata")
  public ResponseEntity<AttachmentMetadataResponse> createAttachment(
    @Valid @RequestBody AttachmentMetadataRequest request,
    HttpServletRequest httpRequest
  ) {
    UUID uploadedBy = extractUserIdFromRequest(httpRequest);
    incidentServiceClientService.assertIncidentAccessible(
      request.getIncidentId()
    );
    var attachment = attachmentMetadataMapper.toEntity(request);
    attachment.setUploadedBy(uploadedBy);
    return new ResponseEntity<>(
      attachmentMetadataMapper.toResponse(
        attachmentMetadataService.create(attachment)
      ),
      HttpStatus.CREATED
    );
  }

  // Met a jour les metadonnees d'une piece jointe existante.
  @PutMapping("/{id}")
  @PreAuthorize("denyAll()")
  @Operation(summary = "Update attachment metadata")
  public ResponseEntity<AttachmentMetadataResponse> updateAttachment(
    @PathVariable UUID id,
    @Valid @RequestBody AttachmentMetadataRequest request
  ) {
    var existing = attachmentMetadataService.findById(id);
    incidentServiceClientService.assertIncidentAccessible(
      existing.getIncidentId()
    );
    incidentServiceClientService.assertIncidentAccessible(
      request.getIncidentId()
    );
    return ResponseEntity.ok(
      attachmentMetadataMapper.toResponse(
        attachmentMetadataService.update(
          id,
          attachmentMetadataMapper.toEntity(request)
        )
      )
    );
  }

  // Supprime une piece jointe et son fichier physique.
  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Delete attachment metadata")
  public ResponseEntity<Void> deleteAttachment(
    @PathVariable UUID id,
    HttpServletRequest request
  ) {
    var attachment = attachmentMetadataService.findById(id);
    UUID currentUserId = extractUserIdFromRequest(request);
    if (isUserAvatar(attachment.getIncidentId(), attachment.getCategory())) {
      assertOwnAvatar(attachment.getUploadedBy(), currentUserId);
    } else {
      incidentServiceClientService.assertAttachmentAllowed(
        attachment.getIncidentId(),
        attachment.getCategory() == null ? "INCIDENT" : attachment.getCategory(),
        attachment.getCommentId()
      );
    }
    attachmentMetadataService.delete(id);
    return ResponseEntity.noContent().build();
  }

  // Supprime toutes les pieces jointes rattachees a un incident.
  @DeleteMapping("/incident/{incidentId}")
  @PreAuthorize("denyAll()")
  @Operation(summary = "Delete all attachment metadata for a given incident")
  public ResponseEntity<Void> deleteAttachmentsByIncidentId(
    @PathVariable UUID incidentId
  ) {
    incidentServiceClientService.assertIncidentAccessible(incidentId);
    attachmentMetadataService.deleteByIncidentId(incidentId);
    return ResponseEntity.noContent().build();
  }

  // Televerse un fichier et enregistre ses metadonnees pour un incident donne.
  @PostMapping(
    value = "/upload",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Upload a file and register its metadata for a given incident"
  )
  public ResponseEntity<AttachmentMetadataResponse> uploadAttachment(
    @RequestPart("file") MultipartFile file,
    @RequestParam("incidentId") UUID incidentId,
    @RequestParam(value = "commentId", required = false) UUID commentId,
    @RequestParam(value = "category", required = false) String category,
    @RequestParam(value = "uploadId", required = false) UUID uploadId,
    HttpServletRequest request
  ) {
    UUID uploadedBy = extractUserIdFromRequest(request);

    String resolvedCategory = commentId != null ? "COMMENT"
      : category == null ? "INCIDENT" : category.toUpperCase(java.util.Locale.ROOT);
    incidentServiceClientService.assertAttachmentAllowed(incidentId, resolvedCategory, commentId);
    try {
      return new ResponseEntity<>(
        attachmentMetadataMapper.toResponse(
          attachmentMetadataService.upload(
            attachmentMetadataMapper.toUpload(
              file,
              incidentId,
              commentId,
              resolvedCategory,
              uploadedBy
            ).withUploadId(uploadId)
          )
        ),
        HttpStatus.CREATED
      );
    } catch (IOException ex) {
      throw new FileStorageException(
        "Échec de lecture du fichier téléversé : " + ex.getMessage()
      );
    }
  }

  @PostMapping(
    value = "/avatar",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Upload the authenticated user's avatar")
  // Realise l'intention metier upload avatar.
  public ResponseEntity<AttachmentMetadataResponse> uploadAvatar(
    @RequestPart("file") MultipartFile file,
    @RequestParam(value = "uploadId", required = false) UUID uploadId,
    HttpServletRequest request
  ) {
    UUID uploadedBy = extractUserIdFromRequest(request);
    try {
      return new ResponseEntity<>(
        attachmentMetadataMapper.toResponse(
          attachmentMetadataService.uploadUserAvatar(
            attachmentMetadataMapper.toUpload(file, null, null, null, uploadedBy).withUploadId(uploadId)
          )
        ),
        HttpStatus.CREATED
      );
    } catch (IOException ex) {
      throw new FileStorageException(
        "Échec de lecture de l'avatar téléversé : " + ex.getMessage()
      );
    }
  }

  // Prepare le telechargement de piece jointe autorise.

  @GetMapping("/{id}/download")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Download a file by attachment ID")
  public ResponseEntity<Resource> downloadAttachment(@PathVariable UUID id) {
    AttachmentMetadataResponse metadata = attachmentMetadataMapper.toResponse(
      attachmentMetadataService.findById(id)
    );
    if (metadata.getIncidentId() != null) {
      incidentServiceClientService.assertIncidentAccessible(
        metadata.getIncidentId()
      );
    }
    Resource resource = attachmentMetadataService.download(id);

    String contentType = metadata.getMimeType();
    if (contentType == null || !java.util.Set.of(
      "application/pdf", "image/png", "image/jpeg", "image/gif", "image/bmp", "image/webp", "image/heic",
      "application/msword", "application/vnd.ms-excel",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "application/vnd.oasis.opendocument.text", "application/vnd.oasis.opendocument.spreadsheet",
      "application/zip", "text/plain", "text/csv", "application/rtf"
    ).contains(contentType)) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(contentType))
      .header("X-Content-Type-Options", "nosniff")
      .header("Content-Security-Policy", "sandbox; default-src 'none'")
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        org.springframework.http.ContentDisposition.attachment()
          .filename(metadata.getFilename(), java.nio.charset.StandardCharsets.UTF_8).build().toString()
      )
      .body(resource);
  }

  /**
   * Extrait l'UUID de l'utilisateur connecté depuis le token JWT de la requête.
   * Le subject du JWT est l'ID utilisateur (UUID).
   */
  // Extrait user id from requete.
  private UUID extractUserIdFromRequest(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
      throw new IllegalStateException(
        "En-tete Authorization manquant ou invalide"
      );
    }
    String jwt = authHeader.substring(7);
    String userId = jwtUtils.getUserIdFromJwtToken(jwt);
    return UUID.fromString(userId);
  }

  // Verifie que les regles metier autorisent l operation sur piece jointe.

  private void assertCanAccessUploader(UUID uploadedBy, UUID currentUserId) {
    if (
      Objects.equals(uploadedBy, currentUserId) ||
      hasAuthority("INCIDENT_VIEW_ALL")
    ) {
      return;
    }
    throw new AccessDeniedException(
      "Vous n'avez pas la permission d'acceder aux pieces jointes d'un autre utilisateur"
    );
  }

  // Verifie si incident accessible.

  private boolean isIncidentAccessible(UUID incidentId) {
    if (incidentId == null) {
      return true;
    }
    try {
      incidentServiceClientService.assertIncidentAccessible(incidentId);
      return true;
    } catch (RuntimeException ex) {
      return false;
    }
  }

  // Verifie que les regles metier autorisent l operation sur piece jointe.

  private boolean hasAuthority(String authority) {
    Authentication authentication =
      SecurityContextHolder.getContext().getAuthentication();
    return (
      authentication != null &&
      authentication
        .getAuthorities()
        .stream()
        .anyMatch(granted -> authority.equalsIgnoreCase(granted.getAuthority()))
    );
  }

  // Verifie si user avatar.

  private boolean isUserAvatar(UUID incidentId, String category) {
    return incidentId == null && "USER_AVATAR".equalsIgnoreCase(category);
  }

  // Verifie que les regles metier autorisent l operation sur piece jointe.

  private void assertOwnAvatar(UUID uploadedBy, UUID currentUserId) {
    if (Objects.equals(uploadedBy, currentUserId)) {
      return;
    }
    throw new AccessDeniedException(
      "Vous n'avez pas la permission de supprimer l'avatar d'un autre utilisateur"
    );
  }
}
