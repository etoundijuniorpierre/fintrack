// DTO : transporte les donnees liees a job trigger result entre les couches.

package com.fintrack.reporting.model.dto.response.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Resultat d'un declenchement manuel de job : nombre d'elements reellement traites.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTriggerResultResponse {

  private String jobName;
  private long affected;
}
