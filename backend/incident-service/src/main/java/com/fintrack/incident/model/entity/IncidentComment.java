// Entite metier : represente les donnees persistees liees a incident comment.

package com.fintrack.incident.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite JPA representant un commentaire associe a un incident.

@Getter
@Setter
@ToString(exclude = { "incident" })
@EqualsAndHashCode(callSuper = true, exclude = { "incident" })
@Entity
@Table(name = "incident_comments")
@Access(AccessType.FIELD)
public class IncidentComment extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private Incident incident;

  @NotNull
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @NotBlank
  @Size(max = 2000)
  @Column(nullable = false, length = 2000)
  private String content;

  @Column(name = "is_internal", nullable = false)
  private boolean isInternal;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_comment_id")
  private IncidentComment parentComment;
}
