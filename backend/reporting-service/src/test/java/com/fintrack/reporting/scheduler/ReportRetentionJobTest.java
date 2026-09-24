// Tests unitaires : verifient la purge automatique des rapports expires.

package com.fintrack.reporting.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.service.SystemConfigService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

// Protege le calcul de la date limite selon la retention configuree.
class ReportRetentionJobTest {

  // Verifie que la purge respecte la duree choisie par le Super Admin.
  @Test
  void shouldDeleteReportsCreatedBeforeConfiguredRetentionCutoff() {
    GeneratedReportRepository repository = org.mockito.Mockito.mock(
      GeneratedReportRepository.class
    );
    SystemConfigService systemConfigService = org.mockito.Mockito.mock(
      SystemConfigService.class
    );
    ZoneId zone = ZoneId.of("Africa/Lagos");
    Clock clock = Clock.fixed(Instant.parse("2026-07-29T10:00:00Z"), zone);
    when(
      systemConfigService.getThresholdLong(
        ReportRetentionJob.RETENTION_SETTING_KEY,
        ReportRetentionJob.DEFAULT_RETENTION_DAYS
      )
    ).thenReturn(30L);

    ReportRetentionJob job = new ReportRetentionJob(
      repository,
      systemConfigService,
      clock
    );
    job.purgeExpiredReports();

    verify(repository).deleteCreatedBefore(
      LocalDateTime.ofInstant(clock.instant(), zone).minusDays(30)
    );
  }
}
