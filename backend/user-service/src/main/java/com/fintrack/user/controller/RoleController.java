// Controleur REST : expose les operations HTTP liees a role.

package com.fintrack.user.controller;

import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.request.RoleRequest;
import com.fintrack.user.model.dto.response.RoleResponse;
import com.fintrack.user.model.mapper.RoleMapper;
import com.fintrack.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Controleur REST de gestion des roles et de leurs permissions (CRUD).
@RestController
@RequestMapping(ApiConstants.Endpoints.ROLES)
@Tag(
  name = "Role Management",
  description = "Endpoints for managing user roles and permissions"
)
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;
  private final RoleMapper roleMapper;

  // Liste paginee des roles.
  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'ROLE_ASSIGN', 'USER_VIEW_ALL', 'USER_CREATE_ALL_AGENT', 'USER_CREATE_AGENT_AGENCY', 'USER_CREATE_AGENT_SERVICE', 'USER_CREATE_CHEF_AGENCE', 'USER_CREATE_CHEF_SERVICE', 'USER_CREATE_ADMIN')"
  )
  @Operation(summary = "Get all roles with pagination")
  public ResponseEntity<Page<RoleResponse>> getAllRoles(Pageable pageable) {
    return ResponseEntity.ok(
      roleService.findAll(pageable).map(roleMapper::roleToRoleResponse)
    );
  }

  // Liste complete (non paginee) des roles.
  @GetMapping("/all")
  @PreAuthorize(
    "hasAnyAuthority('ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'ROLE_ASSIGN', 'USER_VIEW_ALL', 'USER_CREATE_ALL_AGENT', 'USER_CREATE_AGENT_AGENCY', 'USER_CREATE_AGENT_SERVICE', 'USER_CREATE_CHEF_AGENCE', 'USER_CREATE_CHEF_SERVICE', 'USER_CREATE_ADMIN')"
  )
  @Operation(summary = "Get all roles as a list")
  public ResponseEntity<List<RoleResponse>> getAllRolesList() {
    return ResponseEntity.ok(
      roleService
        .findAll()
        .stream()
        .map(roleMapper::roleToRoleResponse)
        .toList()
    );
  }

  // Recupere un role par son identifiant.
  @GetMapping("/{id}")
  @PreAuthorize(
    "hasAnyAuthority('ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'ROLE_ASSIGN', 'USER_VIEW_ALL', 'USER_CREATE_ALL_AGENT', 'USER_CREATE_AGENT_AGENCY', 'USER_CREATE_AGENT_SERVICE', 'USER_CREATE_CHEF_AGENCE', 'USER_CREATE_CHEF_SERVICE', 'USER_CREATE_ADMIN')"
  )
  @Operation(summary = "Get role by ID")
  public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID id) {
    return ResponseEntity.ok(
      roleMapper.roleToRoleResponse(roleService.findById(id))
    );
  }

  // Cree un nouveau role.
  @PostMapping
  @PreAuthorize("hasAuthority('ROLE_CREATE')")
  @Operation(summary = "Create a new role")
  public ResponseEntity<RoleResponse> createRole(
    @Valid @RequestBody RoleRequest request
  ) {
    return new ResponseEntity<>(
      roleMapper.roleToRoleResponse(
        roleService.create(roleMapper.roleRequestToRole(request))
      ),
      HttpStatus.CREATED
    );
  }

  // Met a jour un role existant.
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('ROLE_UPDATE')")
  @Operation(summary = "Update an existing role")
  public ResponseEntity<RoleResponse> updateRole(
    @PathVariable UUID id,
    @Valid @RequestBody RoleRequest request
  ) {
    return ResponseEntity.ok(
      roleMapper.roleToRoleResponse(
        roleService.update(id, roleMapper.roleRequestToRole(request))
      )
    );
  }

  // Supprime un role.
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('ROLE_DELETE')")
  @Operation(summary = "Delete a role")
  public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
    roleService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
