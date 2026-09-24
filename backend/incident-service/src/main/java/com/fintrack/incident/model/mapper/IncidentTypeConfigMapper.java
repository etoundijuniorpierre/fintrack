// Mapper : convertit les donnees liees a incident type config entre modeles.
package com.fintrack.incident.model.mapper;

import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.model.dto.request.IncidentTypeConfigRequest;
import com.fintrack.incident.model.dto.response.IncidentTypeConfigResponse;
import com.fintrack.incident.model.dto.response.ServiceSummaryResponse;
import com.fintrack.incident.model.dto.response.UserSummaryResponse;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.readmodel.ExternalService;
import com.fintrack.incident.model.readmodel.ExternalUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

// Mapper MapStruct pour la configuration des types d'incidents.

@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE,
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class IncidentTypeConfigMapper {

  @Autowired
  protected UserClientService userClientService;

  // Convertit les donnees du domaine incident type configuration entre les modeles utilises.

  // defaultValue applique le defaut a la creation quand le champ est omis (null).
  // Indispensable pour validatorScope : sur toEntity (sans @MappingTarget) la
  // strategie IGNORE ne s'applique pas et l'enum recevrait null (colonne NOT NULL).
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "active", source = "active", defaultValue = "true")
  @Mapping(
    target = "requiresValidation",
    source = "requiresValidation",
    defaultValue = "true"
  )
  @Mapping(
    target = "requiresCauseAnalysis",
    source = "requiresCauseAnalysis",
    defaultValue = "true"
  )
  @Mapping(
    target = "requiresDirectionValidation",
    source = "requiresDirectionValidation",
    defaultValue = "true"
  )
  @Mapping(target = "slaHours", source = "slaHours")
  @Mapping(
    target = "defaultCriticality",
    source = "defaultCriticality",
    defaultValue = "HIGH"
  )
  @Mapping(
    target = "emailNotificationsEnabled",
    source = "emailNotificationsEnabled",
    defaultValue = "false"
  )
  @Mapping(target = "inAppNotificationsEnabled", constant = "true")
  @Mapping(
    target = "validatorScope",
    source = "validatorScope",
    defaultValue = "AGENCY_MANAGER"
  )
  // treaterRoles/resolverRoles/closerRoles/reopenerRoles : auto-mappes par nom
  // (memes Set<IncidentActorRole>). Les defauts d'etape sont appliques par le service.
  public abstract IncidentTypeConfig toEntity(
    IncidentTypeConfigRequest request
  );

  // Convertit les donnees du domaine incident type configuration entre les modeles utilises.

  @Mapping(
    target = "defaultTargetService",
    source = "defaultTargetServiceId",
    qualifiedByName = "uuidToServiceSummary"
  )
  @Mapping(
    target = "defaultTargetUser",
    source = "defaultTargetUserId",
    qualifiedByName = "uuidToUserSummary"
  )
  @Mapping(
    target = "directionValidators",
    source = "directionValidatorIds",
    qualifiedByName = "uuidsToUserSummaries"
  )
  public abstract IncidentTypeConfigResponse toResponse(
    IncidentTypeConfig entity
  );

  // Applique le changement demande apres validation metier.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "active", source = "active")
  @Mapping(target = "inAppNotificationsEnabled", constant = "true")
  @Mapping(
    target = "defaultTargetServiceId",
    source = "defaultTargetServiceId",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
  )
  @Mapping(
    target = "defaultTargetUserId",
    source = "defaultTargetUserId",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
  )
  @Mapping(target = "directionValidatorIds", source = "directionValidatorIds")
  @Mapping(target = "treaterRoles", source = "treaterRoles")
  @Mapping(target = "resolverRoles", source = "resolverRoles")
  @Mapping(target = "closerRoles", source = "closerRoles")
  @Mapping(target = "reopenerRoles", source = "reopenerRoles")
  @Mapping(
    target = "description",
    source = "description",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
  )
  @Mapping(
    target = "slaHours",
    source = "slaHours",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
  )
  @Mapping(
    target = "defaultCriticality",
    source = "defaultCriticality",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
  )
  public abstract void updateEntityFromRequest(
    IncidentTypeConfigRequest request,
    @MappingTarget IncidentTypeConfig entity
  );

  // Realise l'intention metier uuid to service summary.

  @Named("uuidToServiceSummary")
  protected ServiceSummaryResponse uuidToServiceSummary(UUID id) {
    if (id == null) return null;
    ExternalService r = userClientService.getService(id);
    if (r == null) return null;
    ServiceSummaryResponse s = new ServiceSummaryResponse();
    s.setId(r.getId());
    s.setName(r.getName());
    s.setDescription(r.getDescription());
    return s;
  }

  // Realise l'intention metier uuid to user summary.

  // Resout une liste d'utilisateurs (valideurs de la Direction) a partir de leurs identifiants.
  @Named("uuidsToUserSummaries")
  protected List<UserSummaryResponse> uuidsToUserSummaries(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    List<UserSummaryResponse> summaries = new ArrayList<>();
    for (UUID id : ids) {
      UserSummaryResponse summary = uuidToUserSummary(id);
      if (summary != null) summaries.add(summary);
    }
    return summaries;
  }

  @Named("uuidToUserSummary")
  protected UserSummaryResponse uuidToUserSummary(UUID id) {
    if (id == null) return null;
    ExternalUser r = userClientService.getUser(id);
    if (r == null) return null;
    UserSummaryResponse u = new UserSummaryResponse();
    u.setId(r.getId());
    u.setUsername(r.getUsername());
    u.setFirstName(r.getFirstName());
    u.setLastName(r.getLastName());
    u.setEmail(r.getEmail());
    return u;
  }
}
