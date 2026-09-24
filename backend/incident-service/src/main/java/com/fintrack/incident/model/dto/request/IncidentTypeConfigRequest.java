// DTO : transporte les donnees liees a incident type config entre les couches.

package com.fintrack.incident.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentActorRole;
import com.fintrack.incident.model.constant.IncidentValidatorScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de requete pour configurer un type d'incident.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentTypeConfigRequest {

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 100, message = "{validation.size.max}")
  private String name;

  @NotBlank(message = "{validation.not_blank}")
  @Size(max = 200, message = "{validation.size.max}")
  private String displayName;

  @Size(max = 1000, message = "{validation.size.max}")
  private String description;

  @JsonProperty("isActive")
  @Builder.Default
  private Boolean active = true;

  private Integer slaHours;
  private UUID defaultTargetServiceId;

  private UUID defaultTargetUserId;

  private Boolean requiresValidation;

  private Boolean requiresCauseAnalysis;

  private Boolean requiresDirectionValidation;

  private Set<UUID> directionValidatorIds;

  private Boolean emailNotificationsEnabled;

  // Etapes configurables multi-choix : au moins un role habilite par etape. Un
  // ensemble vide/omis => defaut de l'etape applique cote service.
  private Set<IncidentActorRole> treaterRoles;

  private Set<IncidentActorRole> resolverRoles;

  private Set<IncidentActorRole> closerRoles;

  private Set<IncidentActorRole> reopenerRoles;

  private IncidentValidatorScope validatorScope;

  private Criticality defaultCriticality;
}
