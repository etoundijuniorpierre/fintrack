// Entite metier : represente les donnees persistees liees a incident resolution cycle.

package com.fintrack.incident.model.entity;

import com.fintrack.incident.model.constant.ResolutionCycleOutcome;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Un cycle de traitement d'un incident : de son ouverture (creation ou reouverture)
// a son issue. Une reouverture ouvre un cycle N+1 au lieu d'ecraser les jalons du
// precedent, ce qui rend les delais historiques reproductibles.
@Getter
@Setter
@ToString(exclude = { "incident" })
@EqualsAndHashCode(callSuper = true, exclude = { "incident" })
@Entity
@Table(
  name = "incident_resolution_cycles",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_incident_resolution_cycle_no",
    columnNames = { "incident_id", "cycle_no" }
  ),
  indexes = {
    @Index(
      name = "idx_incident_cycle_closed_at",
      columnList = "closed_at"
    ),
    @Index(
      name = "idx_incident_cycle_resolved_at",
      columnList = "resolved_at"
    ),
  }
)
@Access(AccessType.FIELD)
public class IncidentResolutionCycle extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private Incident incident;

  @NotNull
  @Column(name = "cycle_no", nullable = false)
  private Integer cycleNo;

  @NotNull
  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "validated_at")
  private LocalDateTime validatedAt;

  @Column(name = "treated_at")
  private LocalDateTime treatedAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  // Echeance engagee a l'ouverture du cycle : fige le jugement SLA du cycle.
  @Column(name = "due_date_snapshot")
  private LocalDateTime dueDateSnapshot;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, columnDefinition = "varchar(30)")
  private ResolutionCycleOutcome outcome;

  // Temps pendant lequel l'horloge etait arretee (blocage, attente d'actualite).
  @NotNull
  @Column(name = "paused_minutes", nullable = false)
  private Long pausedMinutes = 0L;
}
