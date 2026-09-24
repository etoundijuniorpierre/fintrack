// Mapper : convertit les donnees liees a agency entre modeles.

package com.fintrack.user.model.mapper;

import com.fintrack.user.exception.EntityNotFoundException;
import com.fintrack.user.model.dto.request.AgencyRequest;
import com.fintrack.user.model.dto.response.AgencyResponse;
import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

// Mapper MapStruct pour convertir les informations d'agences.

@Mapper(
  componentModel = "spring",
  unmappedTargetPolicy = ReportingPolicy.IGNORE,
  uses = { UserMapper.class }
)
public abstract class AgencyMapper {

  @Autowired
  protected UserRepository userRepository;

  // Realise l'intention metier agency to agency reponse.

  @Mapping(target = "members", source = "users")
  public abstract AgencyResponse agencyToAgencyResponse(Agency agency);

  // Precise l'intention metier associee a agence.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "active", source = "active")
  @Mapping(
    target = "headOfAgency",
    source = "headUserId",
    qualifiedByName = "idToUser"
  )
  public abstract Agency agencyRequestToAgency(AgencyRequest request);

  // Applique le changement demande apres validation metier.

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "active", source = "active")
  @Mapping(
    target = "headOfAgency",
    source = "headUserId",
    qualifiedByName = "idToUser"
  )
  public abstract void updateAgencyFromRequest(
    AgencyRequest request,
    @MappingTarget Agency agency
  );

  // Realise l'intention metier agency liste to agency reponse liste.

  public abstract List<AgencyResponse> agencyListToAgencyResponseList(
    List<Agency> agencies
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
