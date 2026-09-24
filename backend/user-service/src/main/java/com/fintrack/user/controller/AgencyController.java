// Controleur REST : expose les operations HTTP liees a agency.

package com.fintrack.user.controller;

import com.fintrack.user.constant.ApiConstants;
import com.fintrack.user.model.dto.request.AgencyRequest;
import com.fintrack.user.model.dto.response.AgencyResponse;
import com.fintrack.user.model.mapper.AgencyMapper;
import com.fintrack.user.security.UserAccessGuard;
import com.fintrack.user.service.AgencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Controleur REST de gestion des agences (CRUD et affectation d'un responsable).
@RestController
@RequestMapping(ApiConstants.Endpoints.AGENCIES)
@Tag(
  name = "Agency Management",
  description = "Endpoints for managing agencies"
)
@RequiredArgsConstructor
public class AgencyController {

  private final AgencyService agencyService;
  private final AgencyMapper agencyMapper;
  private final UserAccessGuard userAccess;

  // Liste paginee des agences ; un utilisateur sans vue globale ne voit que sa propre agence.
  @GetMapping
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_AGENCY', 'USER_VIEW_SERVICE', 'USER_VIEW_ALL', 'SETTINGS_SYSTEM')"
  )
  @Operation(summary = "Get all agencies with pagination")
  public ResponseEntity<Page<AgencyResponse>> getAllAgencies(
    Pageable pageable
  ) {
    if (
      !userAccess.hasAuthority("USER_VIEW_ALL") &&
      !userAccess.hasAuthority("SETTINGS_SYSTEM")
    ) {
      return ResponseEntity.ok(
        userAccess
          .currentAgencyId()
          .<Page<AgencyResponse>>map(agencyId -> {
            AgencyResponse response = agencyMapper.agencyToAgencyResponse(
              agencyService.findById(agencyId)
            );
            return new PageImpl<>(List.of(response), pageable, 1);
          })
          .orElse(Page.empty(pageable))
      );
    }
    return ResponseEntity.ok(
      agencyService.findAll(pageable).map(agencyMapper::agencyToAgencyResponse)
    );
  }

  // Liste complete (non paginee) des agences ; restreinte a l'agence de l'utilisateur sans vue globale.
  @GetMapping("/all")
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_AGENCY', 'USER_VIEW_SERVICE', 'USER_VIEW_ALL', 'SETTINGS_SYSTEM')"
  )
  @Operation(summary = "Get all agencies as a list")
  public ResponseEntity<List<AgencyResponse>> getAllAgenciesList() {
    if (
      !userAccess.hasAuthority("USER_VIEW_ALL") &&
      !userAccess.hasAuthority("SETTINGS_SYSTEM")
    ) {
      return ResponseEntity.ok(
        userAccess
          .currentAgencyId()
          .map(agencyId ->
            List.of(
              agencyMapper.agencyToAgencyResponse(
                agencyService.findById(agencyId)
              )
            )
          )
          .orElse(List.of())
      );
    }
    return ResponseEntity.ok(
      agencyService
        .findAll()
        .stream()
        .map(agencyMapper::agencyToAgencyResponse)
        .toList()
    );
  }

  // Recupere une agence par son identifiant.
  @GetMapping("/{id}")
  @PreAuthorize(
    "hasAnyAuthority('USER_VIEW_AGENCY', 'USER_VIEW_SERVICE', 'USER_VIEW_ALL', 'SETTINGS_SYSTEM') and @userAccess.canViewAgency(#id)"
  )
  @Operation(summary = "Get agency by ID")
  public ResponseEntity<AgencyResponse> getAgencyById(@PathVariable UUID id) {
    return ResponseEntity.ok(
      agencyMapper.agencyToAgencyResponse(agencyService.findById(id))
    );
  }

  // Cree une nouvelle agence.
  @PostMapping
  @PreAuthorize("hasAuthority('SETTINGS_SYSTEM')")
  @Operation(summary = "Create a new agency")
  public ResponseEntity<AgencyResponse> createAgency(
    @Valid @RequestBody AgencyRequest request
  ) {
    return new ResponseEntity<>(
      agencyMapper.agencyToAgencyResponse(
        agencyService.create(agencyMapper.agencyRequestToAgency(request))
      ),
      HttpStatus.CREATED
    );
  }

  // Met a jour une agence existante.
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('SETTINGS_SYSTEM')")
  @Operation(summary = "Update an existing agency")
  public ResponseEntity<AgencyResponse> updateAgency(
    @PathVariable UUID id,
    @Valid @RequestBody AgencyRequest request
  ) {
    return ResponseEntity.ok(
      agencyMapper.agencyToAgencyResponse(
        agencyService.update(id, agencyMapper.agencyRequestToAgency(request))
      )
    );
  }

  // Supprime une agence.
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('SETTINGS_SYSTEM')")
  @Operation(summary = "Delete an agency")
  public ResponseEntity<Void> deleteAgency(@PathVariable UUID id) {
    agencyService.delete(id);
    return ResponseEntity.noContent().build();
  }

  // Affecte un utilisateur comme responsable de l'agence.
  @PatchMapping("/{id}/head/{userId}")
  @PreAuthorize("hasAuthority('SETTINGS_SYSTEM')")
  @Operation(summary = "Assign a head to an agency")
  public ResponseEntity<AgencyResponse> assignHead(
    @PathVariable UUID id,
    @PathVariable UUID userId
  ) {
    return ResponseEntity.ok(
      agencyMapper.agencyToAgencyResponse(agencyService.assignHead(id, userId))
    );
  }
}
