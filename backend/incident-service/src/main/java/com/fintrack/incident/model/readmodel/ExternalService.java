// Composant backend : porte la logique liee a external.

package com.fintrack.incident.model.readmodel;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne d'un service resolu depuis user-service.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalService {

  private UUID id;
  private String name;
  private String description;
  private boolean active;
}
