// DTO : transporte les donnees liees a attachment metadata entre les couches.

package com.fintrack.document.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// DTO de requete pour la creation des metadonnees d'une piece jointe.
@Getter
@Setter
public class AttachmentMetadataRequest {

  @NotNull(message = "{validation.not_null}")
  private UUID incidentId;

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 255)
  private String filename;

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 500)
  private String storagePath;

  @NotNull(message = "{validation.not_null}")
  private Long fileSize;

  @Size(max = 100)
  private String mimeType;

  @NotNull(message = "{validation.not_null}")
  private UUID uploadedBy;

  @NotNull(message = "{validation.not_null}")
  private LocalDateTime uploadedAt;
}
