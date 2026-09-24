// DTO : transporte les donnees liees a bulk purge entre les couches.

package com.fintrack.audit.model.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Resultat d'une operation de purge (reelle ou dryRun).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPurgeResponse {

  private String scope;
  private long deletedCount;
  private long preservedCount;
  private long preservedSensitive;
  private boolean dryRun;
  private Instant executedAt;
  private List<String> preservedSensitiveActions;
}
