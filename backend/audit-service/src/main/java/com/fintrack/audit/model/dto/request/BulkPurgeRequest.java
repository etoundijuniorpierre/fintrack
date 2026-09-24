// DTO : transporte les donnees liees a bulk purge entre les couches.

package com.fintrack.audit.model.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Parametres de purge manuelle declenchee par le Super Admin.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPurgeRequest {

  @Min(1)
  private int olderThanDays;

  @Min(0)
  private int preserveLastN;

  private boolean dryRun;
}
