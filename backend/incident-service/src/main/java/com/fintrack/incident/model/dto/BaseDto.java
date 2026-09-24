// DTO : transporte les donnees liees a base entre les couches.

package com.fintrack.incident.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

// Classe de base contenant les proprietes communes des objets de transfert de donnees (DTO).

@Data
public abstract class BaseDto {

  private UUID id;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UUID modifiedBy;
}
