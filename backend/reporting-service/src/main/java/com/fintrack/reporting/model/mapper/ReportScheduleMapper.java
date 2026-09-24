// Mapper : convertit les donnees liees a report schedule entre modeles.

package com.fintrack.reporting.model.mapper;

import com.fintrack.reporting.client.user.UserServiceClientService;
import com.fintrack.reporting.model.dto.request.ReportScheduleRequest;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.dto.response.ReportScheduleResponse;
import com.fintrack.reporting.model.dto.response.UserSummaryResponse;
import com.fintrack.reporting.model.entity.ReportSchedule;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

// Mapper MapStruct pour la planification des rapports.

@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class ReportScheduleMapper {

  @Autowired
  protected UserServiceClientService userServiceClientService;

  // Precise l'intention metier associee a planification de rapport.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "lastGeneratedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  public abstract ReportSchedule toEntity(ReportScheduleRequest request);

  // Convertit les donnees du domaine planification de rapport entre les modeles utilises.

  @Mapping(target = "id", source = "id")
  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "updatedAt", source = "updatedAt")
  @Mapping(target = "modifiedBy", source = "modifiedBy")
  @Mapping(
    target = "createdBy",
    source = "createdBy",
    qualifiedByName = "uuidToUserSummary"
  )
  public abstract ReportScheduleResponse toResponse(ReportSchedule entity);

  // Garantit la compatibilite des programmations creees avant le modele de contenu.
  @AfterMapping
  protected void applyContentTypeDefault(
    @MappingTarget ReportScheduleResponse response
  ) {
    response.setContentType(ReportContentType.orDefault(response.getContentType()));
  }

  // Realise l'intention metier uuid to user summary.

  @Named("uuidToUserSummary")
  protected UserSummaryResponse uuidToUserSummary(UUID id) {
    if (id == null) return null;
    return userServiceClientService.resolveUser(id);
  }

  // Liste to chaine.

  public String listToString(List<String> list) {
    if (list == null) return null;
    return String.join(",", list);
  }

  // Realise l'intention metier chaine to liste.

  public List<String> stringToList(String str) {
    if (str == null || str.isBlank()) return List.of();
    return Arrays.asList(str.split(","));
  }
}
