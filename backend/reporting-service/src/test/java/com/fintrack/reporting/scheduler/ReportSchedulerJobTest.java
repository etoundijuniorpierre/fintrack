// Tests backend : verifie le declenchement des rapports dans le fuseau applicatif.
package com.fintrack.reporting.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fintrack.reporting.client.user.UserServiceClient;
import com.fintrack.reporting.client.user.dto.UserClientResponse;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.entity.ReportSchedule;
import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.repository.ReportScheduleRepository;
import com.fintrack.reporting.service.impl.AsyncReportGenerator;
import com.fintrack.reporting.service.impl.AutomaticReportGroupingService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportSchedulerJobTest {

  @Test
  void triggerNow_UsesConfiguredApplicationTimeZone() {
    ReportScheduleRepository scheduleRepository = mock(
      ReportScheduleRepository.class
    );
    GeneratedReportRepository reportRepository = mock(
      GeneratedReportRepository.class
    );
    AsyncReportGenerator asyncGenerator = mock(AsyncReportGenerator.class);
    AutomaticReportGroupingService groupingService = mock(
      AutomaticReportGroupingService.class
    );
    UserServiceClient userServiceClient = mock(UserServiceClient.class);
    Clock lagosClock = Clock.fixed(
      Instant.parse("2026-06-22T12:30:00Z"),
      ZoneId.of("Africa/Lagos")
    );
    ReportSchedulerJob scheduler = new ReportSchedulerJob(
      scheduleRepository,
      reportRepository,
      asyncGenerator,
      groupingService,
      lagosClock,
      userServiceClient
    );

    ReportSchedule schedule = new ReportSchedule();
    schedule.setId(UUID.randomUUID());
    schedule.setName("Rapport agences");
    schedule.setType(ReportType.DAILY);
    schedule.setFormat(ReportFormat.PDF);
    schedule.setScope("agency");
    schedule.setSendTime(LocalTime.of(13, 30));
    schedule.setCreatedBy(UUID.randomUUID());
    schedule.setActive(true);
    UserClientResponse creator = new UserClientResponse();
    creator.setId(schedule.getCreatedBy());
    creator.setActive(true);
    creator.setPermissions(Set.of("REPORT_GENERATE"));
    when(userServiceClient.getUserById(schedule.getCreatedBy())).thenReturn(
      creator
    );
    when(scheduleRepository.findByIsActiveTrue()).thenReturn(List.of(schedule));
    when(reportRepository.save(any(GeneratedReport.class))).thenAnswer(
      invocation -> {
        GeneratedReport report = invocation.getArgument(0);
        report.setId(UUID.randomUUID());
        return report;
      }
    );

    int triggered = scheduler.triggerNow();

    assertThat(triggered).isEqualTo(1);
    verify(groupingService).populate(
      argThat(report ->
        report
          .getPeriodStart()
          .toLocalDate()
          .equals(LocalDate.of(2026, 6, 22))
      ),
      eq("agency")
    );
    verify(reportRepository).save(
      argThat(report ->
        report.getFilters() != null &&
        report.getFilters().contains("\"documentLanguage\":\"fr\"")
      )
    );
    verify(asyncGenerator).generate(any(UUID.class));
  }

  @Test
  void triggerNow_SkipsScheduleWhenGeneratePermissionWasRevoked() {
    ReportScheduleRepository scheduleRepository = mock(
      ReportScheduleRepository.class
    );
    GeneratedReportRepository reportRepository = mock(
      GeneratedReportRepository.class
    );
    UserServiceClient userServiceClient = mock(UserServiceClient.class);
    ReportSchedulerJob scheduler = new ReportSchedulerJob(
      scheduleRepository,
      reportRepository,
      mock(AsyncReportGenerator.class),
      mock(AutomaticReportGroupingService.class),
      Clock.systemUTC(),
      userServiceClient
    );
    ReportSchedule schedule = new ReportSchedule();
    schedule.setId(UUID.randomUUID());
    schedule.setCreatedBy(UUID.randomUUID());
    schedule.setActive(true);
    UserClientResponse creator = new UserClientResponse();
    creator.setId(schedule.getCreatedBy());
    creator.setActive(true);
    creator.setPermissions(Set.of("REPORT_VIEW_OWN"));
    when(scheduleRepository.findByIsActiveTrue()).thenReturn(List.of(schedule));
    when(userServiceClient.getUserById(schedule.getCreatedBy())).thenReturn(
      creator
    );

    int triggered = scheduler.triggerNow();

    assertThat(triggered).isZero();
    verify(reportRepository, never()).save(any());
  }

  @Test
  void isLastBusinessDayOfMonth_RollsBackFromSundayToSaturday() {
    // Decembre 2023 se termine un dimanche (31) : le dernier jour ouvrable est
    // le samedi 30.
    assertThat(
      ReportSchedulerJob.isLastBusinessDayOfMonth(LocalDate.of(2023, 12, 30))
    ).isTrue();
    assertThat(
      ReportSchedulerJob.isLastBusinessDayOfMonth(LocalDate.of(2023, 12, 31))
    ).isFalse();
    assertThat(
      ReportSchedulerJob.isLastBusinessDayOfMonth(LocalDate.of(2023, 12, 29))
    ).isFalse();
  }

  @Test
  void isLastBusinessDayOfMonth_UsesLastDayWhenNotSunday() {
    // Janvier 2024 se termine un mercredi (31) : c'est le dernier jour ouvrable.
    assertThat(
      ReportSchedulerJob.isLastBusinessDayOfMonth(LocalDate.of(2024, 1, 31))
    ).isTrue();
    assertThat(
      ReportSchedulerJob.isLastBusinessDayOfMonth(LocalDate.of(2024, 1, 30))
    ).isFalse();
  }
}
