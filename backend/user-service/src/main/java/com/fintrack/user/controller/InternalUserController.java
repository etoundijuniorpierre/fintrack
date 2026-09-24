// Controleur REST : expose les operations HTTP liees a internal user.

package com.fintrack.user.controller;

import com.fintrack.user.model.dto.response.AgencyInternalResponse;
import com.fintrack.user.model.dto.response.ServiceInternalResponse;
import com.fintrack.user.model.dto.response.UserInternalResponse;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.entity.Agency;
import com.fintrack.user.model.entity.ServiceEntity;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.mapper.UserInternalMapper;
import com.fintrack.user.service.AgencyService;
import com.fintrack.user.service.DepartmentService;
import com.fintrack.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controleur REST des endpoints internes consommes par les autres microservices (utilisateurs, agences, departements).
@RestController
@RequestMapping("/api/v1/userService/internal")
@Tag(
  name = "Internal API",
  description = "Endpoints internes pour les appels inter-services"
)
@RequiredArgsConstructor
public class InternalUserController {

  private final UserService userService;
  private final AgencyService agencyService;
  private final DepartmentService departmentService;
  private final UserInternalMapper userInternalMapper;

  // Retourne un utilisateur par son identifiant (vue interne allegee).
  @GetMapping("/users/{id}")
  @Operation(
    summary = "Get user by ID (internal)",
    description = "Endpoint interne pour l'incident-service"
  )
  public ResponseEntity<UserInternalResponse> getUserById(
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(
      userInternalMapper.toInternalResponse(userService.findById(id))
    );
  }

  // Retourne les utilisateurs actifs utiles aux regroupements de rapports.
  @GetMapping("/users/report-subjects")
  @Operation(summary = "List active report subjects (internal)")
  public ResponseEntity<List<UserInternalResponse>> getReportSubjects() {
    return ResponseEntity.ok(
      userService
        .findActiveReportSubjects()
        .stream()
        .map(userInternalMapper::toReportSubjectResponse)
        .toList()
    );
  }

  // Retourne les agences actives utiles aux regroupements de rapports.
  @GetMapping("/agencies")
  @Operation(summary = "List active agencies (internal)")
  public ResponseEntity<List<AgencyInternalResponse>> getAgencies() {
    return ResponseEntity.ok(
      agencyService
        .findAll()
        .stream()
        .filter(Agency::isActive)
        .map(userInternalMapper::toAgencyInternalResponse)
        .toList()
    );
  }

  // Retourne les services actifs utiles aux regroupements de rapports.
  @GetMapping("/departments")
  @Operation(summary = "List active departments (internal)")
  public ResponseEntity<List<ServiceInternalResponse>> getDepartments() {
    return ResponseEntity.ok(
      departmentService
        .findAll()
        .stream()
        .filter(ServiceEntity::isActive)
        .map(userInternalMapper::toServiceInternalResponse)
        .toList()
    );
  }

  // Retourne les IDs des utilisateurs inactifs (qualite des donnees : filtre
  // "incidents assignes a un utilisateur inactif" cote incident-service).
  @GetMapping("/users/inactive-ids")
  @Operation(summary = "List inactive user IDs (internal)")
  public ResponseEntity<List<UUID>> getInactiveUserIds() {
    return ResponseEntity.ok(userService.findInactiveIds());
  }

  // Retourne une agence par son identifiant (vue interne allegee).
  @GetMapping("/agencies/{id}")
  @Operation(
    summary = "Get agency by ID (internal)",
    description = "Endpoint interne pour l'incident-service"
  )
  public ResponseEntity<AgencyInternalResponse> getAgencyById(
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(
      userInternalMapper.toAgencyInternalResponse(agencyService.findById(id))
    );
  }

  // Retourne un departement par son identifiant (vue interne allegee).
  @GetMapping("/departments/{id}")
  @Operation(
    summary = "Get department by ID (internal)",
    description = "Endpoint interne pour l'incident-service"
  )
  public ResponseEntity<ServiceInternalResponse> getDepartmentById(
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(
      userInternalMapper.toServiceInternalResponse(
        departmentService.findById(id)
      )
    );
  }

  // Retourne le responsable d'un departement, ou 204 si aucun responsable n'est defini.
  @GetMapping("/departments/{id}/head")
  @Operation(
    summary = "Get head user of a department (internal)",
    description = "Retourne le chef du service/département."
  )
  public ResponseEntity<UserInternalResponse> getDepartmentHead(
    @PathVariable UUID id
  ) {
    ServiceEntity service = departmentService.findByIdWithHead(id);
    if (service.getHeadOfService() == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(
      userInternalMapper.toInternalResponse(service.getHeadOfService())
    );
  }

  // Retourne le responsable d'une agence, ou 204 si aucun responsable n'est defini.
  @GetMapping("/agencies/{id}/head")
  @Operation(
    summary = "Get head user of an agency (internal)",
    description = "Retourne le chef de l'agence."
  )
  public ResponseEntity<UserInternalResponse> getAgencyHead(
    @PathVariable UUID id
  ) {
    Agency agency = agencyService.findById(id);
    User head = agency.getHeadOfAgency();
    if (head == null || !head.isActive()) {
      List<User> agencyHeads = userService
        .findByAgencyId(id)
        .stream()
        .filter(User::isActive)
        .filter(user -> user.hasRole(RoleConstants.CHEF_AGENCE.getName()))
        .toList();
      if (agencyHeads.size() == 1) {
        head = agencyHeads.getFirst();
      } else {
        head = null;
      }
    }
    if (head == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(
      userInternalMapper.toInternalResponse(head)
    );
  }

  // Retourne la liste des utilisateurs ADMIN / SUPER_ADMIN.
  @GetMapping("/users/admins")
  @Operation(
    summary = "Get all admins (internal)",
    description = "Retourne les utilisateurs ADMIN / SUPER_ADMIN."
  )
  public ResponseEntity<List<UserInternalResponse>> getAdmins() {
    return ResponseEntity.ok(
      userService
        .findAdmins()
        .stream()
        .map(userInternalMapper::toInternalResponse)
        .toList()
    );
  }
}
