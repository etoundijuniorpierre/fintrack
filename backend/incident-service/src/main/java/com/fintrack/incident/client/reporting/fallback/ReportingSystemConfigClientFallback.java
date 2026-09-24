// Client inter-services : communique avec les services externes lies a reporting system config client.

package com.fintrack.incident.client.reporting.fallback;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClient;
import com.fintrack.incident.client.reporting.dto.EmailNotificationSettingsClient;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

// Repli maitrisant les reponses degradees du client de configuration systeme reporting.

@Component
public class ReportingSystemConfigClientFallback
  implements ReportingSystemConfigClient
{

  // Fournit seuils a la couche appelante.

  @Override
  public Map<String, Object> getThresholds() {
    return Map.of();
  }

  // Repli : catalogue vide. Interprete par le service appelant comme "tout actif,
  // aucune exclusion" pour ne pas bloquer les e-mails si le reporting est indisponible.
  @Override
  public EmailNotificationSettingsClient getEmailNotifications() {
    return EmailNotificationSettingsClient.builder().events(List.of()).build();
  }
}
