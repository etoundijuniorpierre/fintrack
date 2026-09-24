// Controleur REST : expose les operations HTTP liees a department.

package com.fintrack.user.controller;

import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.request.ServiceRequest;
import com.fintrack.user.model.dto.response.ServiceResponse;
import com.fintrack.user.model.mapper.ServiceMapper;
import com.fintrack.user.service.DepartmentService;
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

// Controleur REST de gestion des departements/services internes (CRUD et affectation d'un responsable).
@RestController
@RequestMapping(ApiConstants.Endpoints.DEPARTMENTS)
@Tag(
  name = "Department Management",
  description = "Endpoints for managing internal departments/services"
)
@RequiredArgsConstructor
public class DepartmentController {

  private final DepartmentService departmentService;
  private final ServiceMapper serviceMapper;

  // Liste paginee des departements.
  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_AGENCY', 'USER_VIEW_ALL', 'SETTINGS_SYSTEM')"
  )
  @Operation(summary = "Get all departments with pagination")
  public ResponseEntity<Page<ServiceResponse>> getAllDepartments(
    Pageable pageable
  ) {
    return ResponseEntity.ok(
      departmentService
        .findAll(pageable)
        .map(serviceMapper::serviceToServiceResponse)
    );
  }

  // Liste complete (non paginee) des departements.
  @GetMapping("/all")
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_AGENCY', 'USER_VIEW_ALL', 'SETTINGS_SYSTEM', 'INCIDENT_CREATE')"
  )
  @Operation(summary = "Get all departments as a list")
  public ResponseEntity<List<ServiceResponse>> getAllDepartmentsList() {
    return ResponseEntity.ok(
      departmentService
        .findAll()
        .stream()
        .map(serviceMapper::serviceToServiceResponse)
        .toList()
    );
  }

  // Recupere un departement par son identifiant.
  @GetMapping("/{id}")
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_AGENCY', 'USER_VIEW_ALL', 'SETTINGS_SYSTEM')"
  )
  @Operation(summary = "Get department by ID")
  public ResponseEntity<ServiceResponse> getDepartmentById(
    @PathVariable UUID id
  ) {
    return ResponseEntity.ok(
      serviceMapper.serviceToServiceResponse(departmentService.findById(id))
    );
  }

  // Cree un nouveau departement.
  @PostMapping
  @PreAuthorize("hasAuthority('SETTINGS_SYSTEM')")
  @Operation(summary = "Create a new department")
  public ResponseEntity<ServiceResponse> createDepartment(
    @Valid @RequestBody ServiceRequest request
  ) {
    return new ResponseEntity<>(
      serviceMapper.serviceToServiceResponse(
        departmentService.create(serviceMapper.serviceRequestToService(request))
      ),
      HttpStatus.CREATED
    );
  }

  // Met a jour un departement existant.
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('SETTINGS_SYSTEM')")
  @Operation(summary = "Update an existing department")
  public ResponseEntity<ServiceResponse> updateDepartment(
    @PathVariable UUID id,
    @Valid @RequestBody ServiceRequest request
  ) {
    return ResponseEntity.ok(
      serviceMapper.serviceToServiceResponse(
        departmentService.update(
          id,
          serviceMapper.serviceRequestToService(request)
        )
      )
    );
  }

  // Supprime un departement.
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('SETTINGS_SYSTEM')")
  @Operation(summary = "Delete a department")
  public ResponseEntity<Void> deleteDepartment(@PathVariable UUID id) {
    departmentService.delete(id);
    return ResponseEntity.noContent().build();
  }

  // Affecte un utilisateur comme responsable du departement.
  @PatchMapping("/{id}/head/{userId}")
  @PreAuthorize("hasAuthority('SETTINGS_SYSTEM')")
  @Operation(summary = "Assign a head to a department")
  public ResponseEntity<ServiceResponse> assignHead(
    @PathVariable UUID id,
    @PathVariable UUID userId
  ) {
    return ResponseEntity.ok(
      serviceMapper.serviceToServiceResponse(
        departmentService.assignHead(id, userId)
      )
    );
  }
}
