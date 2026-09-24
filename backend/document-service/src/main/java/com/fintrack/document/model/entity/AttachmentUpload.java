// Entite metier : represente les donnees persistees liees a attachment upload.

package com.fintrack.document.model.entity;

import java.io.InputStream;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Entite metier representant piece jointe upload.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUpload {

  private String originalFilename;
  private String contentType;
  private long size;
  private InputStream content;
  private UUID incidentId;
  private UUID commentId;
  private String category;
  private UUID uploadedBy;
  private UUID uploadId;

  public AttachmentUpload withUploadId(UUID id) {
    this.uploadId = id;
    return this;
  }
}
