// Controleur REST : expose les operations HTTP liees a incident comment.

package com.fintrack.incident.controller;

import com.fintrack.incident.constant.ApiConstants;
import com.fintrack.incident.exception.BusinessRuleViolationException;
import com.fintrack.incident.exception.ErrorCode;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.dto.request.IncidentCommentRequest;
import com.fintrack.incident.model.dto.response.IncidentCommentResponse;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.mapper.IncidentCommentMapper;
import com.fintrack.incident.security.IncidentAccessGuard;
import com.fintrack.incident.security.UserDetailsImpl;
import com.fintrack.incident.service.IncidentCommentService;
import com.fintrack.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Controleur REST pour consulter et ajouter des commentaires sur un incident
@RestController
@RequestMapping(ApiConstants.Endpoints.INCIDENT_COMMENTS)
@Tag(
  name = "Incident Comments",
  description = "Endpoints for managing incident comments"
)
@RequiredArgsConstructor
public class IncidentCommentController {

  // Un incident sorti du circuit fige ses echanges : les commentaires existants
  // restent lisibles, aucun ne s'ajoute. Derive de la reference unique -- reenumerer
  // CLOTURE et REJETE laissait un incident ANNULE accepter de nouveaux commentaires,
  // alors que son commentaire disait deja "terminal".
  private static final Set<IncidentStatus> COMMENT_LOCKED_STATUSES =
    IncidentStatus.TERMINAL_STATUSES;

  private final IncidentCommentService incidentCommentService;
  private final IncidentCommentMapper incidentCommentMapper;
  private final IncidentService incidentService;
  private final IncidentAccessGuard incidentAccessGuard;

  @GetMapping("/{id}/comments")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN', 'INCIDENT_TREAT')"
  )
  @Operation(summary = "Get all comments for a specific incident")
  // Fournit incident comments au cas d usage appelant.
  public ResponseEntity<List<IncidentCommentResponse>> getIncidentComments(
    @PathVariable UUID id,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    incidentAccessGuard.assertCanView(
      incidentService.findById(id),
      currentUser
    );
    return ResponseEntity.ok(
      incidentCommentMapper.toResponseList(
        incidentCommentService.findByIncidentId(id)
      )
    );
  }

  @PostMapping("/{id}/comments")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN', 'INCIDENT_TREAT')"
  )
  @Operation(summary = "Add a comment to an incident")
  // Prepare l'ajout de commentaire d'incident apres validation metier.
  public ResponseEntity<IncidentCommentResponse> addComment(
    @PathVariable UUID id,
    @Valid @RequestBody IncidentCommentRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident incident = incidentService.findById(id);
    incidentAccessGuard.assertCanView(incident, currentUser);

    if (COMMENT_LOCKED_STATUSES.contains(incident.getStatus())) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_COMMENT_LOCKED,
        ErrorCode.INCIDENT_COMMENT_LOCKED.getMessageKey()
      );
    }

    return new ResponseEntity<>(
      incidentCommentMapper.toResponse(
        request.getParentCommentId() == null
          ? incidentCommentService.addComment(
              id,
              currentUser.getId(),
              request.getContent(),
              request.isInternal()
            )
          : incidentCommentService.addComment(
              id,
              currentUser.getId(),
              request.getContent(),
              request.isInternal(),
              request.getParentCommentId()
            )
      ),
      HttpStatus.CREATED
    );
  }

  @PutMapping("/{id}/comments/{commentId}")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN', 'INCIDENT_TREAT')"
  )
  @Operation(summary = "Update a comment on an incident")
  public ResponseEntity<IncidentCommentResponse> updateComment(
    @PathVariable UUID id,
    @PathVariable UUID commentId,
    @Valid @RequestBody IncidentCommentRequest request,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident incident = incidentService.findById(id);
    incidentAccessGuard.assertCanView(incident, currentUser);

    if (COMMENT_LOCKED_STATUSES.contains(incident.getStatus())) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_COMMENT_LOCKED,
        ErrorCode.INCIDENT_COMMENT_LOCKED.getMessageKey()
      );
    }

    return ResponseEntity.ok(
      incidentCommentMapper.toResponse(
        incidentCommentService.updateComment(
          id,
          commentId,
          currentUser.getId(),
          request.getContent()
        )
      )
    );
  }

  @DeleteMapping("/{id}/comments/{commentId}")
  @PreAuthorize(
    "hasAnyAuthority('INCIDENT_VIEW_ALL', 'INCIDENT_VIEW_AGENCY', 'INCIDENT_VIEW_SERVICE', 'INCIDENT_VIEW_OWN', 'INCIDENT_TREAT')"
  )
  @Operation(summary = "Delete a comment from an incident")
  public ResponseEntity<Void> deleteComment(
    @PathVariable UUID id,
    @PathVariable UUID commentId,
    @RequestParam(defaultValue = "false") boolean onlyIfEmpty,
    @AuthenticationPrincipal UserDetailsImpl currentUser
  ) {
    Incident incident = incidentService.findById(id);
    incidentAccessGuard.assertCanView(incident, currentUser);

    if (COMMENT_LOCKED_STATUSES.contains(incident.getStatus())) {
      throw new BusinessRuleViolationException(
        ErrorCode.INCIDENT_COMMENT_LOCKED,
        ErrorCode.INCIDENT_COMMENT_LOCKED.getMessageKey()
      );
    }

    if (onlyIfEmpty) {
      incidentCommentService.deleteCommentIfEmpty(id, commentId, currentUser.getId());
    } else {
      incidentCommentService.deleteComment(id, commentId, currentUser.getId());
    }
    return ResponseEntity.noContent().build();
  }
}
