// DTO : transporte le reglage e-mail d'un evenement recu du reporting-service.

package com.fintrack.incident.client.reporting.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Reglage e-mail d'un evenement (interrupteur + exclusions).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailNotificationEventSettingClient {

  private String event;
  private boolean enabled;
  private boolean supportsExclusion;
  private List<UUID> excludedUserIds;
}
