// Controleur REST : expose les operations HTTP liees a user.

package com.fintrack.user.controller;

import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.constant.role.RoleConstants;
import com.fintrack.user.model.dto.request.ChangePasswordRequest;
import com.fintrack.user.model.dto.request.ProfileUpdateRequest;
import com.fintrack.user.model.dto.request.UserRequest;
import com.fintrack.user.model.dto.response.UserResponse;
import com.fintrack.user.model.dto.response.UserSummaryResponse;
import com.fintrack.user.model.entity.User;
import com.fintrack.user.model.mapper.UserMapper;
import com.fintrack.user.security.UserAccessGuard;
import com.fintrack.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Controleur REST exposant les operations de gestion des utilisateurs.
@RestController
@RequestMapping(ApiConstants.Endpoints.USERS)
@Tag(
  name = "User Management",
  description = "Endpoints for managing FinTrack users"
)
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final UserMapper userMapper;
  private final UserAccessGuard userAccess;

  // Point d'acces GET pour recuperer des donnees.
  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_ALL', 'USER_VIEW_AGENCY', 'USER_VIEW_SERVICE', 'REPORT_GENERATE', 'REPORT_SEND_EMAIL')"
  )
  @Operation(
    summary = "Get all users with pagination",
    description = "Returns a paginated list of users"
  )
  public ResponseEntity<Page<UserResponse>> getAllUsers(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String role,
    @RequestParam(required = false) UUID agencyId,
    @RequestParam(required = false) UUID serviceId,
    @RequestParam(required = false) Boolean active,
    @RequestParam(required = false) Boolean connected,
    @RequestParam(required = false) Boolean locked,
    @RequestParam(required = false) String missingField,
    Pageable pageable
  ) {
    if (userAccess.hasAuthority("USER_VIEW_ALL")) {
      /// Filtres qualite (liens Super Admin) : listes filtrees dediees.
      if (Boolean.TRUE.equals(locked)) {
        return ResponseEntity.ok(
          userService
            .findLocked(pageable)
            .map(userMapper::userToAdminUserResponse)
        );
      }
      if ("role".equals(missingField)) {
        return ResponseEntity.ok(
          userService
            .findWithoutRole(pageable)
            .map(userMapper::userToAdminUserResponse)
        );
      }
      if ("scope".equals(missingField)) {
        return ResponseEntity.ok(
          userService
            .findWithoutScope(pageable)
            .map(userMapper::userToAdminUserResponse)
        );
      }
      // Un Super Admin voit tout le monde ; un Admin voit tous les autres
      // utilisateurs mais pas les comptes Super Admin.
      Page<UserResponse> users = userAccess.isSuperAdmin()
        ? userService
            .findFiltered(
              keyword,
              role,
              agencyId,
              serviceId,
              active,
              connected,
              pageable
            )
            .map(userMapper::userToAdminUserResponse)
        : userService
            .findFilteredExcludingRole(
              RoleConstants.SUPER_ADMIN.getName(),
              keyword,
              role,
              agencyId,
              serviceId,
              active,
              connected,
              pageable
            )
            .map(userMapper::userToAdminUserResponse);
      return ResponseEntity.ok(users);
    }
    if (userAccess.hasAuthority("USER_VIEW_AGENCY")) {
      return ResponseEntity.ok(
        userAccess
          .currentAgencyId()
          .map(scopeAgencyId ->
            userService
              .findFilteredByAgencyId(
                scopeAgencyId,
                keyword,
                role,
                agencyId,
                serviceId,
                active,
                connected,
                pageable
              )
              .map(userMapper::userToScopedUserResponse)
          )
          .orElse(Page.empty(pageable))
      );
    }
    if (userAccess.hasAuthority("USER_VIEW_SERVICE")) {
      return ResponseEntity.ok(
        userAccess
          .currentServiceId()
          .map(scopeServiceId ->
            userService
              .findFilteredByServiceId(
                scopeServiceId,
                keyword,
                role,
                agencyId,
                serviceId,
                active,
                connected,
                pageable
              )
              .map(userMapper::userToScopedUserResponse)
          )
          .orElse(Page.empty(pageable))
      );
    }
    return ResponseEntity.ok(Page.empty(pageable));
  }

  // Point d'acces GET pour recuperer des donnees.
  @GetMapping("/all")
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_ALL', 'USER_VIEW_AGENCY', 'USER_VIEW_SERVICE', 'INCIDENT_CREATE', 'REPORT_GENERATE', 'REPORT_SEND_EMAIL')"
  )
  @Operation(summary = "Get all users as a list")
  public ResponseEntity<List<UserResponse>> getAllUsersList() {
    if (userAccess.hasAuthority("USER_VIEW_ALL")) {
      List<User> users = userAccess.isSuperAdmin()
        ? userService.findAll()
        : userService.findAllExcludingRole(RoleConstants.SUPER_ADMIN.getName());
      return ResponseEntity.ok(
        users.stream().map(userMapper::userToAdminUserResponse).toList()
      );
    }
    if (userAccess.hasAuthority("USER_VIEW_AGENCY")) {
      return ResponseEntity.ok(
        userAccess
          .currentAgencyId()
          .map(agencyId ->
            userService
              .findByAgencyId(agencyId)
              .stream()
              .map(userMapper::userToScopedUserResponse)
              .toList()
          )
          .orElse(List.of())
      );
    }
    if (userAccess.hasAuthority("USER_VIEW_SERVICE")) {
      return ResponseEntity.ok(
        userAccess
          .currentServiceId()
          .map(serviceId ->
            userService
              .findByServiceId(serviceId)
              .stream()
              .map(userMapper::userToScopedUserResponse)
              .toList()
          )
          .orElse(List.of())
      );
    }
    return ResponseEntity.ok(List.of());
  }

  // Point d'acces GET pour recuperer les utilisateurs assignables au traitement/resolution d'incidents.
  @GetMapping("/assignable")
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_ALL', 'USER_VIEW_AGENCY', 'USER_VIEW_SERVICE', 'INCIDENT_ASSIGN')"
  )
  @Operation(summary = "Get assignable users for incident treatment")
  public ResponseEntity<List<UserSummaryResponse>> getAssignableUsers(
    @RequestParam(name = "serviceId", required = false) UUID serviceId
  ) {
    List<User> assignables = userService.findAssignableUsers(serviceId);
    return ResponseEntity.ok(
      assignables.stream().map(userMapper::userToUserSummaryResponse).toList()
    );
  }

  // Point d'acces GET pour recuperer les utilisateurs disposant de la permission VALIDATION_DIRECTION.
  @GetMapping("/validators")
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_ALL', 'USER_VIEW_AGENCY', 'USER_VIEW_SERVICE', 'SETTINGS_INCIDENT_TYPES', 'REPORT_GENERATE', 'INCIDENT_CREATE')"
  )
  @Operation(summary = "Get users with VALIDATION_DIRECTION permission")
  public ResponseEntity<List<UserSummaryResponse>> getDirectionValidators() {
    List<User> validators = userService.findDirectionValidators();
    return ResponseEntity.ok(
      validators.stream().map(userMapper::userToUserSummaryResponse).toList()
    );
  }

  // Point d'acces GET pour recuperer des donnees.
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('USER_VIEW_ALL') or @userAccess.canViewUser(#id)")
  @Operation(summary = "Get user by ID")
  public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
    return ResponseEntity.ok(
      toVisibleUserResponse(id, userService.findById(id))
    );
  }

  // Point d'acces POST pour creer une ressource.
  @PostMapping
  @PreAuthorize(
    "hasAnyAuthority('USER_CREATE_ALL_AGENT', 'USER_CREATE_AGENT_AGENCY', 'USER_CREATE_AGENT_SERVICE', 'USER_CREATE_CHEF_AGENCE', 'USER_CREATE_CHEF_SERVICE', 'USER_CREATE_ADMIN')"
  )
  @Operation(summary = "Create a new user")
  public ResponseEntity<UserResponse> createUser(
    @Valid @RequestBody UserRequest request
  ) {
    return ResponseEntity.ok(
      userMapper.userToAdminUserResponse(
        userService.create(userMapper.userRequestToUser(request))
      )
    );
  }

  // Point d'acces PUT pour mettre a jour une ressource.
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('USER_UPDATE', 'USER_UPDATE_AGENT')")
  @Operation(summary = "Update an existing user")
  public ResponseEntity<UserResponse> updateUser(
    @PathVariable UUID id,
    @Valid @RequestBody UserRequest request
  ) {
    return ResponseEntity.ok(
      userMapper.userToAdminUserResponse(
        userService.update(id, userMapper.userRequestToUser(request))
      )
    );
  }

  // Point d'acces DELETE pour supprimer une ressource.
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('USER_DELETE')")
  @Operation(summary = "Delete a user")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }

  // Point d'acces PATCH pour modifier partiellement une ressource.
  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAuthority('USER_UPDATE')")
  @Operation(summary = "Toggle user active status")
  public ResponseEntity<UserResponse> toggleStatus(@PathVariable UUID id) {
    return ResponseEntity.ok(
      userMapper.userToAdminUserResponse(userService.toggleStatus(id))
    );
  }

  // Point d'acces PATCH pour modifier partiellement une ressource.
  @PatchMapping("/{id}/agency/{agencyId}")
  @PreAuthorize("hasAuthority('USER_UPDATE')")
  @Operation(summary = "Assign user to an agency")
  public ResponseEntity<UserResponse> assignToAgency(
    @PathVariable UUID id,
    @PathVariable UUID agencyId
  ) {
    return ResponseEntity.ok(
      userMapper.userToAdminUserResponse(
        userService.assignToAgency(id, agencyId)
      )
    );
  }

  // Point d'acces PATCH pour modifier partiellement une ressource.
  @PatchMapping("/{id}/permissions")
  @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
  @Operation(
    summary = "Set multiple permissions for a user (overwrites existing individual permissions)"
  )
  public ResponseEntity<UserResponse> assignPermissions(
    @PathVariable UUID id,
    @RequestBody List<UUID> permissionIds
  ) {
    return ResponseEntity.ok(
      userMapper.userToAdminUserResponse(
        userService.assignPermissions(id, permissionIds)
      )
    );
  }

  // Point d'acces POST pour creer une ressource.
  @PostMapping("/{id}/permissions/{permissionId}")
  @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
  @Operation(summary = "Add a single specific permission to a user")
  public ResponseEntity<UserResponse> addPermission(
    @PathVariable UUID id,
    @PathVariable UUID permissionId
  ) {
    return ResponseEntity.ok(
      userMapper.userToAdminUserResponse(
        userService.addPermission(id, permissionId)
      )
    );
  }

  // Point d'acces DELETE pour supprimer une ressource.
  @DeleteMapping("/{id}/permissions/{permissionId}")
  @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
  @Operation(summary = "Remove a single specific permission from a user")
  public ResponseEntity<UserResponse> removePermission(
    @PathVariable UUID id,
    @PathVariable UUID permissionId
  ) {
    return ResponseEntity.ok(
      userMapper.userToAdminUserResponse(
        userService.removePermission(id, permissionId)
      )
    );
  }

  // Point d'acces POST pour creer une ressource.
  @PostMapping("/{id}/regenerate-password")
  @PreAuthorize("hasAuthority('USER_UPDATE')")
  @Operation(summary = "Regenerate user temporary password (admin only)")
  public ResponseEntity<UserResponse> regeneratePassword(
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(
      userMapper.userToAdminUserResponse(userService.regeneratePassword(id))
    );
  }

  // Point d'acces PATCH pour modifier partiellement une ressource.
  @PatchMapping("/{id}/profile")
  @PreAuthorize("hasAuthority('USER_UPDATE') or @userAccess.isSelf(#id)")
  @Operation(summary = "Update user profile")
  public ResponseEntity<UserResponse> updateProfile(
    @PathVariable UUID id,
    @Valid @RequestBody ProfileUpdateRequest request
  ) {
    return ResponseEntity.ok(
      toVisibleUserResponse(
        id,
        userService.updateProfile(
          id,
          userMapper.profileUpdateRequestToUser(request)
        )
      )
    );
  }

  // Point d'acces POST pour creer une ressource.
  @PostMapping("/{id}/change-password")
  @PreAuthorize("@userAccess.isSelf(#id)")
  @Operation(summary = "Change user password")
  public ResponseEntity<UserResponse> changePassword(
    @PathVariable UUID id,
    @Valid @RequestBody ChangePasswordRequest request
  ) {
    return ResponseEntity.ok(
      userMapper.userToUserResponse(
        userService.changePassword(
          id,
          request.getCurrentPassword(),
          request.getNewPassword()
        )
      )
    );
  }

  // Convertit les donnees du domaine utilisateur entre les modeles utilises.

  private UserResponse toVisibleUserResponse(UUID requestedId, User user) {
    if (userAccess.hasAuthority("USER_VIEW_ALL")) {
      return userMapper.userToAdminUserResponse(user);
    }
    if (userAccess.isSelf(requestedId)) {
      return userMapper.userToUserResponse(user);
    }
    return userMapper.userToScopedUserResponse(user);
  }
}
