// Entite metier : represente les donnees persistees liees a incident history.

package com.fintrack.incident.model.entity;

import com.fintrack.incident.model.constant.ActionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite JPA representant une etape de l'historique d'evolution d'un incident.

@Getter
@Setter
@ToString(exclude = { "incident" })
@EqualsAndHashCode(callSuper = true, exclude = { "incident" })
@Entity
@Table(
  name = "incident_histories",
  indexes = {
    @Index(
      name = "idx_incident_history_action_created",
      columnList = "action, created_at"
    ),
    @Index(
      name = "idx_incident_history_action_new_created",
      columnList = "action, new_value, created_at"
    ),
    @Index(
      name = "idx_incident_history_incident_created",
      columnList = "incident_id, created_at"
    ),
    @Index(
      name = "idx_incident_history_user_action_created",
      columnList = "user_id, action, created_at"
    ),
  }
)
@Access(AccessType.FIELD)
public class IncidentHistory extends BaseEntity {

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "incident_id", nullable = false)
  private Incident incident;
  @Column(name = "user_id")
  private UUID userId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 100, columnDefinition = "varchar(100)")
  private ActionType action;

  @Column(name = "old_value", length = 255)
  private String oldValue;

  @Column(name = "new_value", length = 255)
  private String newValue;

  @Column(length = 1000)
  private String comment;
}
