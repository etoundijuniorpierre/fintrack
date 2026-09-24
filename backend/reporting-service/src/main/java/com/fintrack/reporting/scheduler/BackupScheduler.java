// Planificateur : declenche la sauvegarde quotidienne a l'heure choisie par le Super Admin.

package com.fintrack.reporting.scheduler;

import com.fintrack.reporting.model.readmodel.superadmin.BackupOutcome;
import com.fintrack.reporting.service.BackupService;
import com.fintrack.reporting.service.SystemConfigService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler implements SchedulingConfigurer {

  private static final long DISABLED_RECHECK_MINUTES = 15;

  private final BackupService backupService;
  private final SystemConfigService systemConfigService;

  @Value("${fintrack.time-zone:Africa/Lagos}")
  private String timeZone;

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    taskRegistrar.addTriggerTask(this::runIfEnabled, triggerContext -> nextRun());
  }

  private Instant nextRun() {
    ZoneId zone = ZoneId.of(timeZone);
    Instant now = Instant.now();

    if (!isEnabled()) {
      return now.plusSeconds(DISABLED_RECHECK_MINUTES * 60);
    }

    int hour = (int) Math.min(
      23,
      Math.max(0, systemConfigService.getThresholdLong("backupScheduleHour", 20))
    );
    Instant todayAtHour = LocalDate.now(zone)
      .atTime(LocalTime.of(hour, 0))
      .atZone(zone)
      .toInstant();

    return todayAtHour.isAfter(now)
      ? todayAtHour
      : todayAtHour.plusSeconds(24 * 3600);
  }

  private boolean isEnabled() {
    return systemConfigService.getThresholdLong("backupScheduleEnabled", 1) != 0;
  }

  // Relit l'activation au moment de tirer : un changement pendant l'attente est pris en compte.
  public void runIfEnabled() {
    if (!isEnabled()) {
      return;
    }
    try {
      BackupOutcome outcome = backupService.run();
      if (outcome.isSuccess()) {
        log.info(
          "Sauvegarde planifiée terminée (B2 : {}).",
          outcome.getB2SyncStatus()
        );
      } else {
        log.error("Sauvegarde planifiée en échec : {}", outcome.getMessage());
      }
    } catch (RuntimeException ex) {
      log.error("Échec inattendu de la sauvegarde planifiée", ex);
    }
  }
}
