// Controleur REST : expose les operations HTTP liees a enum.

package com.fintrack.incident.controller;

import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.model.constant.ActionType;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentCause;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.PeriodType;
import com.fintrack.incident.model.dto.response.EnumResponse;
import com.fintrack.incident.model.mapper.EnumMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Controleur REST renvoyant les valeurs d'enumerations metier avec libelles et descriptions traduits.
@RestController
@RequestMapping(ApiConstants.API_BASE_PATH + "/enums")
@Tag(
  name = "Enums",
  description = "Endpoints for retrieving translated enum values"
)
@RequiredArgsConstructor
public class EnumController {

  private final EnumMapper enumMapper;

  // Renvoie tous les statuts d'incident avec libelle et description traduits.
  @GetMapping("/incident-statuses")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all incident statuses with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getIncidentStatuses() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        IncidentStatus.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }

  // Renvoie tous les niveaux de criticite avec libelle et description traduits.
  @GetMapping("/criticalities")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all criticality levels with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getCriticalities() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        Criticality.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }

  // Renvoie tous les types d'action avec libelle et description traduits.
  @GetMapping("/action-types")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all action types with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getActionTypes() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        ActionType.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }

  // Renvoie tous les types de periode avec libelle et description traduits.
  @GetMapping("/period-types")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all period types with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getPeriodTypes() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        PeriodType.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }

  // Renvoie toutes les causes d'incident avec libelle et description traduits.
  @GetMapping("/incident-causes")
  @PreAuthorize("isAuthenticated()")
  @Operation(
    summary = "Get all incident causes with translated name and description"
  )
  public ResponseEntity<List<EnumResponse>> getIncidentCauses() {
    return ResponseEntity.ok(
      enumMapper.toResponses(
        IncidentCause.values(),
        LocaleContextHolder.getLocale()
      )
    );
  }
}
