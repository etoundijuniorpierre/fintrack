// Composant backend : porte la logique liee a external agency.

package com.fintrack.incident.model.readmodel;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne d'une agence resolue depuis user-service.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAgency {

  private UUID id;
  private String name;
  private String code;
  private boolean active;
}
