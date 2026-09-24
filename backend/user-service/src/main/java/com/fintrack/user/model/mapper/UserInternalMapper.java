// Mapper : convertit les donnees liees a user internal entre modeles.

package com.fintrack.user.model.mapper;

import com.fintrack.user.model.dto.response.AgencyInternalResponse;
import com.fintrack.user.model.dto.response.ServiceInternalResponse;
import com.fintrack.user.model.dto.response.UserInternalResponse;
import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.Permission;
import com.fintrack.user.model.entity.Role;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.entity.User;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

// Mapper MapStruct pour les informations utilisateur internes.

@Component
public class UserInternalMapper {

  // Convertit un utilisateur en contrat interne inter-services.
  public UserInternalResponse toInternalResponse(User user) {
    if (user == null) return null;

    UserInternalResponse response = new UserInternalResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setFirstName(user.getFirstName());
    response.setLastName(user.getLastName());
    response.setEmail(user.getEmail());
    response.setActive(user.isActive());

    Set<String> roleNames = new HashSet<>();
    if (user.getRoles() != null) {
      for (Role role : user.getRoles()) {
        if (role.getName() != null) {
          roleNames.add(role.getName());
        }
      }
    }
    response.setRoles(Collections.unmodifiableSet(roleNames));

    Set<String> permNames = new HashSet<>();
    if (user.getRoles() != null) {
      for (Role role : user.getRoles()) {
        if (role.getPermissions() != null) {
          for (Permission perm : role.getPermissions()) {
            if (perm.getName() != null) {
              permNames.add(perm.getName());
            }
          }
        }
      }
    }
    if (user.getPermissions() != null) {
      for (Permission perm : user.getPermissions()) {
        if (perm.getName() != null) {
          permNames.add(perm.getName());
        }
      }
    }
    // Soustraction des permissions revoquees : (directes ∪ role) - revoquees.
    if (user.getRevokedPermissions() != null) {
      for (Permission perm : user.getRevokedPermissions()) {
        if (perm.getName() != null) {
          permNames.remove(perm.getName());
        }
      }
    }
    response.setPermissions(Collections.unmodifiableSet(permNames));

    response.setAgencyId(
      user.getAgency() != null ? user.getAgency().getId() : null
    );
    response.setServiceId(
      user.getService() != null ? user.getService().getId() : null
    );

    return response;
  }

  // Convertit un utilisateur en sujet leger pour les regroupements de rapports.
  public UserInternalResponse toReportSubjectResponse(User user) {
    if (user == null) return null;

    UserInternalResponse response = new UserInternalResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setFirstName(user.getFirstName());
    response.setLastName(user.getLastName());
    response.setEmail(user.getEmail());
    response.setActive(user.isActive());
    response.setRoles(Set.of());
    response.setPermissions(Set.of());
    response.setAgencyId(
      user.getAgency() != null ? user.getAgency().getId() : null
    );
    response.setServiceId(
      user.getService() != null ? user.getService().getId() : null
    );
    return response;
  }

  // Convertit une agence en contrat interne inter-services.
  public AgencyInternalResponse toAgencyInternalResponse(Agency agency) {
    if (agency == null) return null;

    AgencyInternalResponse response = new AgencyInternalResponse();
    response.setId(agency.getId());
    response.setName(agency.getName());
    response.setCode(agency.getCode());
    response.setActive(agency.isActive());
    return response;
  }

  // Convertit un service en contrat interne inter-services.
  public ServiceInternalResponse toServiceInternalResponse(
    ServiceEntity service
  ) {
    if (service == null) return null;

    ServiceInternalResponse response = new ServiceInternalResponse();
    response.setId(service.getId());
    response.setName(service.getName());
    response.setDescription(service.getDescription());
    response.setActive(service.isActive());
    return response;
  }
}
