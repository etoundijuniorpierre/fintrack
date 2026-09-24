// DTO : transporte les donnees liees a enum entre les couches.

package com.fintrack.document.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse representant une valeur d'enumeration avec son libelle traduit.
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
