// DTO : transporte les donnees liees a comparison entry entre les couches.

package com.fintrack.incident.model.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de comparaison entree.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonEntry {

  private UUID id;
  private String name;
  private DashboardMetricsResponse metrics;
}
