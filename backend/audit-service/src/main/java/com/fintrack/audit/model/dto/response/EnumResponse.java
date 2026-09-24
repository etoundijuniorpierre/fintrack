// DTO : transporte les donnees liees a enum entre les couches.

package com.fintrack.audit.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO representant une valeur d'enumeration avec son code et ses libelles traduits.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumResponse {

  /** Identifiant technique stable. Jamais traduit. */
  private String code;
  /** Libellé traduit selon la locale de la requête. */
  private String name;
  /** Description traduite selon la locale de la requête. */
  private String description;
}
