// DTO : transporte les donnees liees a user permission client entre les couches.

package com.fintrack.reporting.client.superadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Transporte les donnees liees a utilisateur permission client entre services.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionClientResponse {

  private String name;
  private String description;
}
