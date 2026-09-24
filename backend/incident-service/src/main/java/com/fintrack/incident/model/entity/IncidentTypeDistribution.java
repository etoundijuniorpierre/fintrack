// Entite metier : represente les donnees persistees liees a incident type distribution.

package com.fintrack.incident.model.entity;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Projection interne de distribution des incidents par type.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentTypeDistribution {

  private UUID typeId;
  private String typeName;
  private long count;
}
