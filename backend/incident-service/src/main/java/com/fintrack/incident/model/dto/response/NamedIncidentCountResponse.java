// DTO : transporte les donnees liees a named incident count entre les couches.

package com.fintrack.incident.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Fragment wire du tableau de bord : paire libelle/compte (mappe depuis le read-model).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NamedIncidentCountResponse {

  private String name;
  private long count;
}
