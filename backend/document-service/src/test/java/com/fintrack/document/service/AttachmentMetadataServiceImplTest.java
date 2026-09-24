// Tests du service de pieces jointes : valide persistance, stockage et limites.

package com.fintrack.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fintrack.document.exception.EntityNotFoundException;
import com.fintrack.document.exception.ErrorCode;
import com.fintrack.document.exception.FileStorageException;
import com.fintrack.document.model.entity.AttachmentUpload;
import com.fintrack.document.model.entity.AttachmentMetadata;
import com.fintrack.document.repository.AttachmentMetadataRepository;
import com.fintrack.document.service.impl.AttachmentMetadataServiceImpl;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
// Verifie les regles metier appliquees aux metadonnees et fichiers joints.
class AttachmentMetadataServiceImplTest {

  @Mock
  private AttachmentMetadataRepository attachmentMetadataRepository;

  @Mock
  private FileStorageService fileStorageService;

  @Mock private AttachmentUploadService uploads;
  @Mock private AttachmentStorageCleanupService cleanup;
  @Mock private AttachmentScopeLock locks;

  @InjectMocks
  private AttachmentMetadataServiceImpl attachmentMetadataService;

  private AttachmentMetadata testAttachment;
  private UUID testId;
  private UUID incidentId;
  private UUID uploadedBy;

  @BeforeEach
  void setUp() {
    testId = UUID.randomUUID();
    incidentId = UUID.randomUUID();
    uploadedBy = UUID.randomUUID();

    testAttachment = new AttachmentMetadata();
    testAttachment.setId(testId);
    testAttachment.setIncidentId(incidentId);
    testAttachment.setFilename("test.pdf");
    testAttachment.setStoragePath("uploads/test.pdf");
    testAttachment.setFileSize(1024L);
    testAttachment.setMimeType("application/pdf");
    testAttachment.setUploadedBy(uploadedBy);
    testAttachment.setUploadedAt(LocalDateTime.now());
  }

  // ── findAll (pageable) ────────────────────────────────────────────────────

  @Test
  @DisplayName("findAll(pageable) - Returns page of attachments")
  void findAll_Pageable_ReturnsPage() {
    PageRequest pageable = PageRequest.of(0, 10);
    Page<AttachmentMetadata> page = new PageImpl<>(
      List.of(testAttachment),
      pageable,
      1
    );
    when(attachmentMetadataRepository.findAll(pageable)).thenReturn(page);

    Page<AttachmentMetadata> result = attachmentMetadataService.findAll(
      pageable
    );

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(testId);
  }

  // ── findAll (list) ────────────────────────────────────────────────────────

  @Test
  @DisplayName("findAll() - Returns list of all attachments")
  void findAll_ReturnsList() {
    when(attachmentMetadataRepository.findAll()).thenReturn(
      List.of(testAttachment)
    );

    List<AttachmentMetadata> result = attachmentMetadataService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(testId);
  }

  // ── findById ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("findById - Returns attachment when found")
  void findById_Found_ReturnsAttachment() {
    when(attachmentMetadataRepository.findById(testId)).thenReturn(
      Optional.of(testAttachment)
    );

    AttachmentMetadata result = attachmentMetadataService.findById(testId);

    assertThat(result.getId()).isEqualTo(testId);
    assertThat(result.getFilename()).isEqualTo("test.pdf");
  }

  @Test
  @DisplayName("findById - Throws EntityNotFoundException when not found")
  void findById_NotFound_ThrowsException() {
    UUID unknownId = UUID.randomUUID();
    when(attachmentMetadataRepository.findById(unknownId)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      attachmentMetadataService.findById(unknownId)
    ).isInstanceOf(EntityNotFoundException.class);
  }

  // ── findByIncidentId ──────────────────────────────────────────────────────

  @Test
  @DisplayName("findByIncidentId - Returns attachments for incident")
  void findByIncidentId_ReturnsAttachments() {
    when(attachmentMetadataRepository.findByIncidentId(incidentId)).thenReturn(
      List.of(testAttachment)
    );

    List<AttachmentMetadata> result =
      attachmentMetadataService.findByIncidentId(incidentId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getIncidentId()).isEqualTo(incidentId);
  }

  @Test
  @DisplayName("findByIncidentId - Returns empty list when no attachments")
  void findByIncidentId_NoMatch_ReturnsEmpty() {
    when(attachmentMetadataRepository.findByIncidentId(any())).thenReturn(
      List.of()
    );

    List<AttachmentMetadata> result =
      attachmentMetadataService.findByIncidentId(UUID.randomUUID());

    assertThat(result).isEmpty();
  }

  // ── findByUploadedBy ──────────────────────────────────────────────────────

  @Test
  @DisplayName("findByUploadedBy - Returns attachments uploaded by user")
  void findByUploadedBy_ReturnsAttachments() {
    when(attachmentMetadataRepository.findByUploadedBy(uploadedBy)).thenReturn(
      List.of(testAttachment)
    );

    List<AttachmentMetadata> result =
      attachmentMetadataService.findByUploadedBy(uploadedBy);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUploadedBy()).isEqualTo(uploadedBy);
  }

  @Test
  @DisplayName("findByUploadedBy - Returns empty list when no attachments")
  void findByUploadedBy_NoMatch_ReturnsEmpty() {
    when(attachmentMetadataRepository.findByUploadedBy(any())).thenReturn(
      List.of()
    );

    List<AttachmentMetadata> result =
      attachmentMetadataService.findByUploadedBy(UUID.randomUUID());

    assertThat(result).isEmpty();
  }

  // ── create ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("create - Refuses raw metadata")
  void create_SavesAndReturnsAttachment() {
    assertThatThrownBy(() -> attachmentMetadataService.create(testAttachment))
      .isInstanceOfSatisfying(FileStorageException.class, ex ->
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PERMISSION_DENIED));
    verifyNoInteractions(attachmentMetadataRepository, fileStorageService);
  }

  // ── update ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("update - Refuses raw metadata")
  void update_ExistingAttachment_UpdatesAndReturns() {
    assertThatThrownBy(() -> attachmentMetadataService.update(testId, testAttachment))
      .isInstanceOfSatisfying(FileStorageException.class, ex ->
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PERMISSION_DENIED));
    verifyNoInteractions(attachmentMetadataRepository, fileStorageService);
  }

  @Test
  @DisplayName("update - Refuses unknown targets too")
  void update_NotFound_ThrowsException() {
    assertThatThrownBy(() -> attachmentMetadataService.update(UUID.randomUUID(), testAttachment))
      .isInstanceOfSatisfying(FileStorageException.class, ex ->
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PERMISSION_DENIED));
    verifyNoInteractions(attachmentMetadataRepository, fileStorageService);
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("delete - Deletes attachment when found")
  void delete_ExistingAttachment_Deletes() {
    when(attachmentMetadataRepository.findById(testId)).thenReturn(
      Optional.of(testAttachment)
    );

    attachmentMetadataService.delete(testId);

    verify(cleanup).scheduleDeletion(testAttachment.getStoragePath());
    verifyNoInteractions(fileStorageService);
    verify(attachmentMetadataRepository).deleteById(testId);
  }

  @Test
  @DisplayName("delete - Throws EntityNotFoundException when not found")
  void delete_NotFound_ThrowsException() {
    UUID unknownId = UUID.randomUUID();
    when(attachmentMetadataRepository.findById(unknownId)).thenReturn(
      Optional.empty()
    );

    assertThatThrownBy(() ->
      attachmentMetadataService.delete(unknownId)
    ).isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  @DisplayName("deleteByIncidentId - Refuses public bulk deletion")
  void deleteByIncidentId_DeletesMatchingAttachments() {
    assertThatThrownBy(() -> attachmentMetadataService.deleteByIncidentId(incidentId))
      .isInstanceOfSatisfying(FileStorageException.class, ex ->
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PERMISSION_DENIED));
    verifyNoInteractions(attachmentMetadataRepository, fileStorageService);
  }

  @Test
  @DisplayName("upload - Rejects a file larger than 150 MB before storage")
  void upload_FileLargerThanLimit_RejectsBeforeStorage() {
    AttachmentUpload upload = AttachmentUpload.builder()
      .originalFilename("oversized.pdf")
      .contentType("application/pdf")
      .size(150L * 1024 * 1024 + 1)
      .content(new ByteArrayInputStream(new byte[] { 1 }))
      .incidentId(incidentId)
      .uploadedBy(uploadedBy)
      .build();

    assertThatThrownBy(() -> attachmentMetadataService.upload(upload))
      .isInstanceOfSatisfying(FileStorageException.class, ex ->
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED)
      );
    verifyNoInteractions(fileStorageService, attachmentMetadataRepository);
  }

  @Test
  void upload_EmptyFile_RejectsEveryAttachmentCategoryBeforeStorage() {
    for (String category : List.of("DECLARATION", "COMMENT", "SOLUTION", "TREATMENT", "RESOLUTION", "UNRESOLVED", "CLOSURE", "CANCELLATION")) {
      AttachmentUpload upload = AttachmentUpload.builder()
        .originalFilename("empty.pdf")
        .size(0)
        .content(new ByteArrayInputStream(new byte[0]))
        .incidentId(incidentId)
        .category(category)
        .build();
      assertThatThrownBy(() -> attachmentMetadataService.upload(upload))
        .isInstanceOf(FileStorageException.class)
        .hasMessageContaining("vide");
    }
    verifyNoInteractions(fileStorageService, attachmentMetadataRepository);
  }

  @Test
  void uploadUserAvatar_EmptyFile_RejectsBeforeStorage() {
    AttachmentUpload upload = AttachmentUpload.builder()
      .originalFilename("avatar.png")
      .size(0)
      .content(new ByteArrayInputStream(new byte[0]))
      .build();
    assertThatThrownBy(() -> attachmentMetadataService.uploadUserAvatar(upload))
      .isInstanceOf(FileStorageException.class)
      .hasMessageContaining("vide");
    verifyNoInteractions(fileStorageService, attachmentMetadataRepository);
  }
}
