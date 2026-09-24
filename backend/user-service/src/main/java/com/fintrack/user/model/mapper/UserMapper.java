// Mapper : convertit les donnees liees a user entre modeles.

package com.fintrack.user.model.mapper;

import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.dto.request.ProfileUpdateRequest;
import com.fintrack.user.model.dto.request.UserRequest;
import com.fintrack.user.model.dto.response.AgencyResponse;
import com.fintrack.user.model.dto.response.ServiceResponse;
import com.fintrack.user.model.dto.response.UserResponse;
import com.fintrack.user.model.dto.response.UserStatsResponse;
import com.fintrack.user.model.dto.response.UserSummaryResponse;
import com.fintrack.user.model.entity.*;
import com.fintrack.user.model.readmodel.UserStats;
import com.fintrack.user.repository.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.*;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

// Mapper MapStruct pour la gestion des utilisateurs.

@Mapper(
  componentModel = "spring",
  uses = { RoleMapper.class, PermissionMapper.class },
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
// Assure les conversions du domaine user.
public abstract class UserMapper {

  @Autowired
  protected AgencyRepository agencyRepository;

  @Autowired
  protected ServiceRepository serviceRepository;

  @Autowired
  protected RoleRepository roleRepository;

  @Autowired
  protected PermissionRepository permissionRepository;

  @Autowired
  protected MessageSource messageSource;

  protected String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  // Realise l'intention metier user to user reponse.

  @Mapping(
    target = "agency",
    source = "agency",
    qualifiedByName = "agencyToUserResponse"
  )
  @Mapping(
    target = "service",
    source = "service",
    qualifiedByName = "serviceToUserResponse"
  )
  @Mapping(
    target = "managedServiceIds",
    source = "managedServices",
    qualifiedByName = "servicesToIds"
  )
  @Mapping(target = "managedAgencyId", source = "managedAgency.id")
  public abstract UserResponse userToUserResponse(User user);

  // Realise l'intention metier user to admin user reponse.

  @Mapping(
    target = "agency",
    source = "agency",
    qualifiedByName = "agencyToUserResponse"
  )
  @Mapping(
    target = "service",
    source = "service",
    qualifiedByName = "serviceToUserResponse"
  )
  @Mapping(
    target = "managedServiceIds",
    source = "managedServices",
    qualifiedByName = "servicesToIds"
  )
  @Mapping(target = "managedAgencyId", source = "managedAgency.id")
  public abstract UserResponse userToAdminUserResponse(User user);

  // Precise l'intention metier associee a utilisateur.

  @Mapping(
    target = "agency",
    source = "agency",
    qualifiedByName = "agencyToUserResponse"
  )
  @Mapping(
    target = "service",
    source = "service",
    qualifiedByName = "serviceToUserResponse"
  )
  @Mapping(
    target = "managedServiceIds",
    source = "managedServices",
    qualifiedByName = "servicesToIds"
  )
  @Mapping(target = "managedAgencyId", source = "managedAgency.id")
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "permissions", ignore = true)
  public abstract UserResponse userToScopedUserResponse(User user);

  // Realise l'intention metier user to user summary reponse.

  public abstract UserSummaryResponse userToUserSummaryResponse(User user);

  // Precise l'intention metier associee a utilisateur.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(
    target = "managedServices",
    source = "managedServiceIds",
    qualifiedByName = "idsToServices"
  )
  @Mapping(
    target = "agency",
    source = "agencyId",
    qualifiedByName = "idToAgency"
  )
  @Mapping(
    target = "service",
    source = "serviceId",
    qualifiedByName = "idToService"
  )
  @Mapping(target = "roles", source = "roleIds", qualifiedByName = "idsToRoles")
  @Mapping(
    target = "permissions",
    source = "permissionIds",
    qualifiedByName = "idsToPermissions"
  )
  @Mapping(
    target = "revokedPermissions",
    source = "revokedPermissionIds",
    qualifiedByName = "idsToPermissions"
  )
  @Mapping(
    target = "managedAgency",
    source = "managedAgencyId",
    qualifiedByName = "idToAgency"
  )
  public abstract User userRequestToUser(UserRequest request);

  // Applique le changement demande apres validation metier.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(
    target = "managedServices",
    source = "managedServiceIds",
    qualifiedByName = "idsToServices"
  )
  @Mapping(
    target = "agency",
    source = "agencyId",
    qualifiedByName = "idToAgency"
  )
  @Mapping(
    target = "service",
    source = "serviceId",
    qualifiedByName = "idToService"
  )
  @Mapping(target = "roles", source = "roleIds", qualifiedByName = "idsToRoles")
  @Mapping(
    target = "permissions",
    source = "permissionIds",
    qualifiedByName = "idsToPermissions"
  )
  @Mapping(
    target = "revokedPermissions",
    source = "revokedPermissionIds",
    qualifiedByName = "idsToPermissions"
  )
  @Mapping(
    target = "managedAgency",
    source = "managedAgencyId",
    qualifiedByName = "idToAgency"
  )
  public abstract void updateUserFromRequest(
    UserRequest request,
    @MappingTarget User user
  );

  // Realise l'intention metier user liste to user reponse liste.

  public abstract List<UserSummaryResponse> userListToUserResponseList(
    List<User> users
  );

  // Realise l'intention metier user stats to user stats reponse.

  public abstract UserStatsResponse userStatsToUserStatsResponse(
    UserStats stats
  );

  // Precise l'intention metier associee a utilisateur.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "password", ignore = true)
  @Mapping(target = "firstName", ignore = true)
  @Mapping(target = "lastName", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "failedLoginAttempts", ignore = true)
  @Mapping(target = "lastLogin", ignore = true)
  @Mapping(target = "firstLogin", ignore = true)
  @Mapping(target = "agency", ignore = true)
  @Mapping(target = "service", ignore = true)
  @Mapping(target = "managedServices", ignore = true)
  @Mapping(target = "managedAgency", ignore = true)
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "permissions", ignore = true)
  @Mapping(
    target = "phoneNumber",
    source = "phoneNumber",
    qualifiedByName = "stringToPhoneNumber"
  )
  public abstract User profileUpdateRequestToUser(ProfileUpdateRequest request);

  // Realise l'intention metier id to agency.

  @Named("idToAgency")
  protected Agency idToAgency(UUID id) {
    return id != null
      ? agencyRepository
          .findById(id)
          .orElseThrow(() ->
            new EntityNotFoundException(
              "Agence introuvable avec l'identifiant : " + id
            )
          )
      : null;
  }

  // Realise l'intention metier id to service.

  @Named("idToService")
  protected ServiceEntity idToService(UUID id) {
    return id != null
      ? serviceRepository
          .findById(id)
          .orElseThrow(() ->
            new EntityNotFoundException(
              "Service introuvable avec l'identifiant : " + id
            )
          )
      : null;
  }

  // Realise l'intention metier ids to roles.

  @Named("idsToRoles")
  protected Set<Role> idsToRoles(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return null;
    }
    List<Role> roles = roleRepository.findAllById(ids);
    if (roles.size() != ids.size()) {
      Set<UUID> foundIds = roles
        .stream()
        .map(Role::getId)
        .collect(Collectors.toSet());
      Set<UUID> missingIds = new HashSet<>(ids);
      missingIds.removeAll(foundIds);
      throw new EntityNotFoundException(
        t("user.error.role_not_found", missingIds)
      );
    }
    return new HashSet<>(roles);
  }

  // Realise l'intention metier ids to permissions.

  @Named("idsToPermissions")
  protected Set<Permission> idsToPermissions(Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return null;
    }
    List<Permission> permissions = permissionRepository.findAllById(ids);
    if (permissions.size() != ids.size()) {
      Set<UUID> foundIds = permissions
        .stream()
        .map(Permission::getId)
        .collect(Collectors.toSet());
      Set<UUID> missingIds = new HashSet<>(ids);
      missingIds.removeAll(foundIds);
      throw new EntityNotFoundException(
        t("user.error.permission_not_found", missingIds)
      );
    }
    return new HashSet<>(permissions);
  }

  // Realise l'intention metier services to ids.

  @Named("servicesToIds")
  protected Set<UUID> servicesToIds(Set<ServiceEntity> services) {
    if (services == null) return null;
    return services
      .stream()
      .map(ServiceEntity::getId)
      .collect(Collectors.toSet());
  }

  // Realise l'intention metier ids to services.

  @Named("idsToServices")
  protected Set<ServiceEntity> idsToServices(Set<UUID> ids) {
    if (ids == null) {
      return null;
    }
    if (ids.isEmpty()) {
      return new HashSet<>();
    }
    List<ServiceEntity> services = serviceRepository.findAllById(ids);
    if (services.size() != ids.size()) {
      Set<UUID> foundIds = services
        .stream()
        .map(ServiceEntity::getId)
        .collect(Collectors.toSet());
      Set<UUID> missingIds = new HashSet<>(ids);
      missingIds.removeAll(foundIds);
      throw new EntityNotFoundException(
        t("user.error.service_not_found", missingIds)
      );
    }
    return new HashSet<>(services);
  }

  // Realise l'intention metier agency to user reponse.

  @Named("agencyToUserResponse")
  protected AgencyResponse agencyToUserResponse(Agency agency) {
    if (agency == null) {
      return null;
    }

    AgencyResponse response = new AgencyResponse();
    response.setId(agency.getId());
    response.setCreatedAt(agency.getCreatedAt());
    response.setUpdatedAt(agency.getUpdatedAt());
    response.setModifiedBy(agency.getModifiedBy());
    response.setName(agency.getName());
    response.setCode(agency.getCode());
    response.setAddress(agency.getAddress());
    response.setActive(agency.isActive());
    return response;
  }

  // Realise l'intention metier service to user reponse.

  @Named("serviceToUserResponse")
  protected ServiceResponse serviceToUserResponse(ServiceEntity service) {
    if (service == null) {
      return null;
    }

    ServiceResponse response = new ServiceResponse();
    response.setId(service.getId());
    response.setCreatedAt(service.getCreatedAt());
    response.setUpdatedAt(service.getUpdatedAt());
    response.setModifiedBy(service.getModifiedBy());
    response.setName(service.getName());
    response.setDescription(service.getDescription());
    response.setActive(service.isActive());
    // Expose le chef du service pour que le front puisse signaler, sur la
    // page Mon Profil , que l'utilisateur est lui-mme le chef de ce
    // service (badge Chef du service ).
    if (service.getHeadOfService() != null) {
      User head = service.getHeadOfService();
      UserSummaryResponse headSummary = new UserSummaryResponse();
      headSummary.setId(head.getId());
      headSummary.setUsername(head.getUsername());
      headSummary.setFirstName(head.getFirstName());
      headSummary.setLastName(head.getLastName());
      response.setHeadOfService(headSummary);
    }
    return response;
  }

  // Realise l'intention metier chaine to phone number.

  @Named("stringToPhoneNumber")
  protected Long stringToPhoneNumber(String phoneNumber) {
    if (phoneNumber == null) {
      return null;
    }
    try {
      return Long.parseLong(phoneNumber);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
