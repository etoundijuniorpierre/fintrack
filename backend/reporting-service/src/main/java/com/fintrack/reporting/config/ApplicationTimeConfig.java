// Configuration temporelle : aligne les traitements planifies sur le fuseau applicatif.
package com.fintrack.reporting.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Fournit une horloge unique et testable basee sur APP_TIME_ZONE.
@Configuration
public class ApplicationTimeConfig {

  @Bean
  // Cree l'horloge utilisee par les planifications et horodatages automatiques.
  public Clock applicationClock(
    @Value("${fintrack.time-zone}") String timeZone
  ) {
    return Clock.system(ZoneId.of(timeZone));
  }
}
