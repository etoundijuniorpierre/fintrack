// Service metier : coordonne les operations du domaine incident type configuration.

package com.fintrack.incident.service.impl;

import com.fintrack.incident.client.audit.AuditServiceClientService;
import com.fintrack.incident.client.audit.constant.AuditAction;
import com.fintrack.incident.client.audit.constant.AuditStatus;
import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.exception.DuplicateResourceException;
import com.fintrack.incident.exception.EntityNotFoundException;
import com.fintrack.incident.exception.ErrorCode;
import com.fintrack.incident.model.constant.IncidentActorRole;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.readmodel.ExternalService;
import com.fintrack.incident.model.readmodel.ExternalUser;
import com.fintrack.incident.repository.IncidentTypeConfigRepository;
import com.fintrack.incident.service.IncidentTypeConfigService;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementation du service de configuration des types d'incidents.

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IncidentTypeConfigServiceImpl
  implements IncidentTypeConfigService
{

  private final IncidentTypeConfigRepository incidentTypeConfigRepository;
  private final AuditServiceClientService auditServiceClientService;
  private final UserClientService userClientService;

  @Override
  @Transactional(readOnly = true)
  // Liste les types d'incident selon le perimetre demande.
  public Page<IncidentTypeConfig> findAll(Pageable pageable) {
    log.debug(
      "Recherche de toutes les configurations de type d'incident avec pagination : {}",
      pageable
    );
    return incidentTypeConfigRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  // Liste les types d'incident selon le perimetre demande.
  public List<IncidentTypeConfig> findAll() {
    log.debug("Recherche de toutes les configurations de type d'incident");
    return incidentTypeConfigRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les types incident par identifiant.
  public IncidentTypeConfig findById(UUID id) {
    log.debug(
      "Recherche d'une configuration de type d'incident par id : {}",
      id
    );
    return incidentTypeConfigRepository
      .findById(id)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.INCIDENT_TYPE_CONFIG_NOT_FOUND,
          ErrorCode.INCIDENT_TYPE_CONFIG_NOT_FOUND.getMessageKey()
        )
      );
  }

  @Override
  @Transactional(readOnly = true)
  // Recherche les types incident par nom.
  public IncidentTypeConfig findByName(String name) {
    log.debug(
      "Recherche d'une configuration de type d'incident par nom : {}",
      name
    );
    return incidentTypeConfigRepository
      .findByName(name)
      .orElseThrow(() ->
        new EntityNotFoundException(
          ErrorCode.INCIDENT_TYPE_CONFIG_NOT_FOUND,
          "IncidentTypeConfig not found with name: " + name
        )
      );
  }

  @Override
  // Cree un element du domaine incident type configuration apres validation metier.
  public IncidentTypeConfig create(
    IncidentTypeConfig typeConfig,
    UUID actorId
  ) {
    log.debug("Création de new IncidentTypeConfig: {}", typeConfig);

    if (incidentTypeConfigRepository.existsByName(typeConfig.getName())) {
      throw new DuplicateResourceException(
        ErrorCode.INCIDENT_TYPE_CONFIG_DUPLICATE_NAME,
        ErrorCode.INCIDENT_TYPE_CONFIG_DUPLICATE_NAME.getMessageKey()
      );
    }

    validateTargets(typeConfig);

    typeConfig.setId(null);
    applyActorRoleDefaults(typeConfig);
    IncidentTypeConfig saved = incidentTypeConfigRepository.save(typeConfig);
    auditServiceClientService.audit(
      actorId,
      null,
      null,
      AuditAction.SETTINGS_CHANGE.getName(),
      "INCIDENT_TYPE_CONFIG",
      saved.getId().toString(),
      AuditStatus.SUCCESS.getName(),
      typeConfigSnapshot(saved)
    );
    return saved;
  }

  @Override
  // Met a jour un element du domaine incident type configuration avec les donnees validees.
  public IncidentTypeConfig update(
    UUID id,
    IncidentTypeConfig typeConfig,
    UUID actorId
  ) {
    log.debug(
      "Mise à jour de IncidentTypeConfig avec id {}: {}",
      id,
      typeConfig
    );

    IncidentTypeConfig existingConfig = findById(id);

    if (
      !existingConfig.getName().equals(typeConfig.getName()) &&
      incidentTypeConfigRepository.existsByName(typeConfig.getName())
    ) {
      throw new DuplicateResourceException(
        ErrorCode.INCIDENT_TYPE_CONFIG_DUPLICATE_NAME,
        ErrorCode.INCIDENT_TYPE_CONFIG_DUPLICATE_NAME.getMessageKey()
      );
    }

    existingConfig.setName(typeConfig.getName());
    existingConfig.setDisplayName(typeConfig.getDisplayName());
    existingConfig.setDescription(typeConfig.getDescription());
    existingConfig.setActive(typeConfig.isActive());
    existingConfig.setSlaHours(typeConfig.getSlaHours());
    existingConfig.setDefaultTargetServiceId(
      typeConfig.getDefaultTargetServiceId()
    );
    existingConfig.setDefaultTargetUserId(typeConfig.getDefaultTargetUserId());
    existingConfig.setDefaultCriticality(typeConfig.getDefaultCriticality());
    existingConfig.setRequiresValidation(typeConfig.isRequiresValidation());
    existingConfig.setRequiresCauseAnalysis(
      typeConfig.isRequiresCauseAnalysis()
    );
    existingConfig.setRequiresDirectionValidation(
      typeConfig.isRequiresDirectionValidation()
    );
    existingConfig.setDirectionValidatorIds(
      typeConfig.getDirectionValidatorIds() != null
        ? new HashSet<>(typeConfig.getDirectionValidatorIds())
        : new HashSet<>()
    );
    existingConfig.setTreaterRoles(
      new HashSet<>(typeConfig.getTreaterRoles())
    );
    existingConfig.setResolverRoles(
      new HashSet<>(typeConfig.getResolverRoles())
    );
    existingConfig.setCloserRoles(new HashSet<>(typeConfig.getCloserRoles()));
    existingConfig.setReopenerRoles(
      new HashSet<>(typeConfig.getReopenerRoles())
    );
    applyActorRoleDefaults(existingConfig);
    existingConfig.setValidatorScope(typeConfig.getValidatorScope());

    validateTargets(existingConfig);

    IncidentTypeConfig saved = incidentTypeConfigRepository.save(
      existingConfig
    );
    auditServiceClientService.audit(
      actorId,
      null,
      null,
      AuditAction.SETTINGS_CHANGE.getName(),
      "INCIDENT_TYPE_CONFIG",
      id.toString(),
      AuditStatus.SUCCESS.getName(),
      typeConfigSnapshot(saved)
    );
    return saved;
  }

  @Override
  // Supprime le type d'incident et conserve un instantane complet dans l'audit.
  public void delete(UUID id, UUID actorId) {
    log.debug("Suppression de IncidentTypeConfig avec id: {}", id);

    // On charge l'entite avant suppression pour figer son instantane dans l'audit.
    IncidentTypeConfig existingConfig = findById(id);
    Map<String, Object> snapshot = typeConfigSnapshot(existingConfig);

    incidentTypeConfigRepository.deleteById(id);
    auditServiceClientService.audit(
      actorId,
      null,
      null,
      AuditAction.SETTINGS_CHANGE.getName(),
      "INCIDENT_TYPE_CONFIG",
      id.toString(),
      AuditStatus.SUCCESS.getName(),
      snapshot
    );
  }

  /** Instantané des champs d'un type d'incident, conservé dans l'audit. */
  // Realise l'intention metier type config instantane.
  private Map<String, Object> typeConfigSnapshot(
    IncidentTypeConfig typeConfig
  ) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("name", typeConfig.getName());
    snapshot.put("displayName", typeConfig.getDisplayName());
    snapshot.put("description", typeConfig.getDescription());
    snapshot.put("active", typeConfig.isActive());
    snapshot.put("slaHours", typeConfig.getSlaHours());
    snapshot.put(
      "defaultTargetServiceId",
      typeConfig.getDefaultTargetServiceId() != null
        ? typeConfig.getDefaultTargetServiceId().toString()
        : null
    );
    snapshot.put(
      "defaultTargetUserId",
      typeConfig.getDefaultTargetUserId() != null
        ? typeConfig.getDefaultTargetUserId().toString()
        : null
    );
    snapshot.put("requiresValidation", typeConfig.isRequiresValidation());
    snapshot.put("requiresCauseAnalysis", typeConfig.isRequiresCauseAnalysis());
    snapshot.put("requiresDirectionValidation", typeConfig.isRequiresDirectionValidation());
    snapshot.put(
      "directionValidatorIds",
      typeConfig.getDirectionValidatorIds() != null
        ? typeConfig
          .getDirectionValidatorIds()
          .stream()
          .map(UUID::toString)
          .sorted()
          .toList()
        : List.of()
    );
    snapshot.put("treaterRoles", roleNames(typeConfig.getTreaterRoles()));
    snapshot.put("resolverRoles", roleNames(typeConfig.getResolverRoles()));
    snapshot.put("closerRoles", roleNames(typeConfig.getCloserRoles()));
    snapshot.put("reopenerRoles", roleNames(typeConfig.getReopenerRoles()));
    snapshot.put(
      "validatorScope",
      typeConfig.getValidatorScope() != null
        ? typeConfig.getValidatorScope().name()
        : null
    );
    return snapshot;
  }

  // Noms de roles tries pour un instantane d'audit stable.
  private List<String> roleNames(Set<IncidentActorRole> roles) {
    return roles == null
      ? List.of()
      : roles.stream().map(IncidentActorRole::name).sorted().toList();
  }

  // Defauts d'etape (multi-choix) : appliques quand aucun role n'est configure.
  private static final Set<IncidentActorRole> DEFAULT_TREATER_ROLES =
    EnumSet.of(IncidentActorRole.ASSIGNEE);
  private static final Set<IncidentActorRole> DEFAULT_RESOLVER_ROLES =
    EnumSet.of(IncidentActorRole.SOURCE_AGENCY_MANAGER);
  private static final Set<IncidentActorRole> DEFAULT_CLOSER_ROLES =
    EnumSet.of(IncidentActorRole.ASSIGNEE);
  private static final Set<IncidentActorRole> DEFAULT_REOPENER_ROLES =
    EnumSet.of(IncidentActorRole.SOURCE_AGENCY_MANAGER);

  // Renseigne chaque etape avec son defaut si aucun role n'a ete choisi.
  private void applyActorRoleDefaults(IncidentTypeConfig config) {
    config.setTreaterRoles(
      orDefaultRoles(config.getTreaterRoles(), DEFAULT_TREATER_ROLES)
    );
    config.setResolverRoles(
      orDefaultRoles(config.getResolverRoles(), DEFAULT_RESOLVER_ROLES)
    );
    config.setCloserRoles(
      orDefaultRoles(config.getCloserRoles(), DEFAULT_CLOSER_ROLES)
    );
    config.setReopenerRoles(
      orDefaultRoles(config.getReopenerRoles(), DEFAULT_REOPENER_ROLES)
    );
  }

  private Set<IncidentActorRole> orDefaultRoles(
    Set<IncidentActorRole> roles,
    Set<IncidentActorRole> defaults
  ) {
    return (roles == null || roles.isEmpty())
      ? new HashSet<>(defaults)
      : new HashSet<>(roles);
  }

  private void validateTargets(IncidentTypeConfig config) {
    UUID serviceId = config.getDefaultTargetServiceId();
    UUID userId = config.getDefaultTargetUserId();

    if (serviceId != null) {
      ExternalService service = userClientService.getService(serviceId);
      if (service == null) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          "incident_type_config.error.target_service_not_found"
        );
      }
      if (!service.isActive()) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          "incident_type_config.error.target_service_inactive"
        );
      }
      UUID headId = userClientService.resolveServiceHead(serviceId);
      if (headId != null && userId == null) {
        ExternalUser head = userClientService.getUser(headId);
        if (head != null) {
          if (!head.isActive()) {
            throw new BusinessRuleViolationException(
              ErrorCode.INVALID_INPUT,
              "incident_type_config.error.target_service_manager_inactive"
            );
          }
          if (
            head.getPermissions() == null ||
            (!head.getPermissions().contains("INCIDENT_TREAT") &&
              !head.getPermissions().contains("INCIDENT_RESOLVE"))
          ) {
            throw new BusinessRuleViolationException(
              ErrorCode.INVALID_INPUT,
              "incident_type_config.error.target_service_manager_permissions"
            );
          }
        }
      }
    }

    if (userId != null) {
      ExternalUser user = userClientService.getUser(userId);
      if (user == null) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          "incident_type_config.error.target_user_not_found"
        );
      }
      if (!user.isActive()) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          "incident_type_config.error.target_user_inactive"
        );
      }
      if (
        user.getPermissions() == null ||
        (!user.getPermissions().contains("INCIDENT_TREAT") &&
          !user.getPermissions().contains("INCIDENT_RESOLVE"))
      ) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          "incident_type_config.error.target_user_permissions"
        );
      }

      if (serviceId != null) {
        boolean coherent =
          (user.getServiceId() != null &&
            user.getServiceId().equals(serviceId)) ||
          (user.getManagedServiceIds() != null &&
            user.getManagedServiceIds().contains(serviceId));
        if (!coherent) {
          throw new BusinessRuleViolationException(
            ErrorCode.INVALID_INPUT,
            "incident_type_config.error.target_user_outside_service"
          );
        }
      }
    }

    if (config.isRequiresDirectionValidation()) {
      Set<UUID> validatorIds = config.getDirectionValidatorIds();
      if (validatorIds == null || validatorIds.isEmpty()) {
        throw new BusinessRuleViolationException(
          ErrorCode.INVALID_INPUT,
          "incident_type_config.error.direction_validator_required"
        );
      }
      for (UUID validatorId : validatorIds) {
        ExternalUser validator = userClientService.getUser(validatorId);
        if (validator == null) {
          throw new BusinessRuleViolationException(
            ErrorCode.INVALID_INPUT,
            "incident_type_config.error.direction_validator_not_found"
          );
        }
        if (!validator.isActive()) {
          throw new BusinessRuleViolationException(
            ErrorCode.INVALID_INPUT,
            "incident_type_config.error.direction_validator_inactive"
          );
        }
        if (
          validator.getPermissions() == null ||
          !validator.getPermissions().contains("VALIDATION_DIRECTION")
        ) {
          throw new BusinessRuleViolationException(
            ErrorCode.INVALID_INPUT,
            "incident_type_config.error.direction_validator_permissions"
          );
        }
      }
    } else {
      config.setDirectionValidatorIds(new HashSet<>());
    }
  }
}
