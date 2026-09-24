// DTO : transporte les donnees liees a role entre les couches.

package com.fintrack.user.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fintrack.user.model.dto.BaseDto;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

// DTO de reponse listant le nom et les permissions d'un role.

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleResponse extends BaseDto {

  private String name;
  private String displayName;
  private String description;

  @JsonProperty("isSystem")
  private boolean isSystem;

  private Set<PermissionResponse> permissions;
}
