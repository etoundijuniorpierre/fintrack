// DTO : transporte les donnees liees a incident comment entre les couches.

package com.fintrack.incident.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.incident.model.dto.BaseDto;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

// DTO de reponse detaillant un commentaire d'incident.

@Data
@EqualsAndHashCode(callSuper = true)
public class IncidentCommentResponse extends BaseDto {

  private UserSummaryResponse author;
  private String content;

  @JsonProperty("isInternal")
  private boolean isInternal;

  private UUID parentCommentId;
  private UserSummaryResponse parentAuthor;
}
