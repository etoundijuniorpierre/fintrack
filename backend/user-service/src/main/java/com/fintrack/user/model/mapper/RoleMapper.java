// Mapper : convertit les donnees liees a role entre modeles.

package com.fintrack.user.model.mapper;

import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.dto.request.RoleRequest;
import com.fintrack.user.model.dto.response.RoleResponse;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.repository.PermissionRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

// Mapper MapStruct pour les roles et profils.

@Mapper(
  componentModel = "spring",
  uses = { PermissionMapper.class },
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class RoleMapper {

  @Autowired
  protected PermissionRepository permissionRepository;

  @Autowired
  protected MessageSource messageSource;

  protected String t(String key, Object... args) {
    return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
  }

  // Realise l'intention metier role to role reponse.

  public abstract RoleResponse roleToRoleResponse(Role role);

  // Realise l'intention metier role requete to role.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(
    target = "permissions",
    source = "permissionIds",
    qualifiedByName = "idsToPermissionsRole"
  )
  public abstract Role roleRequestToRole(RoleRequest request);

  // Realise l'intention metier ids to permissions.

  @Named("idsToPermissionsRole")
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
}
