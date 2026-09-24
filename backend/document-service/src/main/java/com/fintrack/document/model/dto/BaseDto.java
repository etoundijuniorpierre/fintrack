// DTO : transporte les donnees liees a base entre les couches.

package com.fintrack.document.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// DTO de base regroupant les champs communs d'audit (identifiant, dates, auteur de modification).
@Getter
@Setter
public abstract class BaseDto {

  private UUID id;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UUID modifiedBy;
}
