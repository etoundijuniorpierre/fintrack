package com.fintrack.document.model.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
@Entity @Table(name = "attachment_storage_cleanup")
@Data @NoArgsConstructor @AllArgsConstructor
public class AttachmentStorageCleanup {
  @Id private UUID id;
  @Column(name = "object_key", nullable = false, length = 500) private String objectKey;
  @Column(name = "retry_after", nullable = false) private LocalDateTime retryAfter;
  @Column(nullable = false) private int attempts;
}
