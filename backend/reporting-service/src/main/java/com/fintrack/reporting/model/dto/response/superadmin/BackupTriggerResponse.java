// DTO de reponse : porte le resultat d'un declenchement de sauvegarde manuelle.

package com.fintrack.reporting.model.dto.response.superadmin;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupTriggerResponse {
  private boolean success;
  private String message;
  private LocalDateTime timestamp;
  private String postgresStatus;
  private String mongoStatus;
  private String b2SyncStatus;
  private String backupDirectory;
}
