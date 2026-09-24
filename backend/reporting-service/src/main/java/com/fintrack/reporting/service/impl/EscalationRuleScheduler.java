// Service metier : orchestre les regles et traitements lies a escalation rule scheduler.

package com.fintrack.reporting.service.impl;

import com.fintrack.reporting.service.EscalationRuleService;
import com.fintrack.reporting.service.SystemConfigService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

// Scan d'escalade a cadence dynamique : l'intervalle est relu depuis le seuil
// Le delai reste un reglage interne ; l'ecran Super Admin n'expose plus de cadence technique.
@Slf4j
@Component
@RequiredArgsConstructor
public class EscalationRuleScheduler implements SchedulingConfigurer {

  private static final long INITIAL_DELAY_SECONDS = 60;

  private final EscalationRuleService escalationRuleService;
  private final SystemConfigService systemConfigService;

  // Declare le comportement technique attendu par l'infrastructure.

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    taskRegistrar.addTriggerTask(this::scan, triggerContext -> {
      long minutes = Math.max(
        1L,
        systemConfigService.getThresholdLong(
          "escalationScanIntervalMinutes",
          15
        )
      );
      Instant last = triggerContext.lastCompletion();
      if (last == null) {
        return Instant.now().plusSeconds(INITIAL_DELAY_SECONDS);
      }
      return last.plus(Duration.ofMinutes(minutes));
    });
  }

  // Realise l'intention metier scan.

  public void scan() {
    try {
      Map<String, Object> outcome = escalationRuleService.evaluateAndDispatch(
        true
      );
      log.debug("Escalation scan terminé: {}", outcome);
    } catch (RuntimeException ex) {
      log.warn("Échec du scan d'escalade", ex);
    }
  }
}
