package com.fintrack.incident.controller;

import com.fintrack.incident.model.constant.IncidentAction;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.security.IncidentAccessGuard;
import com.fintrack.incident.security.IncidentWorkflowGuard;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentCommentService;
import com.fintrack.incident.service.IncidentService;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidentService/incidents")
@RequiredArgsConstructor
public class IncidentAttachmentAccessController {
  private final IncidentService incidentService;
  private final IncidentCommentService commentService;
  private final IncidentAccessGuard accessGuard;
  private final IncidentWorkflowGuard workflowGuard;

  @GetMapping("/{id}/attachment-access")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Boolean> authorize(@PathVariable UUID id,
    @RequestParam String category, @RequestParam(required = false) UUID commentId,
    @AuthenticationPrincipal UserDetailsImpl user) {
    var incident = incidentService.findById(id);
    accessGuard.assertCanView(incident, user);
    if (IncidentStatus.TERMINAL_STATUSES.contains(incident.getStatus())) {
      throw new AccessDeniedException("Les pieces jointes de cet incident sont verrouillees");
    }
    String normalized = category.toUpperCase(Locale.ROOT);
    if ("COMMENT".equals(normalized)) {
      if (commentId == null) throw new AccessDeniedException("Commentaire requis");
      commentService.assertCanModifyAttachments(id, commentId, user.getId());
    } else {
      if (commentId != null) throw new AccessDeniedException("Categorie incoherente");
      IncidentAction action = switch (normalized) {
        case "INCIDENT", "INCIDENT_ATTACHMENT" -> IncidentAction.UPDATE;
        case "SOLUTION" -> IncidentAction.SUBMIT_SOLUTION;
        case "TREATMENT" -> IncidentAction.TREAT;
        case "RESOLUTION" -> IncidentAction.RESOLVE;
        case "UNRESOLVED" -> IncidentAction.MARK_UNRESOLVED;
        case "CLOSURE" -> IncidentAction.CLOSE;
        case "CANCELLATION" -> IncidentAction.CANCEL;
        default -> throw new AccessDeniedException("Categorie de piece jointe inconnue");
      };
      String permission = switch (action) {
        case UPDATE -> "INCIDENT_UPDATE";
        case SUBMIT_SOLUTION, TREAT -> "INCIDENT_TREAT";
        case RESOLVE, MARK_UNRESOLVED -> "INCIDENT_RESOLVE";
        case CLOSE -> "INCIDENT_CLOSE";
        case CANCEL -> "INCIDENT_CANCEL";
        default -> throw new AccessDeniedException("Action interdite");
      };
      if (!accessGuard.hasAuthority(user, permission)) {
        throw new AccessDeniedException("Permission requise : " + permission);
      }
      workflowGuard.assertCanAct(incident, user, action);
    }
    return ResponseEntity.ok(true);
  }
}
