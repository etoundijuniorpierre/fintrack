// Composant backend : porte la logique liee a expected validator.

package com.fintrack.incident.model.readmodel;

import com.fintrack.incident.model.constant.IncidentValidatorRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Valideur attendu sur un incident, une fois la portee du type resolue sur l'incident :
// le role effectif et, pour un role rattache a une structure, le nom de celle-ci.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpectedValidator {

  private IncidentValidatorRole role;
  /** Nom du service ou de l'agence porteuse du role ; null pour ADMIN. */
  private String targetName;
}
