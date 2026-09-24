package com.fintrack.incident.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "attachment_cleanup_tasks")
@Data @NoArgsConstructor @AllArgsConstructor
public class AttachmentCleanupTask {
  @Id private UUID id;
  private UUID incidentId;
  private UUID commentId;
  private LocalDateTime retryAfter;
  private int attempts;
}
