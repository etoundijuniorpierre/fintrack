// Client inter-services : communique avec les services externes lies a reporting system config client.

package com.fintrack.user.client.reporting.fallback;

import com.fintrack.user.client.reporting.ReportingSystemConfigClient;
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
}
