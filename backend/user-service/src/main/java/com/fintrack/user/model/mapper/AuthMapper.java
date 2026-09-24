// Mapper : convertit les donnees liees a auth entre modeles.

package com.fintrack.user.model.mapper;

import com.fintrack.user.model.dto.response.AuthResponse;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.entity.User;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

// Mapper pour convertir les informations d'authentification.

@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AuthMapper {
  // Precise l'intention metier associee a auth.

  @Mapping(target = "token", ignore = true)
  @Mapping(target = "type", constant = "Bearer")
  @Mapping(target = "roles", source = "user", qualifiedByName = "extractRoles")
  @Mapping(
    target = "permissions",
    source = "user",
    qualifiedByName = "extractPermissions"
  )
  @Mapping(target = "serviceId", source = "service.id")
  @Mapping(target = "agencyId", source = "agency.id")
  @Mapping(
    target = "managedServiceIds",
    source = "managedServices",
    qualifiedByName = "extractManagedServiceIds"
  )
  @Mapping(target = "managedAgencyId", source = "managedAgency.id")
  AuthResponse userToAuthResponse(User user);

  @Named("extractRoles")
  // Extrait roles.
  static Set<String> extractRoles(User user) {
    if (user.getRoles() == null) return Set.of();
    return user
      .getRoles()
      .stream()
      .map(r -> r.getName().toLowerCase())
      .collect(Collectors.toSet());
  }

  @Named("extractManagedServiceIds")
  // Extrait managed service ids.
  static Set<UUID> extractManagedServiceIds(
    Set<ServiceEntity> managedServices
  ) {
    if (managedServices == null) return null;
    return managedServices
      .stream()
      .map(ServiceEntity::getId)
      .collect(Collectors.toSet());
  }

  @Named("extractPermissions")
  // Extrait permissions.
  static Set<String> extractPermissions(User user) {
    Set<String> permissions = new HashSet<>();
    if (user.getPermissions() != null) {
      user
        .getPermissions()
        .forEach(p -> permissions.add(p.getName().toUpperCase()));
    }
    if (user.getRoles() != null) {
      user
        .getRoles()
        .stream()
        .filter(r -> r.getPermissions() != null)
        .forEach(r ->
          r
            .getPermissions()
            .forEach(p -> permissions.add(p.getName().toUpperCase()))
        );
    }
    // Soustraction des permissions revoquees : (directes ∪ role) - revoquees.
    if (user.getRevokedPermissions() != null) {
      user
        .getRevokedPermissions()
        .forEach(p -> permissions.remove(p.getName().toUpperCase()));
    }
    return permissions;
  }
}
