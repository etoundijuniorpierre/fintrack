// Composant backend : porte la logique liee a user service health indicator.

package com.fintrack.incident.health;

import com.fintrack.incident.client.user.UserClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

// Indicateur de sante (Health Indicator) du service utilisateur.

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceHealthIndicator implements HealthIndicator {

  private final UserClient userClient;

  private Instant lastCheck = Instant.MIN;
  private Health cachedHealth = Health.unknown().build();
  private static final Duration CHECK_INTERVAL = Duration.ofSeconds(30);

  @Override
  // Verifie la disponibilite du user-service pour la supervision.
  public Health health() {
    Instant now = Instant.now();

    if (Duration.between(lastCheck, now).compareTo(CHECK_INTERVAL) < 0) {
      return cachedHealth;
    }

    try {
      lastCheck = now;

      Map<String, Object> response = userClient.getHealth();
      Object status = response != null ? response.get("status") : null;

      if ("UP".equals(status)) {
        cachedHealth = Health.up()
          .withDetail("service", "user-service")
          .withDetail("endpoint", "/actuator/health")
          .withDetail("userServiceStatus", status)
          .build();

        log.debug("User-service contrôle de santé réussi");
        return cachedHealth;
      }

      cachedHealth = Health.down()
        .withDetail("service", "user-service")
        .withDetail("endpoint", "/actuator/health")
        .withDetail("userServiceStatus", status)
        .build();

      log.warn("User-service contrôle de santé échoué: status={}", status);
    } catch (Exception e) {
      cachedHealth = Health.down()
        .withDetail("service", "user-service")
        .withDetail("endpoint", "/actuator/health")
        .withDetail("error", e.getMessage())
        .withDetail("exception", e.getClass().getName())
        .build();

      log.warn("User-service contrôle de santé échoué: {}", e.getMessage());
    }

    return cachedHealth;
  }

  // Lance une verification immediate de sante du user-service.
  public Health checkNow() {
    lastCheck = Instant.MIN;
    return health();
  }
}
