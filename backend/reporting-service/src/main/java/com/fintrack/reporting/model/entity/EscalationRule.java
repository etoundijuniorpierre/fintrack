// Entite metier : represente les donnees persistees liees a escalation rule.

package com.fintrack.reporting.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Regle d'escalade persistee : activable/desactivable et auditee par le Super Admin.
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
  name = "escalation_rules",
  uniqueConstraints = @UniqueConstraint(columnNames = "rule_key")
)
public class EscalationRule extends BaseEntity {

  @Column(name = "rule_key", nullable = false, length = 64)
  private String ruleKey;

  @Column(name = "trigger_label", nullable = false, length = 255)
  private String triggerLabel;

  @Column(name = "action_label", nullable = false, length = 255)
  private String actionLabel;

  @Column(name = "owner_role", nullable = false, length = 64)
  private String ownerRole;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "automated", nullable = false)
  private boolean automated;

  @Column(name = "last_triggered_at")
  private LocalDateTime lastTriggeredAt;

  @Column(name = "last_evaluation_count")
  private Long lastEvaluationCount;

  @Column(name = "notification_type", length = 64)
  private String notificationType;
}
