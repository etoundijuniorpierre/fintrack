// Entite metier : represente les donnees persistees liees a base.

package com.fintrack.document.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

// Entite de base fournissant l'identifiant et les champs d'audit aux entites persistees.
@Data
@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "modified_by")
  private UUID modifiedBy;

  // Renseigne les dates de creation et de mise a jour avant l'insertion.
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  // Met a jour la date de modification avant chaque mise a jour.
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
