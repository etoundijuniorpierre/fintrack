// Client inter-services : communique avec les services externes lies a reporting system config client.

package com.fintrack.user.client.reporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Lecture resiliente des seuils systeme distants : en cas d'indisponibilite, on retombe sur le repli local.
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingSystemConfigClientService {

  private final ReportingSystemConfigClient reportingSystemConfigClient;

  // Fournit seuil long a la couche appelante.

  public long getThresholdLong(String key, long fallback) {
    try {
      Object value = reportingSystemConfigClient.getThresholds().get(key);
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

  // Fournit seuil int a la couche appelante.

  public int getThresholdInt(String key, int fallback) {
    return (int) getThresholdLong(key, fallback);
  }
}
