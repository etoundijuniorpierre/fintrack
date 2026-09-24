// DTO : transporte les donnees liees a unused incident type entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de inutilise incident type.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnusedIncidentTypeResponse {

  private String id;
  private String name;
  private boolean active;
}
