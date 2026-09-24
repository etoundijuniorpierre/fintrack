package com.fintrack.document.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fintrack.document.model.entity.AttachmentMetadata;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AttachmentMetadataRepositoryTest {

  @Autowired
  private AttachmentMetadataRepository attachmentMetadataRepository;

  @Autowired
  private TestEntityManager entityManager;

  private UUID incidentId;
  private UUID uploadedBy;
  private UUID otherUserId;

  @BeforeEach
  void setUp() {
    incidentId = UUID.randomUUID();
    uploadedBy = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
  }

  private AttachmentMetadata buildAttachment(UUID incidentId, UUID uploadedBy) {
    AttachmentMetadata attachment = new AttachmentMetadata();
    attachment.setIncidentId(incidentId);
    attachment.setFilename("test-document.pdf");
    attachment.setStoragePath("/uploads/test-document.pdf");
    attachment.setFileSize(1024L);
    attachment.setMimeType("application/pdf");
    attachment.setUploadedBy(uploadedBy);
    attachment.setUploadedAt(LocalDateTime.now());
    return attachment;
  }

  @Test
  @DisplayName("findByIncidentId - Returns attachments for given incident")
  void findByIncidentId_ReturnsMatchingAttachments() {
    AttachmentMetadata a1 = buildAttachment(incidentId, uploadedBy);
    AttachmentMetadata a2 = buildAttachment(incidentId, uploadedBy);
    AttachmentMetadata other = buildAttachment(UUID.randomUUID(), uploadedBy);

    entityManager.persist(a1);
    entityManager.persist(a2);
    entityManager.persist(other);
    entityManager.flush();

    List<AttachmentMetadata> result =
      attachmentMetadataRepository.findByIncidentId(incidentId);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(a -> a.getIncidentId().equals(incidentId));
  }

  @Test
  @DisplayName(
    "findByIncidentId - Returns empty when no attachments for incident"
  )
  void findByIncidentId_NoMatch_ReturnsEmpty() {
    assertThat(
      attachmentMetadataRepository.findByIncidentId(UUID.randomUUID())
    ).isEmpty();
  }

  @Test
  @DisplayName("findByUploadedBy - Returns attachments uploaded by given user")
  void findByUploadedBy_ReturnsMatchingAttachments() {
    AttachmentMetadata a1 = buildAttachment(incidentId, uploadedBy);
    AttachmentMetadata a2 = buildAttachment(UUID.randomUUID(), uploadedBy);
    AttachmentMetadata other = buildAttachment(incidentId, otherUserId);

    entityManager.persist(a1);
    entityManager.persist(a2);
    entityManager.persist(other);
    entityManager.flush();

    List<AttachmentMetadata> result =
      attachmentMetadataRepository.findByUploadedBy(uploadedBy);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(a -> a.getUploadedBy().equals(uploadedBy));
  }

  @Test
  @DisplayName(
    "findByUploadedBy - Returns empty when no attachments uploaded by user"
  )
  void findByUploadedBy_NoMatch_ReturnsEmpty() {
    assertThat(
      attachmentMetadataRepository.findByUploadedBy(UUID.randomUUID())
    ).isEmpty();
  }

  @Test
  @DisplayName("save - Persists attachment and generates ID")
  void save_ValidAttachment_PersistsAndGeneratesId() {
    AttachmentMetadata attachment = buildAttachment(incidentId, uploadedBy);

    AttachmentMetadata saved = attachmentMetadataRepository.save(attachment);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getFilename()).isEqualTo("test-document.pdf");
    assertThat(saved.getIncidentId()).isEqualTo(incidentId);
    assertThat(saved.getUploadedBy()).isEqualTo(uploadedBy);
    assertThat(saved.getFileSize()).isEqualTo(1024L);
  }

  @Test
  @DisplayName("save - Persists attachment with audit fields")
  void save_ValidAttachment_PersistsWithAuditFields() {
    AttachmentMetadata attachment = buildAttachment(incidentId, uploadedBy);

    AttachmentMetadata saved = attachmentMetadataRepository.save(attachment);

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    // createdAt et updatedAt sont positionnés par l'auditing Spring Data lors du même
    // appel save() ; on tolère un écart sub-milliseconde car les deux instants peuvent
    // être pris à des nanosecondes différentes par l'auditor.
    assertThat(saved.getCreatedAt()).isCloseTo(
      saved.getUpdatedAt(),
      within(1, ChronoUnit.MILLIS)
    );
  }

  @Test
  @DisplayName("update - Updates audit fields on modification")
  void update_ExistingAttachment_UpdatesAuditFields() {
    AttachmentMetadata attachment = buildAttachment(incidentId, uploadedBy);
    AttachmentMetadata saved = attachmentMetadataRepository.save(attachment);

    // Decale legerement l'horodatage pour verifier le tri temporel.
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    saved.setFilename("updated-document.pdf");
    AttachmentMetadata updated = attachmentMetadataRepository.save(saved);

    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
  }
}
