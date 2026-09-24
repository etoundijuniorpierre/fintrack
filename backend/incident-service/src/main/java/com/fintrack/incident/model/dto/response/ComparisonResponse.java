// DTO : transporte les donnees liees a comparison entre les couches.

package com.fintrack.incident.model.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Comparaison de plusieurs entites de meme type (utilisateurs, services ou agences).
// Chaque entree porte les metriques completes ; le front compare les taux normalises.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResponse {

  private String entityType; // USER, SERVICE, AGENCY
  private List<ComparisonEntry> entries;
}
