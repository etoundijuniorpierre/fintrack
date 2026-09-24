package com.fintrack.document.model.entity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
@Entity @Table(name = "attachment_upload_receipts")
@Data @NoArgsConstructor @AllArgsConstructor
public class AttachmentUploadReceipt {
  @Id private UUID id;
  @Column(name = "attachment_id", nullable = false) private UUID attachmentId;
  @Column(nullable = false, length = 64) private String fingerprint;
}
