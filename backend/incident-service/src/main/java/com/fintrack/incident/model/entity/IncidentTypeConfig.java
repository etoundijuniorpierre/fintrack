// Entite metier : represente les donnees persistees liees a incident type.

package com.fintrack.incident.model.entity;

import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentActorRole;
import com.fintrack.incident.model.constant.IncidentValidatorScope;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Entite JPA representant la configuration d'un type d'incident.

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "incident_type_configs")
public class IncidentTypeConfig extends BaseEntity {

  @NotBlank
  @Column(nullable = false, unique = true, length = 100)
  private String name; // code technique ex: "informatique"

  @NotBlank
  @Column(name = "display_name", nullable = false, length = 200)
  private String displayName; // libellé affiché ex: "Informatique"

  @Column(length = 500)
  private String description;

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @Column(name = "sla_hours", columnDefinition = "integer default 48")
  private Integer slaHours; // délai de traitement en heures

  @Column(name = "default_target_service_id")
  private UUID defaultTargetServiceId; // service proposé par défaut lors d'un transfert

  @Column(name = "default_target_user_id")
  private UUID defaultTargetUserId; // user cible par défaut (cas où le type ne concerne pas un service)

  // columnDefinition avec DEFAULT : permet a ddl-auto=update d'ajouter la colonne
  // NOT NULL sur une table deja peuplee (backfill des lignes existantes).
  @Column(
    name = "requires_validation",
    nullable = false,
    columnDefinition = "boolean default true"
  )
  private boolean requiresValidation = true;

  @Column(
    name = "requires_cause_analysis",
    nullable = false,
    columnDefinition = "boolean default true"
  )
  private boolean requiresCauseAnalysis = false;

  @Column(
    name = "requires_direction_validation",
    nullable = false,
    columnDefinition = "boolean default true"
  )
  private boolean requiresDirectionValidation = false;

  // Valideurs designes de la Direction : un ou plusieurs titulaires de la permission
  // VALIDATION_DIRECTION notifies lors d'une proposition de solution. La validation
  // reste ouverte a tout titulaire de la permission (cf. IncidentWorkflowGuard).
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
    name = "incident_type_config_direction_validators",
    joinColumns = @JoinColumn(name = "incident_type_config_id")
  )
  @Column(name = "validator_id")
  private Set<UUID> directionValidatorIds = new HashSet<>();

  @Column(
    name = "email_notifications_enabled",
    nullable = false,
    columnDefinition = "boolean default false"
  )
  private boolean emailNotificationsEnabled = false;

  @Column(
    name = "in_app_notifications_enabled",
    nullable = false,
    columnDefinition = "boolean default true"
  )
  private boolean inAppNotificationsEnabled = true;

  // Etapes configurables du workflow : chacune admet PLUSIEURS roles habilites
  // (multi-choix). Un ensemble vide signifie "defaut de l'etape" (applique par le
  // service a la creation et par IncidentWorkflowGuard au controle). Meme pattern
  // @ElementCollection que directionValidatorIds.

  // Qui est autorise a traiter un incident de ce type.
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
    name = "incident_type_config_treater_roles",
    joinColumns = @JoinColumn(name = "incident_type_config_id")
  )
  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 50)
  private Set<IncidentActorRole> treaterRoles = new HashSet<>();

  // Qui est autorise a resoudre / marquer non resolu un incident de ce type.
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
    name = "incident_type_config_resolver_roles",
    joinColumns = @JoinColumn(name = "incident_type_config_id")
  )
  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 50)
  private Set<IncidentActorRole> resolverRoles = new HashSet<>();

  // Qui est autorise a cloturer un incident de ce type.
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
    name = "incident_type_config_closer_roles",
    joinColumns = @JoinColumn(name = "incident_type_config_id")
  )
  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 50)
  private Set<IncidentActorRole> closerRoles = new HashSet<>();

  // Qui est autorise a rouvrir un incident traite (RESOLU) de ce type.
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
    name = "incident_type_config_reopener_roles",
    joinColumns = @JoinColumn(name = "incident_type_config_id")
  )
  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 50)
  private Set<IncidentActorRole> reopenerRoles = new HashSet<>();

  // Qui valide les incidents de ce type (defaut : chef du service du createur).
  @Enumerated(EnumType.STRING)
  @Column(
    name = "validator_scope",
    length = 50,
    nullable = false,
    columnDefinition = "varchar(50) default 'SOURCE_SERVICE_MANAGER'"
  )
  private IncidentValidatorScope validatorScope =
    IncidentValidatorScope.SOURCE_SERVICE_MANAGER;

  // Criticite suggeree au declarant lors de la creation d'un incident de ce type.
  @Enumerated(EnumType.STRING)
  @Column(
    name = "default_criticality",
    length = 50,
    columnDefinition = "varchar(50) default 'HIGH'"
  )
  private Criticality defaultCriticality;
}
