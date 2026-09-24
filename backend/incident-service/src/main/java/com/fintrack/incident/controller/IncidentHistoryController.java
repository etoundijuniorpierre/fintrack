// Controleur REST : expose les operations HTTP liees a incident history.

package com.fintrack.incident.controller;

import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.model.dto.response.IncidentHistoryResponse;
import com.fintrack.incident.model.mapper.IncidentHistoryMapper;
import com.fintrack.incident.security.IncidentAccessGuard;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentHistoryService;
import com.fintrack.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Controleur REST pour consulter l'historique des changements d'un incident
@RestController
@RequestMapping(ApiConstants.Endpoints.INCIDENT_HISTORY)
@Tag(
  name = "Incident History",
  description = "Endpoints for viewing incident history"
)
@RequiredArgsConstructor
public class IncidentHistoryController {

  private final IncidentHistoryService incidentHistoryService;
  private final IncidentHistoryMapper incidentHistoryMapper;
  private final IncidentService incidentService;
  private final IncidentAccessGuard incidentAccessGuard;

  @GetMapping("/{id}/history")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN', 'INCIDENT_TREAT')"
  )
  @Operation(summary = "Get history for a specific incident")
  // Fournit incident history au cas d usage appelant.
  public ResponseEntity<List<IncidentHistoryResponse>> getIncidentHistory(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    incidentAccessGuard.assertCanView(
      incidentService.findById(id),
      currentUser
    );
    return ResponseEntity.ok(
      incidentHistoryMapper.toResponseList(
        incidentHistoryService.findByIncidentId(id)
      )
    );
  }
}
