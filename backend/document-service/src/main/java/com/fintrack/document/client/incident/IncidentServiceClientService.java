// Client inter-services : communique avec les services externes lies a incident service client.

package com.fintrack.document.client.incident;

import com.fintrack.document.client.incident.dto.IncidentClientResponse;
import com.fintrack.document.exception.BusinessRuleViolationException;
import com.fintrack.document.exception.ErrorCode;
import com.fintrack.document.model.dto.response.IncidentSummaryResponse;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

// Service client Feign pour communiquer avec le microservice des incidents.

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceClientService {

  // Statuts terminaux (cloture / rejet / annulation).
  private static final Set<String> TERMINAL_STATUSES = Set.of(
    "CLOSED",
    "REJECTED",
    "CANCELLED"
  );

  // Statuts ou le createur peut modifier une PJ de declaration.
  private static final Set<String> INCIDENT_ATTACHMENT_EDITABLE_STATUSES = Set.of(
    "OPEN",
    "PENDING_VALIDATION",
    "REOPENED"
  );

  private final IncidentServiceClient incidentServiceClient;

  public void assertAttachmentAllowed(UUID incidentId, String category, UUID commentId) {
    if (!Boolean.TRUE.equals(incidentServiceClient.authorizeAttachment(incidentId, category, commentId))) {
      throw new BusinessRuleViolationException(ErrorCode.PERMISSION_DENIED,
        "Operation interdite sur cette piece jointe");
    }
  }

  @Cacheable(
    value = "incidents",
    key = "#id",
    condition = "#id != null",
    unless = "#result == null"
  )
  // Resout un incident distant depuis l'incident-service.
  public IncidentSummaryResponse resolveIncident(UUID id) {
    if (id == null) return null;
    log.debug("Résolution de l'incident {} depuis incident-service", id);
    IncidentClientResponse r;
    try {
      r = incidentServiceClient.getIncidentById(id);
    } catch (RuntimeException ex) {
      log.warn(
        "Impossible de résoudre l'incident {} depuis incident-service",
        id,
        ex
      );
      return IncidentSummaryResponse.builder().id(id).title("unknown").build();
    }
    if (r == null) return null;
    return IncidentSummaryResponse.builder()
      .id(r.getId())
      .title(r.getTitle() != null ? r.getTitle() : "unknown")
      .status(r.getStatus())
      .build();
  }

  // Verifie que les regles metier autorisent l'operation sur incident.
  public void assertIncidentAccessible(UUID incidentId) {
    fetchIncidentOrDeny(incidentId);
  }

  // Autorise la modification des PJ de declaration : createur, statut editable.
  public void assertCanModifyAttachments(UUID incidentId, UUID currentUserId) {
    IncidentClientResponse incident = fetchIncidentOrDeny(incidentId);

    if (
      currentUserId == null ||
      !currentUserId.equals(incident.getCreatedById())
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.PERMISSION_DENIED,
        "Only the incident creator can modify attachments"
      );
    }
    if (
      incident.getStatus() == null ||
      !INCIDENT_ATTACHMENT_EDITABLE_STATUSES.contains(incident.getStatus())
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.PERMISSION_DENIED,
        "Attachments are locked once the incident is being handled"
      );
    }
  }

  // Autorise la suppression d'une piece jointe.
  // Exige l'acces a l'incident (non terminal) et que l'utilisateur soit uploader, createur ou assigne.
  public void assertCanDeleteAttachment(
    UUID incidentId,
    UUID uploadedBy,
    UUID currentUserId
  ) {
    IncidentClientResponse incident = fetchIncidentOrDeny(incidentId);

    if (
      incident.getStatus() == null ||
      TERMINAL_STATUSES.contains(incident.getStatus())
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.PERMISSION_DENIED,
        "Cannot delete attachments on a closed, rejected or cancelled incident"
      );
    }

    boolean isUploader = currentUserId != null && currentUserId.equals(uploadedBy);
    boolean isCreator = currentUserId != null && currentUserId.equals(incident.getCreatedById());
    boolean isAssignee = currentUserId != null && currentUserId.equals(incident.getAssignedToId());

    if (!isUploader && !isCreator && !isAssignee) {
      throw new BusinessRuleViolationException(
        ErrorCode.PERMISSION_DENIED,
        "Vous n'avez pas la permission de supprimer cette pièce jointe"
      );
    }
  }

  // Autorise l'ajout d'une PJ de commentaire : acces a l'incident, non terminal.
  public void assertCanAttachToComment(UUID incidentId) {
    IncidentClientResponse incident = fetchIncidentOrDeny(incidentId);
    if (
      incident.getStatus() == null ||
      TERMINAL_STATUSES.contains(incident.getStatus())
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.PERMISSION_DENIED,
        "Comments are locked once the incident is closed or rejected"
      );
    }
  }

  // Autorise l'ajout d'une PJ de traitement / resolution / cloture / solution.
  public void assertCanAttachToResolution(
    UUID incidentId,
    UUID currentUserId
  ) {
    IncidentClientResponse incident = fetchIncidentOrDeny(incidentId);
    if (
      incident.getStatus() == null ||
      TERMINAL_STATUSES.contains(incident.getStatus())
    ) {
      throw new BusinessRuleViolationException(
        ErrorCode.PERMISSION_DENIED,
        "Resolution attachments are locked once the incident is closed, rejected or cancelled"
      );
    }
  }

  // Recupere incident or deny.

  private IncidentClientResponse fetchIncidentOrDeny(UUID incidentId) {
    if (incidentId == null) {
      throw new BusinessRuleViolationException(
        ErrorCode.INVALID_INPUT,
        "L'identifiant de l'incident est requis"
      );
    }
    IncidentClientResponse r = incidentServiceClient.getIncidentById(
      incidentId
    );
    if (r == null || "UNKNOWN".equals(r.getStatus())) {
      throw new BusinessRuleViolationException(
        ErrorCode.PERMISSION_DENIED,
        "Access denied to incident " + incidentId
      );
    }
    return r;
  }
}
