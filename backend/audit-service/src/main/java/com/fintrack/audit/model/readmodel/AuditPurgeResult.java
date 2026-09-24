// Composant backend : porte la logique liee a audit purge result.

package com.fintrack.audit.model.readmodel;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne du resultat de purge des journaux d'audit.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditPurgeResult {

  private String scope;
  private long deletedCount;
  private long preservedCount;
  private long preservedSensitive;
  private boolean dryRun;
  private Instant executedAt;
  private List<String> preservedSensitiveActions;
}
