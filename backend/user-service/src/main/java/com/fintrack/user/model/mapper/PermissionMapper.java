// Mapper : convertit les donnees liees a permission entre modeles.

package com.fintrack.user.model.mapper;

import com.fintrack.user.model.dto.response.PermissionResponse;
import com.fintrack.user.model.entity.Permission;
import java.util.List;
import org.mapstruct.*;

// Mapper MapStruct pour les habilitations (lecture seule).

@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PermissionMapper {
  // Realise l'intention metier permission to permission reponse.

  PermissionResponse permissionToPermissionResponse(Permission permission);
  // Realise l'intention metier permission liste to permission reponse liste.

  List<PermissionResponse> permissionListToPermissionResponseList(
    List<Permission> permissions
  );
}
