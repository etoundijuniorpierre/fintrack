// DTO : transporte l'ensemble des reglages e-mail recus du reporting-service.

package com.fintrack.incident.client.reporting.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Catalogue des reglages e-mail par evenement.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailNotificationSettingsClient {

  private List<EmailNotificationEventSettingClient> events;
}
