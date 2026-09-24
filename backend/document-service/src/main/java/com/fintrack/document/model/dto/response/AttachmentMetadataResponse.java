// DTO : transporte les donnees liees a attachment metadata entre les couches.

package com.fintrack.document.model.dto.response;

import com.fintrack.document.model.dto.BaseDto;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO de reponse exposant les metadonnees completes d'une piece jointe.
@EqualsAndHashCode(callSuper = true)
public class AttachmentMetadataResponse extends BaseDto {

  private UUID incidentId;
  private UUID commentId;
  private IncidentSummaryResponse incident;
  private String category;
  private String filename;
  private String storagePath;
  private Long fileSize;
  private String mimeType;
  private UUID uploadedById;
  private UserSummaryResponse uploadedBy;
  private LocalDateTime uploadedAt;
}
