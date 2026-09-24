package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.incident.IncidentServiceClientService;
import com.fintrack.reporting.client.incident.dto.IncidentClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentDashboardMetricsClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentTypeConfigClientResponse;
import com.fintrack.reporting.client.superadmin.SuperAdminAuditClient;
import com.fintrack.reporting.client.superadmin.SuperAdminHealthClient;
import com.fintrack.reporting.client.superadmin.SuperAdminNotificationClient;
import com.fintrack.reporting.client.superadmin.SuperAdminStatsClientService;
import com.fintrack.reporting.client.superadmin.SuperAdminUserClient;
import com.fintrack.reporting.client.superadmin.dto.AgencyClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AuditLogClientResponse;
import com.fintrack.reporting.client.superadmin.dto.AuditStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.ServiceClientResponse;
import com.fintrack.reporting.client.superadmin.dto.SuperAdminUserClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserPermissionClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserRoleClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserStatsClientResponse;
import com.fintrack.reporting.client.superadmin.dto.UserSummaryClientResponse;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportGenerationType;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.dto.response.superadmin.DataQualityIssueResponse;
import com.fintrack.reporting.model.dto.response.superadmin.SuperAdminOverviewResponse;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.mapper.superadmin.DataQualityBuilder;
import com.fintrack.reporting.model.mapper.superadmin.NotificationsBuilder;
import com.fintrack.reporting.model.mapper.superadmin.SuperAdminMapper;
import com.fintrack.reporting.model.mapper.superadmin.SystemHealthBuilder;
import com.fintrack.reporting.model.readmodel.superadmin.HealthProbe;
import com.fintrack.reporting.model.readmodel.superadmin.SystemThresholds;
import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.service.AuditExportService;
import com.fintrack.reporting.service.SystemConfigService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SuperAdminServiceImplTest {

  @Mock
  private IncidentServiceClientService incidentService;

  @Mock
  private SuperAdminUserClient userClient;

  @Mock
  private SuperAdminNotificationClient notificationClient;

  @Mock
  private SuperAdminAuditClient auditClient;

  @Mock
  private GeneratedReportRepository reportRepository;

  @Mock
  private SuperAdminHealthClient healthClient;

  @Mock
  private SystemConfigService systemConfigService;

  @Mock
  private AuditExportService auditExportService;

  @Mock
  private AuditServiceClientService auditService;

  @Mock
  private com.fintrack.reporting.service.BackupService backupService;

  @Mock
  private org.springframework.context.MessageSource messageSource;

  private SuperAdminServiceImpl service;
  private SuperAdminMapper superAdminMapper;

  @BeforeEach
  void setUp() {
    SystemHealthBuilder systemHealthBuilder = new SystemHealthBuilder();
    NotificationsBuilder notificationsBuilder = new NotificationsBuilder(
      systemConfigService
    );
    DataQualityBuilder dataQualityBuilder = new DataQualityBuilder();
    SuperAdminStatsClientService statsClientService =
      new SuperAdminStatsClientService(
        notificationClient,
        auditClient,
        userClient
      );
    superAdminMapper = new SuperAdminMapper(
      dataQualityBuilder,
      systemHealthBuilder,
      notificationsBuilder
    );

    service = new SuperAdminServiceImpl(
      incidentService,
      userClient,
      statsClientService,
      reportRepository,
      systemConfigService,
      auditExportService,
      auditService,
      healthClient,
      backupService,
      messageSource,
      "http://user-service:8081",
      "http://incident-service:8082",
      "http://document-service:8083",
      "http://notification-service:8084",
      "FinTrack - Reporting Service",
      "http://audit-service:8087"
    );
    ReflectionTestUtils.setField(service, "reportsFetchSize", 1000);
    ReflectionTestUtils.setField(service, "statsSampleSize", 20);

    // Comportement de configuration systeme par defaut : les seuils de repli sont retournes.
    when(systemConfigService.getThresholds()).thenReturn(
      SystemThresholds.builder()
        .defaultSlaHours(24L)
        .criticalIncidentHours(4L)
        .slaReminderIntervalHours(24L)
        .maxTransfersBeforeAlert(2L)
        .notificationMaxRetryCount(5L)
        .loginMaxFailedAttempts(5L)
        .tempPasswordValidityMinutes(30L)
        .escalationScanIntervalMinutes(15L)
        .build()
    );
    when(
      systemConfigService.getThresholdLong(anyString(), anyLong())
    ).thenAnswer(invocation -> invocation.getArgument(1));

    // Health probe stub.
    when(healthClient.probe(anyString(), anyString())).thenAnswer(
      invocation -> {
        String baseUrl = invocation.getArgument(1, String.class);
        return HealthProbe.builder()
          .key(invocation.getArgument(0))
          .baseUrl(baseUrl)
          .status("UP")
          .responseTimeMs(5L)
          .endpoint(baseUrl + "/actuator/health")
          .build();
      }
    );

    when(incidentService.getIncidentTypeConfigsOrThrow()).thenReturn(List.of());
  }

  @Test
  @DisplayName(
    "getOverview aggregates governance, quality, reporting and notification data"
  )
  void getOverview_AggregatesCrossServiceData() {
    UUID inactiveUserId = UUID.randomUUID();
    UUID agencyId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();

    IncidentTypeConfigClientResponse typeConfig =
      new IncidentTypeConfigClientResponse(
        UUID.randomUUID().toString(),
        "TYPE",
        "Type Display Name",
        "Type Description",
        true,
        24
      );
    UserSummaryClientResponse inactiveUserSummary =
      UserSummaryClientResponse.builder()
        .id(inactiveUserId)
        .username("inactive")
        .email("inactive@fintrack.com")
        .build();
    AgencyClientResponse agency1 = new AgencyClientResponse(
      agencyId,
      "Finstar",
      "FIN",
      true,
      null,
      List.of()
    );
    ServiceClientResponse service1 = new ServiceClientResponse(
      serviceId,
      "IT",
      "IT Service",
      true,
      null,
      List.of()
    );

    when(
      incidentService.getDashboardMetricsOrThrow("all", null, null)
    ).thenReturn(
      new IncidentDashboardMetricsClientResponse(
        30L,
        2L,
        12.34,
        9.5,
        30.0,
        List.of(),
        List.of()
      )
    );

    when(
      incidentService.getIncidentsOrThrow(
        "all",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
      )
    ).thenReturn(
      List.of(
        IncidentClientResponse.builder()
          .id(UUID.randomUUID().toString())
          .title("inc-1")
          .description("desc-1")
          .type(typeConfig)
          .criticality("CRITICAL")
          .status("IN_PROGRESS")
          .reopenCount(0)
          .transferCount(0)
          .agency(agency1)
          .createdAt("2026-06-11T12:00:00")
          .participantUserIds(Set.of())
          .build(),
        IncidentClientResponse.builder()
          .id(UUID.randomUUID().toString())
          .title("inc-2")
          .description("desc-2")
          .type(typeConfig)
          .criticality("LOW")
          .status("REJECTED")
          .reopenCount(0)
          .transferCount(0)
          .createdAt("2026-06-11T12:00:00")
          .participantUserIds(Set.of())
          .build(),
        IncidentClientResponse.builder()
          .id(UUID.randomUUID().toString())
          .title("inc-3")
          .description("desc-3")
          .type(typeConfig)
          .criticality("LOW")
          .status("TRANSFERRED")
          .assignedTo(inactiveUserSummary)
          .transferredToService(service1)
          .reopenCount(0)
          .transferCount(3)
          .agency(agency1)
          .createdAt("2026-06-11T12:00:00")
          .creatorServiceId(serviceId != null ? serviceId.toString() : null)
          .participantUserIds(Set.of())
          .build()
      )
    );

    when(userClient.getUsers()).thenReturn(
      List.of(
        new SuperAdminUserClientResponse(
          inactiveUserId,
          "inactive",
          "inactive@fintrack.com",
          false,
          List.of(
            new UserRoleClientResponse("USER", "User", "User", true, List.of())
          ),
          null,
          null,
          null,
          null
        ),
        new SuperAdminUserClientResponse(
          UUID.randomUUID(),
          "norole",
          "norole@fintrack.com",
          true,
          List.of(),
          null,
          null,
          null,
          null
        )
      )
    );

    when(userClient.getUserStats("ALL", "all")).thenReturn(
      new UserStatsClientResponse(1L, 1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)
    );

    when(userClient.getRoles()).thenReturn(
      List.of(
        new UserRoleClientResponse(
          "SUPER_ADMIN",
          "Super Admin",
          "Super Admin Description",
          true,
          List.of(
            new UserPermissionClientResponse(
              "INCIDENT_VIEW_ALL",
              "View all incidents"
            )
          )
        )
      )
    );

    when(userClient.getAgencies()).thenReturn(List.of(agency1));
    when(userClient.getServices()).thenReturn(List.of(service1));
    when(userClient.getPermissions()).thenReturn(
      List.of(
        new UserPermissionClientResponse(
          "INCIDENT_VIEW_ALL",
          "View all incidents"
        )
      )
    );

    NotificationClientResponse failedNotification =
      new NotificationClientResponse(
        "n2",
        "EMAIL",
        "bad-recipient",
        1,
        null,
        "FAILED",
        null,
        "Echec envoi email",
        "Email delivery failed"
      );
    NotificationStatsClientResponse notificationStats =
      new NotificationStatsClientResponse(
        2L,
        1L,
        1L,
        0L,
        Map.of("SENT", 1L, "FAILED", 1L),
        Map.of("EMAIL", 2L),
        1L,
        0L,
        List.of(failedNotification),
        List.of(failedNotification),
        List.of(),
        Map.of()
      );
    when(notificationClient.getStats(any(), any(), anyInt())).thenReturn(
      notificationStats
    );

    AuditLogClientResponse auditLog = new AuditLogClientResponse(
      UUID.randomUUID().toString(),
      null,
      "root",
      null,
      "ROLE_UPDATE",
      "SUCCESS",
      null,
      null,
      null,
      null
    );
    AuditStatsClientResponse auditStats = new AuditStatsClientResponse(
      1L,
      Map.of("ROLE_UPDATE", 1L),
      Map.of("SUCCESS", 1L),
      0L,
      List.of(auditLog),
      List.of(),
      List.of(auditLog),
      List.of(),
      Map.of()
    );
    when(auditClient.getStats(any(), any(), anyInt(), anyInt())).thenReturn(
      auditStats
    );

    when(reportRepository.findAll(any(Pageable.class))).thenReturn(
      new PageImpl<>(
        List.of(
          report("available", "AVAILABLE", ReportGenerationType.MANUAL, 1024L),
          report("failed", "FAILED", ReportGenerationType.AUTOMATIC, null)
        )
      )
    );

    SuperAdminOverviewResponse overview = superAdminMapper.toOverviewResponse(
      service.getOverview()
    );

    assertThat(overview.getGovernance().getTotalIncidents()).isEqualTo(3L);
    assertThat(overview.getGovernance().getRejectedRate()).isEqualTo(33.3);
    assertThat(overview.getGovernance().getTransferredRate()).isEqualTo(33.3);
    assertThat(
      overview.getGovernance().getSuperAdminKpis().getUnassignedIncidents()
    ).isEqualTo(1L);
    assertThat(overview.getGovernance().getActiveUsers()).isEqualTo(1L);
    assertThat(overview.getReporting().getFailed()).isEqualTo(1);
    assertThat(overview.getNotifications().getFailed()).isEqualTo(1L);
    assertThat(overview.getDataQuality().getIssues())
      .extracting(DataQualityIssueResponse::getKey)
      .contains(
        "incidents-without-agency",
        "users-without-role",
        "incidents-assigned-inactive-user"
      );
    assertThat(overview.getDataQuality().getAgenciesWithoutHead()).hasSize(1);
    assertThat(overview.getDataQuality().getServicesWithoutHead()).hasSize(1);
    assertThat(overview.getSystemHealth()).hasSize(6);
    assertThat(overview.getAudit().getExportEndpoint()).isNotNull();
    assertThat(overview.getMeta().getGeneratedAt()).isNotBlank();
  }

  @Test
  @DisplayName("getOverview reports DOWN status when health probe returns DOWN")
  void getOverview_HealthFailure_ReturnsDownRow() {
    when(healthClient.probe(eq("user"), anyString())).thenAnswer(invocation -> {
      String baseUrl = invocation.getArgument(1, String.class);
      return HealthProbe.builder()
        .key("user")
        .baseUrl(baseUrl)
        .status("DOWN")
        .responseTimeMs(10L)
        .endpoint(baseUrl + "/actuator/health")
        .build();
    });
    when(
      incidentService.getDashboardMetricsOrThrow("all", null, null)
    ).thenReturn(
      new IncidentDashboardMetricsClientResponse(
        0,
        0,
        0.0,
        0.0,
        0.0,
        List.of(),
        List.of()
      )
    );
    when(
      incidentService.getIncidentsOrThrow(
        "all",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
      )
    ).thenReturn(List.of());
    when(reportRepository.findAll(any(Pageable.class))).thenReturn(
      new PageImpl<>(List.of())
    );

    SuperAdminOverviewResponse overview = superAdminMapper.toOverviewResponse(
      service.getOverview()
    );

    assertThat(overview.getSystemHealth()).anyMatch(
      row -> "user".equals(row.getKey()) && "DOWN".equals(row.getStatus())
    );
    assertThat(overview.getSystemHealth()).anyMatch(
      row -> "reporting".equals(row.getKey()) && "UP".equals(row.getStatus())
    );
  }

  private GeneratedReport report(
    String name,
    String status,
    ReportGenerationType generationType,
    Long fileSize
  ) {
    GeneratedReport report = new GeneratedReport();
    report.setId(UUID.randomUUID());
    report.setName(name);
    report.setType(ReportType.CUSTOM);
    report.setFormat(ReportFormat.PDF);
    report.setStatus(status);
    report.setGenerationType(generationType);
    report.setCreatedBy(UUID.randomUUID());
    report.setFilePath(fileSize == null ? null : "/tmp/" + name + ".pdf");
    report.setFileSize(fileSize);
    return report;
  }
}
