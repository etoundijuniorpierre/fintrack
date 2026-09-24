// Entite metier : represente les donnees persistees liees a escalation rule event.

package com.fintrack.reporting.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Historique des evaluations d'une regle d'escalade.
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "escalation_rule_events")
public class EscalationRuleEvent extends BaseEntity {

  @Column(name = "rule_key", nullable = false, length = 64)
  private String ruleKey;

  @Column(name = "evaluated_at", nullable = false)
  private LocalDateTime evaluatedAt;

  @Column(name = "hit_count", nullable = false)
  private long hitCount;

  @Column(name = "notifications_emitted", nullable = false)
  private long notificationsEmitted;

  @Column(name = "outcome", nullable = false, length = 32)
  private String outcome;

  @Column(name = "details", columnDefinition = "TEXT")
  private String details;
}
