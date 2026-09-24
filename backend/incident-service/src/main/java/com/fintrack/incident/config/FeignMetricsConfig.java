// Configuration Spring : declare les regles techniques liees a feign metrics.

package com.fintrack.incident.config;

import feign.Capability;
import feign.micrometer.MicrometerCapability;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration exposant les metriques Micrometer sur les appels Feign
@Configuration
public class FeignMetricsConfig {

  // Active la collecte de metriques Feign.

  @Bean
  public Capability micrometerCapability(MeterRegistry registry) {
    return new MicrometerCapability(registry);
  }
}
