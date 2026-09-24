// Mapper : convertit les donnees liees a incident history entre modeles.

package com.fintrack.incident.model.mapper;

import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.dto.response.IncidentHistoryResponse;
import com.fintrack.incident.model.dto.response.UserSummaryResponse;
import com.fintrack.incident.model.entity.IncidentHistory;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

// Mapper MapStruct pour convertir l'historique des incidents en DTOs.

@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class IncidentHistoryMapper {

  private static final Pattern UUID_PATTERN = Pattern.compile(
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
  );

  @Autowired
  protected UserClientService userClientService;

  @Autowired
  protected IncidentTypeConfigRepository incidentTypeConfigRepository;

  // Convertit les donnees du domaine incident history entre les modeles utilises.

  @Mapping(
    target = "user",
    source = "userId",
    qualifiedByName = "historyUuidToUserSummary"
  )
  @Mapping(target = "oldValue", ignore = true)
  @Mapping(target = "newValue", ignore = true)
  public abstract IncidentHistoryResponse toResponse(IncidentHistory entity);

  // Realise l'intention metier enrich display values.

  @AfterMapping
  protected void enrichDisplayValues(
    IncidentHistory entity,
    @MappingTarget IncidentHistoryResponse response
  ) {
    response.setOldValue(
      resolveDisplayValue(entity.getAction(), entity.getOldValue())
    );
    response.setNewValue(
      resolveDisplayValue(entity.getAction(), entity.getNewValue())
    );
  }

  // Convertit une collection d'entites en reponses API.
  public List<IncidentHistoryResponse> toResponseList(
    List<IncidentHistory> entities
  ) {
    if (entities == null) return List.of();
    return entities.stream().map(this::toResponse).toList();
  }

  // Realise l'intention metier history uuid to user summary.

  @Named("historyUuidToUserSummary")
  protected UserSummaryResponse historyUuidToUserSummary(UUID id) {
    return userClientService.resolveUser(id);
  }

  // Resout display value a partir du contexte disponible.

  private String resolveDisplayValue(ActionType action, String rawValue) {
    if (rawValue == null || rawValue.isBlank()) {
      return rawValue;
    }
    if (!isUuid(rawValue)) {
      // Statut, libelle libre, etc. : on laisse le frontend traduire.
      return rawValue;
    }
    UUID id;
    try {
      id = UUID.fromString(rawValue);
    } catch (IllegalArgumentException ex) {
      return rawValue;
    }

    if (action == ActionType.TYPE_CHANGE) {
      return incidentTypeConfigRepository
        .findById(id)
        .map(typeConfig -> typeConfig.getDisplayName())
        .orElse(rawValue);
    }

    // ASSIGNMENT : UUID utilisateur. Pour les autres actions on tente
    // aussi user service afin de ne jamais laisser un UUID brut s'afficher.
    String userLabel = resolveUserLabel(id);
    if (userLabel != null) {
      return userLabel;
    }
    if (action == ActionType.TRANSFER || action == ActionType.ROUTING) {
      String serviceName = userClientService.resolveServiceName(id);
      if (
        serviceName != null &&
        !serviceName.isBlank() &&
        !serviceName.equals(id.toString())
      ) {
        return serviceName;
      }
      String agencyName = userClientService.resolveAgencyName(id);
      if (
        agencyName != null &&
        !agencyName.isBlank() &&
        !agencyName.equals(id.toString())
      ) {
        return agencyName;
      }
    }
    return incidentTypeConfigRepository
      .findById(id)
      .map(typeConfig -> typeConfig.getDisplayName())
      .orElse(rawValue);
  }

  // Resout utilisateur label a partir du contexte disponible.

  private String resolveUserLabel(UUID id) {
    UserSummaryResponse user = userClientService.resolveUser(id);
    if (user == null) return null;
    String firstName = user.getFirstName();
    String lastName = user.getLastName();
    boolean hasFirst = firstName != null && !firstName.isBlank();
    boolean hasLast = lastName != null && !lastName.isBlank();
    if (hasFirst && hasLast) return firstName + " " + lastName;
    if (hasFirst) return firstName;
    if (hasLast) return lastName;
    String username = user.getUsername();
    if (
      username != null && !username.isBlank() && !"unknown".equals(username)
    ) {
      return username;
    }
    return null;
  }

  // Verifie si uuid.

  private boolean isUuid(String value) {
    return UUID_PATTERN.matcher(value).matches();
  }
}
