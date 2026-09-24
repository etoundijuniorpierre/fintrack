// Composant backend : porte la logique liee a notification purge result.

package com.fintrack.notification.model.readmodel;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Modele interne du resultat de purge des notifications.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPurgeResult {

  private String scope;
  private long deletedCount;
  private long preservedCount;
  private boolean dryRun;
  private Instant executedAt;
}
