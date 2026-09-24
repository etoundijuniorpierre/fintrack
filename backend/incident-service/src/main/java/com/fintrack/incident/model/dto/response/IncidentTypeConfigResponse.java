// DTO : transporte les donnees liees a incident type config entre les couches.

package com.fintrack.incident.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.incident.model.constant.Criticality;
import com.fintrack.incident.model.constant.IncidentActorRole;
import com.fintrack.incident.model.constant.IncidentValidatorScope;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant la configuration d'un type d'incident.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentTypeConfigResponse {

  private UUID id;
  private String name;
  private String displayName;
  private String description;

  @JsonProperty("isActive")
  private boolean active;

  private Integer slaHours;

  private ServiceSummaryResponse defaultTargetService;

  /** User cible par défaut (cas où le type d'incident ne concerne pas un service). */
  private UserSummaryResponse defaultTargetUser;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UUID modifiedBy;

  private boolean requiresValidation;
  private boolean requiresCauseAnalysis;
  private boolean requiresDirectionValidation;
  private List<UserSummaryResponse> directionValidators;
  private boolean emailNotificationsEnabled;
  private Set<IncidentActorRole> treaterRoles;
  private Set<IncidentActorRole> resolverRoles;
  private Set<IncidentActorRole> closerRoles;
  private Set<IncidentActorRole> reopenerRoles;
  private IncidentValidatorScope validatorScope;
  private Criticality defaultCriticality;
}
