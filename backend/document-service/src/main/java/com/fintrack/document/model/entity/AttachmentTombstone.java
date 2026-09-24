package com.fintrack.document.model.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "attachment_tombstones")
@Data @NoArgsConstructor @AllArgsConstructor
public class AttachmentTombstone {
  @Id @Column(length = 100) private String id;
}
