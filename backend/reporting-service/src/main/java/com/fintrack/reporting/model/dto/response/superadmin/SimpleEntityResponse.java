// DTO : transporte les donnees liees a simple entity entre les couches.

package com.fintrack.reporting.model.dto.response.superadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de simple entite.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleEntityResponse {

  private String id;
  private String name;
}
