// DTO : transporte les donnees liees a base entre les couches.

package com.fintrack.reporting.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// DTO de base regroupant les champs d'audit communs aux reponses
@Getter
@Setter
public abstract class BaseDto {

  private UUID id;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UUID modifiedBy;
}
