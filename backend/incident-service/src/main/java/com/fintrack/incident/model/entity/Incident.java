// Entite metier : represente les donnees persistees liees a incident.

package com.fintrack.incident.model.entity;

import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentCause;
import com.fintrack.incident.model.constant.IncidentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite JPA representant un incident persistant en base de donnees.

@Getter
@Setter
@ToString(exclude = { "history", "comments" })
@EqualsAndHashCode(callSuper = true, exclude = { "history", "comments" })
@Entity
@Table(
  name = "incidents",
  indexes = {
    @Index(
      name = "idx_incidents_status_created_at",
      columnList = "status, created_at"
    ),
    @Index(
      name = "idx_incidents_agency_created_at",
      columnList = "agency_id, created_at"
    ),
    @Index(
      name = "idx_incidents_assigned_status",
      columnList = "assigned_to, status"
    ),
    @Index(
      name = "idx_incidents_created_by_created_at",
      columnList = "created_by, created_at"
    ),
    @Index(
      name = "idx_incidents_creator_service_created_at",
      columnList = "creator_service_id, created_at"
    ),
    @Index(
      name = "idx_incidents_transferred_service_created_at",
      columnList = "transferred_to_service, created_at"
    ),
    @Index(name = "idx_incidents_due_status", columnList = "due_date, status"),
    @Index(name = "idx_incidents_closed_at", columnList = "closed_at"),
    @Index(name = "idx_incidents_resolved_at", columnList = "resolved_at"),
    @Index(name = "idx_incidents_validated_at", columnList = "validated_at"),
  }
)
@Access(AccessType.FIELD)
public class Incident extends BaseEntity {

  @NotBlank
  @Size(min = 3, max = 200)
  @Column(nullable = false, length = 200)
  private String title;

  @NotBlank
  @Size(max = 5000)
  @Column(nullable = false, length = 5000)
  private String description;

  @NotNull
  @Column(name = "type_id", nullable = false)
  private UUID typeId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private Criticality criticality;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50, columnDefinition = "varchar(50)")
  private IncidentStatus status;

  @Column(name = "validated_at")
  private LocalDateTime validatedAt;

  @Column(name = "transferred_at")
  private LocalDateTime transferredAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @Column(name = "blocked_at")
  private LocalDateTime blockedAt;

  @Column(name = "unblocked_at")
  private LocalDateTime unblockedAt;

  @Column(name = "reopened_at")
  private LocalDateTime reopenedAt;

  // Echeance SLA horodatee : un SLA exprime en heures doit rester comparable
  // a l'heure pres, pas arrondi au jour.
  @Column(name = "due_date")
  private LocalDateTime dueDate;

  // Echeance de reference, posee a la qualification et jamais repoussee par une reprise :
  // sans elle, remettre dueDate a neuf effacerait le retard des metriques de conformite.
  @Column(name = "initial_due_date")
  private LocalDateTime initialDueDate;

  @Column(name = "last_sla_reminder_sent_at")
  private LocalDateTime lastSlaReminderSentAt;

  // Depuis quand une decision est attendue. Jamais solde : le statut fait foi.
  @Column(name = "decision_awaited_since")
  private LocalDateTime decisionAwaitedSince;

  @Column(name = "last_decision_reminder_sent_at")
  private LocalDateTime lastDecisionReminderSentAt;

  // Attente prolongee : le traitant demande a l'entite source si l'incident est encore
  // d'actualite. Tant que ces champs sont renseignes, une reponse est attendue.
  @Column(name = "confirmation_requested_at")
  private LocalDateTime confirmationRequestedAt;

  @Column(name = "confirmation_requested_by")
  private UUID confirmationRequestedBy;

  @Column(name = "last_confirmation_reminder_sent_at")
  private LocalDateTime lastConfirmationReminderSentAt;

  // Derniere relance envoyee pour un incident critique encore ouvert.
  @Column(name = "last_critical_reminder_sent_at")
  private LocalDateTime lastCriticalReminderSentAt;

  /** Date de survenance de l'événement (peut être antérieure à la création). */
  @PastOrPresent(message = "{validation.incident.date_not_future}")
  @Column(name = "incident_date")
  private LocalDate incidentDate;

  /** Date de constatation de l'incident (par défaut = date de création). */
  @PastOrPresent(message = "{validation.incident.date_not_future}")
  @Column(name = "observation_date")
  private LocalDate observationDate;

  @NotNull
  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @Column(name = "validated_by")
  private UUID validatedBy;

  @Column(name = "assigned_to")
  private UUID assignedTo;

  @Column(name = "blocked_by")
  private UUID blockedBy;

  @Column(name = "reopened_by")
  private UUID reopenedBy;

  @Column(name = "transferred_to_service")
  private UUID transferredToService;

  // Service cible a la declaration. Fige comme initialDueDate fige l'engagement :
  // transferredToService est ecrase a chaque transfert, on perdrait sinon le point de
  // depart et donc la capacite de dire qu'un incident a ete reroute ailleurs.
  @Column(name = "initial_target_service_id")
  private UUID initialTargetServiceId;

  @Column(name = "transfer_reason", length = 5000)
  private String transferReason;

  /** Motif obligatoire du rejet par le valideur (en attente de validation -> rejeté). */
  @Column(name = "reject_reason", length = 5000)
  private String rejectReason;

  /** Motif obligatoire de l'annulation. Distinct du rejet : autre acteur, autre decision. */
  @Column(name = "cancel_reason", length = 5000)
  private String cancelReason;

  @Column(name = "blocked_reason", length = 5000)
  private String blockedReason;

  // Statut de l'incident juste avant un blocage (manuel ou SLA) : sert a restaurer le
  // statut d'origine a la reprise. Stocke en texte (nom du statut) pour eviter toute
  // contrainte CHECK a maintenir lors de l'evolution de l'enum des statuts.
  @Column(name = "pre_block_status", length = 50)
  private String preBlockStatus;

  /** Motif obligatoire renseigné lors d'une réouverture (clôture/rejet -> ouvert). */
  @Column(name = "reopen_reason", length = 5000)
  private String reopenReason;

  @Column(name = "reopen_count", columnDefinition = "integer default 0")
  private int reopenCount;

  // Cause de l'incident
  @Enumerated(EnumType.STRING)
  @Column(name = "cause", length = 100)
  private IncidentCause cause;

  /** Précision de la cause : personne responsable (HUMAN/EXTERNAL) ou description (autres). */
  @Column(name = "cause_detail", length = 2000)
  private String causeDetail;

  /** Reference lisible et immuable (FT-I-2026-0001), attribuee a la creation. */
  @Column(name = "reference", length = 20, unique = true)
  private String reference;

  // Compte-rendu du traitement, saisi par le traitant au passage en TREATED.
  @Column(name = "treatment_description", length = 5000)
  private String treatmentDescription;

  // Compte-rendu de resolution, saisi par le valideur a la resolution.
  @Column(name = "resolution_description", length = 5000)
  private String resolutionDescription;

  // Note de cloture, saisie a la cloture.
  @Column(name = "closure_description", length = 5000)
  private String closureDescription;

  /** Solution proposée par le traitant pour validation préalable par la Direction. */
  @Column(name = "proposed_solution", length = 15000)
  private String proposedSolution;

  /** Motif de rejet de la solution par la Direction. */
  @Column(name = "direction_rejection_reason", length = 10000)
  private String directionRejectionReason;

  /**
   * Temps de résolution estimé par le traitant, en heures, saisi à la prise en charge.
   * S'il est renseigné il pilote l'échéance SLA (dueDate) ; sinon le SLA par défaut prime.
   */
  @Column(name = "estimated_resolution_hours")
  private Integer estimatedResolutionHours;

  @NotNull
  @Column(name = "agency_id", nullable = false)
  private UUID agencyId;

  /** Service de rattachement du créateur (permet la validation par le chef de service). */
  @Column(name = "creator_service_id")
  private UUID creatorServiceId;

  /** L'ID de l'incident d'origine si cet incident est issu d'un clonage. */
  @Column(name = "source_incident_id")
  private UUID sourceIncidentId;

  @OneToMany(
    mappedBy = "incident",
    fetch = FetchType.LAZY,
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  private Set<IncidentHistory> history;

  @OneToMany(
    mappedBy = "incident",
    fetch = FetchType.LAZY,
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  private Set<IncidentComment> comments;

  // Maintient une chronologie coherente lors de toute ecriture JPA.
  @AssertTrue(message = "{validation.incident.date_order}")
  public boolean isDateOrderValid() {
    return (
      incidentDate == null ||
      observationDate == null ||
      !incidentDate.isAfter(observationDate)
    );
  }
}
