// DTO : transporte les donnees liees a enum entre les couches.

package com.fintrack.incident.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse generique pour exposer des valeurs d'enumerations.

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumResponse {

  /** Identifiant technique stable (ex: "OPEN", "HIGH"). Jamais traduit. */
  private String code;
  /** Libellé traduit selon la locale de la requête. */
  private String name;
  /** Description traduite selon la locale de la requête. */
  private String description;
}
