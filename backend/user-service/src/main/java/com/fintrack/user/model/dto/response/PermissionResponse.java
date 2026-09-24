// DTO : transporte les donnees liees a permission entre les couches.

package com.fintrack.user.model.dto.response;

import com.fintrack.user.model.dto.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

// DTO de reponse detaillant les caracteristiques d'une permission.

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionResponse extends BaseDto {

  private String name;
  private String description;
}
