// Controleur REST : expose les operations HTTP liees a incident type config.

package com.fintrack.incident.controller;

import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.model.dto.request.IncidentTypeConfigRequest;
import com.fintrack.incident.model.dto.response.IncidentTypeConfigResponse;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.mapper.IncidentTypeConfigMapper;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentTypeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Controleur REST CRUD pour gerer les configurations de types d'incidents
@Slf4j
@RestController
@RequestMapping(ApiConstants.Endpoints.INCIDENT_TYPE_CONFIGS)
@Tag(
  name = "Incident Type Configuration",
  description = "CRUD endpoints for managing incident type configurations"
)
@RequiredArgsConstructor
public class IncidentTypeConfigController {

  private final IncidentTypeConfigService incidentTypeConfigService;
  private final IncidentTypeConfigMapper incidentTypeConfigMapper;

  // Liste les types d'incident selon le perimetre demande.

  @GetMapping
  @PreAuthorize("hasAnyAuthority('SETTINGS_INCIDENT_TYPES', 'INCIDENT_CREATE')")
  @Operation(summary = "Get all incident type configs with pagination")
  public ResponseEntity<Page<IncidentTypeConfigResponse>> findAll(
    Pageable pageable
  ) {
    log.debug(
      "Requête REST pour récupérer tous les IncidentTypeConfigs avec pagination: {}",
      pageable
    );
    Page<IncidentTypeConfig> page = incidentTypeConfigService.findAll(pageable);
    return ResponseEntity.ok(page.map(incidentTypeConfigMapper::toResponse));
  }

  // Liste les types d'incident selon le perimetre demande.

  @GetMapping("/all")
  @PreAuthorize(
    "hasAnyAuthority('SETTINGS_INCIDENT_TYPES', 'INCIDENT_CREATE', 'INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY')"
  )
  @Operation(summary = "Get all incident type configs as list")
  public ResponseEntity<List<IncidentTypeConfigResponse>> findAll() {
    log.debug("Requête REST pour récupérer tous les IncidentTypeConfigs");
    List<IncidentTypeConfigResponse> responses = incidentTypeConfigService
      .findAll()
      .stream()
      .map(incidentTypeConfigMapper::toResponse)
      .collect(Collectors.toList());
    return ResponseEntity.ok(responses);
  }

  // Recherche les types incident par identifiant.

  @GetMapping("/{id}")
  @PreAuthorize(
    "hasAnyAuthority('SETTINGS_INCIDENT_TYPES', 'INCIDENT_CREATE', 'INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY')"
  )
  @Operation(summary = "Get incident type config by ID")
  public ResponseEntity<IncidentTypeConfigResponse> findById(
    @PathVariable UUID id
  ) {
    log.debug("Requête REST pour récupérer IncidentTypeConfig par id: {}", id);
    IncidentTypeConfig config = incidentTypeConfigService.findById(id);
    return ResponseEntity.ok(incidentTypeConfigMapper.toResponse(config));
  }

  // Recherche les types incident par nom.

  @GetMapping("/name/{name}")
  @PreAuthorize(
    "hasAnyAuthority('SETTINGS_INCIDENT_TYPES', 'INCIDENT_CREATE', 'INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY')"
  )
  @Operation(summary = "Get incident type config by name")
  public ResponseEntity<IncidentTypeConfigResponse> findByName(
    @PathVariable String name
  ) {
    log.debug(
      "Requête REST pour récupérer IncidentTypeConfig par name: {}",
      name
    );
    IncidentTypeConfig config = incidentTypeConfigService.findByName(name);
    return ResponseEntity.ok(incidentTypeConfigMapper.toResponse(config));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('SETTINGS_INCIDENT_TYPES')")
  @Operation(summary = "Create a new incident type config")
  // Prepare l'ajout de type d'incident apres validation metier.
  public ResponseEntity<IncidentTypeConfigResponse> create(
    @Valid @RequestBody IncidentTypeConfigRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    log.debug("Requête REST pour créer IncidentTypeConfig: {}", request);
    IncidentTypeConfig entity = incidentTypeConfigMapper.toEntity(request);
    IncidentTypeConfig created = incidentTypeConfigService.create(
      entity,
      currentUser.getId()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(
      incidentTypeConfigMapper.toResponse(created)
    );
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('SETTINGS_INCIDENT_TYPES')")
  @Operation(summary = "Update an incident type config")
  // Applique une modification contrelee sur type d'incident.
  public ResponseEntity<IncidentTypeConfigResponse> update(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentTypeConfigRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    log.debug(
      "Requête REST pour mettre à jour IncidentTypeConfig avec id {}: {}",
      id,
      request
    );
    IncidentTypeConfig existing = incidentTypeConfigService.findById(id);
    incidentTypeConfigMapper.updateEntityFromRequest(request, existing);
    IncidentTypeConfig updated = incidentTypeConfigService.update(
      id,
      existing,
      currentUser.getId()
    );
    return ResponseEntity.ok(incidentTypeConfigMapper.toResponse(updated));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('SETTINGS_INCIDENT_TYPES')")
  @Operation(summary = "Delete an incident type config")
  // Retire les types d'incident cibles apres contrele metier.
  public ResponseEntity<Void> delete(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    log.debug("Requête REST pour supprimer IncidentTypeConfig avec id: {}", id);
    incidentTypeConfigService.delete(id, currentUser.getId());
    return ResponseEntity.noContent().build();
  }
}
