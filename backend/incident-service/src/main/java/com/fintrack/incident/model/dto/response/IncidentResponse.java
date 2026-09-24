// DTO : transporte les donnees liees a incident entre les couches.

package com.fintrack.incident.model.dto.response;

import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentCause;
import com.fintrack.incident.model.constant.IncidentStatus;
import com.fintrack.incident.model.constant.IncidentValidatorRole;
import com.fintrack.incident.model.dto.BaseDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

// DTO de reponse contenant les informations completes d'un incident.

@Data
@EqualsAndHashCode(callSuper = true)
public class IncidentResponse extends BaseDto {

  private String title;
  private String description;
  private IncidentTypeConfigResponse type;
  private Criticality criticality;
  private IncidentStatus status;
  private LocalDateTime validatedAt;
  private LocalDateTime transferredAt;
  private LocalDateTime resolvedAt;
  private LocalDateTime closedAt;
  private LocalDateTime blockedAt;
  private LocalDateTime unblockedAt;
  private LocalDateTime reopenedAt;
  private LocalDateTime dueDate;
  private LocalDate incidentDate;
  private LocalDate observationDate;
  private UserSummaryResponse createdBy;
  private UserSummaryResponse validatedBy;
  private UserSummaryResponse assignedTo;
  private UserSummaryResponse blockedBy;
  private UserSummaryResponse reopenedBy;
  private ServiceSummaryResponse transferredToService;
  private AgencySummaryResponse transferredToAgency;
  private String transferReason;
  private String rejectReason;
  private String cancelReason;
  private String blockedReason;
  private String reopenReason;
  private int reopenCount;
  private Boolean isReopenExpired;
  private Boolean isMaxReopenReached;
  private Boolean canValidate;
  private Boolean canCancel;
  /** Attente prolongee : demande d'actualite en cours (null si aucune n'est attendue). */
  private LocalDateTime confirmationRequestedAt;
  private Boolean canRequestConfirmation;
  private Boolean canConfirmRelevance;
  /** Qui est attendu pour confirmer l'actualite (attente prolongee seulement). */
  private IncidentValidatorRole relevanceResponderRole;
  private String relevanceResponderTarget;
  /** Valideur attendu, portee du type resolue sur cet incident (statuts de validation seulement). */
  private IncidentValidatorRole expectedValidatorRole;
  /** Nom du service ou de l'agence portant le role attendu ; null pour ADMIN. */
  private String expectedValidatorTarget;
  private AgencySummaryResponse agency;
  private UUID creatorServiceId;
  private List<IncidentHistoryResponse> history;
  private List<IncidentCommentResponse> comments;
  private IncidentCause cause;
  private String causeDetail;
  private String reference;
  private String treatmentDescription;
  private String resolutionDescription;
  private String closureDescription;
  private String proposedSolution;
  private String directionRejectionReason;
  private Integer estimatedResolutionHours;
}
