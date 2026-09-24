// Client inter-services : communique avec les services externes lies a reporting system config client.

package com.fintrack.notification.client.reporting;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Lit un seuil metier depuis le reporting-service, avec repli si indisponible.
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingSystemConfigClientService {

  private final ReportingSystemConfigClient reportingSystemConfigClient;

  // Fournit seuil int a la couche appelante.

  public int getThresholdInt(String key, int fallback) {
    try {
      Map<String, Object> thresholds =
        reportingSystemConfigClient.getThresholds();
      Object value = thresholds.get(key);
      if (value instanceof Number number) {
        return number.intValue();
      }
      return value == null ? fallback : Integer.parseInt(value.toString());
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
