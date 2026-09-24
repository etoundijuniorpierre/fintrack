// DTO : transporte les donnees liees a role entre les couches.

package com.fintrack.user.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

// DTO de requete pour l'ajout ou la mise a jour d'un role.

@Data
public class RoleRequest {

  @NotBlank(message = "{role.name.required}")
  private String name;

  private String displayName;
  private String description;

  @JsonProperty("isSystem")
  private Boolean isSystem;

  private Set<UUID> permissionIds;
}
