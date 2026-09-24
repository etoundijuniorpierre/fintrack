// Tests backend : verifie l'orchestration d'un rapport manuel par type.

package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fintrack.reporting.client.audit.AuditServiceClientService;
import com.fintrack.reporting.client.incident.IncidentServiceClientService;
import com.fintrack.reporting.client.notification.NotificationClientService;
import com.fintrack.reporting.client.user.UserServiceClientService;
import com.fintrack.reporting.model.constant.ReportContentType;
import com.fintrack.reporting.model.constant.ReportFormat;
import com.fintrack.reporting.model.constant.ReportType;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.report.ReportDocumentGenerator;
import com.fintrack.reporting.repository.GeneratedReportRepository;
import com.fintrack.reporting.security.UserDetailsImpl;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import tools.jackson.databind.json.JsonMapper;

// Garantit un chargement unique des incidents sans appel inutile aux metriques dashboard.
class ReportServiceIncidentTypeIntegrationTest {

  @Test
  void shouldGenerateTypeAnalysisFromOneIncidentLoad() {
    GeneratedReportRepository repository = mock(GeneratedReportRepository.class);
    IncidentServiceClientService incidentClient = mock(
      IncidentServiceClientService.class
    );
    AsyncReportGenerator asyncGenerator = mock(AsyncReportGenerator.class);
    JsonMapper mapper = new JsonMapper();
    ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    messages.setBasename("i18n/messages");
    messages.setDefaultEncoding("UTF-8");
    ReportServiceImpl service = new ReportServiceImpl(
      repository,
      mock(AuditServiceClientService.class),
      incidentClient,
      mock(AutomaticReportGroupingService.class),
      new IncidentTypeReportBuilder(),
      new IncidentStatusReportBuilder(),
      mock(UserServiceClientService.class),
      mock(ReportDocumentGenerator.class),
      asyncGenerator,
      mapper,
      mock(NotificationClientService.class),
      messages
    );
    UUID userId = UUID.randomUUID();
    UserDetailsImpl user = new UserDetailsImpl(
      userId,
      "agent",
      "password",
      true,
      List.of()
    );
    GeneratedReport report = new GeneratedReport();
    report.setName("Analyse par type");
    report.setType(ReportType.MONTHLY);
    report.setContentType(ReportContentType.INCIDENT_TYPE_ANALYSIS);
    report.setFormat(ReportFormat.PDF);
    report.setFilters(
      "{\"filters\":{\"view\":\"own\"},\"requestedMetrics\":[]}"
    );
    when(
      incidentClient.getIncidentsAsMaps(
        anyString(),
        anyList(),
        anyList(),
        anyList(),
        nullable(String.class),
        nullable(String.class),
        nullable(String.class),
        nullable(String.class),
        nullable(String.class),
        any(),
        any(),
        any(),
        nullable(String.class),
        nullable(String.class)
      )
    ).thenReturn(
      List.of(
        Map.of(
          "status",
          "OPEN",
          "type",
          Map.of("id", "cash", "displayName", "Caisse")
        )
      )
    );
    when(repository.save(any(GeneratedReport.class))).thenAnswer(invocation -> {
      GeneratedReport saved = invocation.getArgument(0);
      saved.setId(UUID.randomUUID());
      return saved;
    });

    GeneratedReport generated = service.generate(report, user);

    assertThat(generated.getMetrics())
      .contains("INCIDENT_TYPE_ANALYSIS", "Caisse", "100.0");
    verify(incidentClient, times(1)).getIncidentsAsMaps(
      anyString(),
      anyList(),
      anyList(),
      anyList(),
      nullable(String.class),
      nullable(String.class),
      nullable(String.class),
      nullable(String.class),
      nullable(String.class),
      any(),
      any(),
      any(),
      nullable(String.class),
      nullable(String.class)
    );
    verify(incidentClient, never()).getDashboardMetricsRaw(
      anyString(),
      nullable(String.class),
      nullable(String.class),
      nullable(String.class),
      nullable(java.time.LocalDateTime.class),
      nullable(java.time.LocalDateTime.class)
    );
    verify(asyncGenerator).generate(generated.getId());
  }
}
