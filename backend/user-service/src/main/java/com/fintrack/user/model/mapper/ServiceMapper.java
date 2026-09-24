// Mapper : convertit les donnees liees a service entre modeles.

package com.fintrack.user.model.mapper;

import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.dto.request.ServiceRequest;
import com.fintrack.user.model.dto.response.ServiceResponse;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

// Assure les conversions du domaine service.

@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE,
  uses = { UserMapper.class }
)
public abstract class ServiceMapper {

  @Autowired
  protected UserRepository userRepository;

  // Realise l'intention metier service to service reponse.

  @Mapping(target = "members", source = "users")
  public abstract ServiceResponse serviceToServiceResponse(
    ServiceEntity service
  );

  // Precise l'intention metier associee a service.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "active", source = "active")
  @Mapping(
    target = "headOfService",
    source = "headUserId",
    qualifiedByName = "idToUser"
  )
  public abstract ServiceEntity serviceRequestToService(ServiceRequest request);

  // Applique le changement demande apres validation metier.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "active", source = "active")
  @Mapping(
    target = "headOfService",
    source = "headUserId",
    qualifiedByName = "idToUser"
  )
  public abstract void updateServiceFromRequest(
    ServiceRequest request,
    @MappingTarget ServiceEntity service
  );

  // Realise l'intention metier service liste to service reponse liste.

  public abstract List<ServiceResponse> serviceListToServiceResponseList(
    List<ServiceEntity> services
  );

  // Realise l'intention metier id to user.

  @Named("idToUser")
  protected User idToUser(UUID id) {
    return id == null
      ? null
      : userRepository
          .findById(id)
          .orElseThrow(() ->
            new EntityNotFoundException(
              "Utilisateur introuvable avec l'identifiant : " + id
            )
          );
  }
}
