// Composant backend : porte la logique liee a metric scope.

package com.fintrack.incident.model.readmodel;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Porte la responsabilite applicative liee a metric perimetre.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricScope {

  private String view;
  private UUID userId;
  private UUID agencyId;
  private UUID serviceId;
}
