// Controleur REST : expose les operations HTTP liees a permission.

package com.fintrack.user.controller;

import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.response.PermissionResponse;
import com.fintrack.user.model.mapper.PermissionMapper;
import com.fintrack.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Controleur REST de consultation des permissions systeme (lecture seule).
// Les permissions sont definies en code (PermissionMatrix + enums par domaine) et
// seedees au demarrage : aucune creation/modification/suppression au runtime.
@RestController
@RequestMapping(ApiConstants.Endpoints.PERMISSIONS)
@Tag(
  name = "Permission Management",
  description = "Endpoints for reading system permissions"
)
@RequiredArgsConstructor
public class PermissionController {

  private final PermissionService permissionService;
  private final PermissionMapper permissionMapper;

  // Liste paginee des permissions.
  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'ROLE_ASSIGN', 'USER_VIEW_ALL')"
  )
  @Operation(summary = "Get all permissions with pagination")
  public ResponseEntity<Page<PermissionResponse>> getAllPermissions(
    Pageable pageable
  ) {
    return ResponseEntity.ok(
      permissionService
        .findAll(pageable)
        .map(permissionMapper::permissionToPermissionResponse)
    );
  }

  // Liste complete (non paginee) des permissions.
  @GetMapping("/all")
  @PreAuthorize(
    "hasAnyAuthority('ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'ROLE_ASSIGN', 'USER_VIEW_ALL')"
  )
  @Operation(summary = "Get all permissions as a list")
  public ResponseEntity<List<PermissionResponse>> getAllPermissionsList() {
    return ResponseEntity.ok(
      permissionService
        .findAll()
        .stream()
        .map(permissionMapper::permissionToPermissionResponse)
        .toList()
    );
  }

  // Recupere une permission par son identifiant.
  @GetMapping("/{id}")
  @PreAuthorize(
    "hasAnyAuthority('ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_DELETE', 'ROLE_ASSIGN', 'USER_VIEW_ALL')"
  )
  @Operation(summary = "Get permission by ID")
  public ResponseEntity<PermissionResponse> getPermissionById(
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(
      permissionMapper.permissionToPermissionResponse(
        permissionService.findById(id)
      )
    );
  }
}
