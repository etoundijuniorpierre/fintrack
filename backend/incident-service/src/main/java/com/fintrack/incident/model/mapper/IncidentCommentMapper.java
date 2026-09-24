// Mapper : convertit les donnees liees a incident comment entre modeles.

package com.fintrack.incident.model.mapper;

import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.model.dto.request.IncidentCommentRequest;
import com.fintrack.incident.model.dto.response.IncidentCommentResponse;
import com.fintrack.incident.model.dto.response.UserSummaryResponse;
import com.fintrack.incident.model.entity.IncidentComment;
import java.util.List;
import java.util.UUID;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

// Mapper MapStruct pour convertir les commentaires d'incidents entre entites et DTOs.

@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class IncidentCommentMapper {

  @Autowired
  protected UserClientService userClientService;

  // Convertit les donnees du domaine incident comment entre les modeles utilises.

  @Mapping(
    target = "author",
    source = "userId",
    qualifiedByName = "commentUuidToUserSummary"
  )
  @Mapping(target = "parentCommentId", source = "parentComment.id")
  @Mapping(
    target = "parentAuthor",
    source = "parentComment.userId",
    qualifiedByName = "commentUuidToUserSummary"
  )
  public abstract IncidentCommentResponse toResponse(IncidentComment entity);

  // Convertit une collection d'entites en reponses API.
  public List<IncidentCommentResponse> toResponseList(
    List<IncidentComment> entities
  ) {
    if (entities == null) return List.of();
    return entities.stream().map(this::toResponse).toList();
  }

  // Precise l'intention metier associee a incident comment.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "incident", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "parentComment", ignore = true)
  public abstract IncidentComment toEntity(IncidentCommentRequest request);

  // Realise l'intention metier comment uuid to user summary.

  @Named("commentUuidToUserSummary")
  protected UserSummaryResponse commentUuidToUserSummary(UUID id) {
    return userClientService.resolveUser(id);
  }
}
