// DTO : transporte les donnees liees a bulk purge entre les couches.

package com.fintrack.notification.model.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO de reponse exposant les informations de bulk purge.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPurgeResponse {

  private String scope;
  private long deletedCount;
  private long preservedCount;
  private boolean dryRun;
  private Instant executedAt;
}
