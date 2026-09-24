// Entite metier : represente les donnees persistees liees a attachment metadata.

package com.fintrack.document.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
// Entite persistee representant les metadonnees d'un fichier joint a un incident.
@Entity
@Table(name = "attachments_metadata")
@Access(AccessType.FIELD)
public class AttachmentMetadata extends BaseEntity {

  @Column(name = "incident_id")
  private UUID incidentId;

  // Commentaire rattache (null pour une PJ d'incident).
  @Column(name = "comment_id")
  private UUID commentId;

  @Size(max = 50)
  @Column(name = "category", length = 50)
  private String category;

  @NotBlank
  @Size(max = 255)
  @Column(nullable = false, length = 255)
  private String filename;

  @NotBlank
  @Size(max = 500)
  @Column(name = "storage_path", nullable = false, length = 500)
  private String storagePath;

  @NotNull
  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Size(max = 100)
  @Column(name = "mime_type", length = 100)
  private String mimeType;

  @NotNull
  @Column(name = "uploaded_by", nullable = false)
  private UUID uploadedBy;

  @NotNull
  @Column(name = "uploaded_at", nullable = false)
  private LocalDateTime uploadedAt;
}
