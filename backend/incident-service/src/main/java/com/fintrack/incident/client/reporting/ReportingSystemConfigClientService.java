// Client inter-services : communique avec les services externes lies a reporting system config client.

package com.fintrack.incident.client.reporting;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Porte la responsabilite applicative liee a reporting configuration systeme client.

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingSystemConfigClientService {

  private final ReportingSystemConfigClient reportingSystemConfigClient;

  // Fournit seuil long a la couche appelante.

  public long getThresholdLong(String key, long fallback) {
    try {
      Map<String, Object> thresholds =
        reportingSystemConfigClient.getThresholds();
      Object value = thresholds.get(key);
      if (value instanceof Number number) {
        return number.longValue();
      }
      return value == null ? fallback : Long.parseLong(value.toString());
    } catch (RuntimeException ex) {
      log.debug(
        "Impossible de charger le seuil reporting '{}', utilisation de la valeur de repli {}: {}",
        key,
        fallback,
        ex.toString()
      );
      return fallback;
    }
  }
}
