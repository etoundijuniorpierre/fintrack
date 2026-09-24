// Traitement planifie : purge les rapports apres la duree de retention configuree.

package com.fintrack.reporting.scheduler;

import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.service.SystemConfigService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Applique quotidiennement la politique de retention des rapports generes.
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportRetentionJob {

  static final String RETENTION_SETTING_KEY = "reportRetentionDays";
  static final long DEFAULT_RETENTION_DAYS = 20;

  private final GeneratedReportRepository repository;
  private final SystemConfigService systemConfigService;
  private final Clock applicationClock;

  // Supprime les rapports crees avant la date limite de retention.
  @Transactional
  @Scheduled(
    cron = "${fintrack.report.retention-cleanup-cron}",
    zone = "${fintrack.time-zone}"
  )
  public void purgeExpiredReports() {
    long retentionDays = systemConfigService.getThresholdLong(
      RETENTION_SETTING_KEY,
      DEFAULT_RETENTION_DAYS
    );
    LocalDateTime cutoff = LocalDateTime
      .now(applicationClock)
      .minusDays(retentionDays);
    int deletedCount = repository.deleteCreatedBefore(cutoff);

    if (deletedCount > 0) {
      log.info(
        "Purge des rapports : {} rapport(s) antérieur(s) à {} supprimé(s)",
        deletedCount,
        cutoff
      );
    }
  }
}
