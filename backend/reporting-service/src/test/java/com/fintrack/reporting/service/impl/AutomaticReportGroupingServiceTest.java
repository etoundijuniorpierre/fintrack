// Tests backend : verifie les regroupements individuels des rapports automatiques.
package com.fintrack.reporting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.reporting.client.incident.IncidentServiceClientService;
import com.fintrack.reporting.client.user.UserServiceClient;
import com.fintrack.reporting.client.user.UserServiceClientService;
import com.fintrack.reporting.client.user.dto.AgencyClientResponse;
import com.fintrack.reporting.client.user.dto.ServiceClientResponse;
import com.fintrack.reporting.client.user.dto.UserClientResponse;
import com.fintrack.reporting.model.entity.GeneratedReport;
import com.fintrack.reporting.model.constant.ReportContentType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AutomaticReportGroupingServiceTest {

  @Mock
  private IncidentServiceClientService incidentServiceClientService;

  @Mock
  private UserServiceClientService userServiceClientService;

  @Mock
  private UserServiceClient userServiceClient;

  @Mock
  private MessageSource messageSource;

  @Test
  void populate_All_RanksResolversOfClosedIncidentsByCountThenName()
    throws Exception {
    JsonMapper mapper = new JsonMapper();
    AutomaticReportGroupingService service = new AutomaticReportGroupingService(
      incidentServiceClientService,
      userServiceClientService,
      userServiceClient,
      mapper,
      messageSource,
      new IncidentTypeReportBuilder(),
      new IncidentStatusReportBuilder()
    );
    UUID creatorId = UUID.randomUUID();
    UserClientResponse creator = new UserClientResponse();
    creator.setId(creatorId);
    creator.setPermissions(Set.of("REPORT_GENERATE_ALL_SCOPES"));
    when(userServiceClient.getUserById(creatorId)).thenReturn(creator);
    when(
      incidentServiceClientService.getIncidentsAsMaps(
        anyString(),
        anyList(),
        anyList(),
        anyList(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull()
      )
    ).thenReturn(
      List.of(
        closedIncidentResolvedBy("Charlie", "Declarant"),
        closedIncidentAssignedTo("Bob"),
        closedIncidentAssignedTo("Alice"),
        closedIncidentResolvedBy("Charlie", "Declarant")
      )
    );
    GeneratedReport report = new GeneratedReport();
    report.setCreatedBy(creatorId);

    service.populate(report, "all");

    Map<String, Object> metrics = mapper.readValue(
      report.getMetrics(),
      new TypeReference<>() {}
    );
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> ranking =
      (List<Map<String, Object>>) metrics.get("topResolvers");
    assertThat(ranking)
      .extracting(row -> row.get("name"))
      .containsExactly("Charlie", "Alice", "Bob");
    assertThat(ranking)
      .extracting(row -> row.get("count"))
      .containsExactly(2, 1, 1);
  }

  @Test
  void populate_All_StatusOverview_AddsGlobalSituationFromDashboard()
    throws Exception {
    JsonMapper mapper = new JsonMapper();
    AutomaticReportGroupingService service = new AutomaticReportGroupingService(
      incidentServiceClientService,
      userServiceClientService,
      userServiceClient,
      mapper,
      messageSource,
      new IncidentTypeReportBuilder(),
      new IncidentStatusReportBuilder()
    );
    UUID creatorId = UUID.randomUUID();
    UserClientResponse creator = new UserClientResponse();
    creator.setId(creatorId);
    creator.setPermissions(Set.of("REPORT_GENERATE_ALL_SCOPES"));
    when(userServiceClient.getUserById(creatorId)).thenReturn(creator);
    when(
      incidentServiceClientService.getIncidentsAsMaps(
        anyString(),
        anyList(),
        anyList(),
        anyList(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull()
      )
    ).thenReturn(List.of(openIncident(), openIncident()));
    when(
      incidentServiceClientService.getDashboardMetricsRaw(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    ).thenReturn(
      Map.of(
        "treatedIncidents", 4,
        "resolvedIncidents", 2,
        "closedIncidents", 1
      )
    );
    when(
      incidentServiceClientService.getPeriodActivityAsMaps(
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    ).thenReturn(new java.util.HashMap<>());
    GeneratedReport report = new GeneratedReport();
    report.setCreatedBy(creatorId);
    report.setContentType(
      com.fintrack.reporting.model.constant.ReportContentType.INCIDENT_STATUS_OVERVIEW
    );

    service.populate(report, "all");

    Map<String, Object> metrics = mapper.readValue(
      report.getMetrics(),
      new TypeReference<>() {}
    );
    @SuppressWarnings("unchecked")
    Map<String, Object> situation =
      (Map<String, Object>) metrics.get("globalSituation");
    // Flux de la periode : issu du tableau de bord.
    assertThat(situation)
      .containsEntry("treatedInPeriod", 4)
      .containsEntry("resolvedInPeriod", 2)
      .containsEntry("closedInPeriod", 1);
    // Backlog : adosse a la liste reelle des incidents ouverts (2 incidents).
    assertThat(situation).containsEntry("untreatedBacklog", 2);
    assertThat(situation).containsKey("categorySections");
    assertThat(situation).containsKey("incidentsList");
  }

  // Incident ouvert minimal pour alimenter le backlog du rapport.
  private Map<String, Object> openIncident() {
    Map<String, Object> incident = new java.util.HashMap<>();
    incident.put("status", "IN_PROGRESS");
    return incident;
  }

  @Test
  void populate_All_GroupsClosuresByClosedAtRatherThanResolvedAt()
    throws Exception {
    JsonMapper mapper = new JsonMapper();
    AutomaticReportGroupingService service = new AutomaticReportGroupingService(
      incidentServiceClientService,
      userServiceClientService,
      userServiceClient,
      mapper,
      messageSource,
      new IncidentTypeReportBuilder(),
      new IncidentStatusReportBuilder()
    );
    UUID creatorId = UUID.randomUUID();
    UserClientResponse creator = new UserClientResponse();
    creator.setId(creatorId);
    creator.setPermissions(Set.of("REPORT_GENERATE_ALL_SCOPES"));
    when(userServiceClient.getUserById(creatorId)).thenReturn(creator);
    when(
      incidentServiceClientService.getIncidentsAsMaps(
        anyString(),
        anyList(),
        anyList(),
        anyList(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull()
      )
    ).thenReturn(
      List.of(
        Map.of(
          "status",
          "CLOSED",
          "createdAt",
          "2026-04-01T08:00:00",
          "resolvedAt",
          "2026-05-31T16:00:00",
          "closedAt",
          "2026-06-01T09:00:00"
        )
      )
    );
    GeneratedReport report = new GeneratedReport();
    report.setCreatedBy(creatorId);

    service.populate(report, "all");

    Map<String, Object> metrics = mapper.readValue(
      report.getMetrics(),
      new TypeReference<>() {}
    );
    assertThat(metrics.get("monthlyClosures")).isEqualTo(
      Map.of("2026-06", 1)
    );
  }

  @Test
  void populate_TypeAnalysis_OmitsEmptyEntitiesAndKeepsOneIncidentLoad()
    throws Exception {
    JsonMapper mapper = new JsonMapper();
    AutomaticReportGroupingService service = new AutomaticReportGroupingService(
      incidentServiceClientService,
      userServiceClientService,
      userServiceClient,
      mapper,
      messageSource,
      new IncidentTypeReportBuilder(),
      new IncidentStatusReportBuilder()
    );
    UUID creatorId = UUID.randomUUID();
    UUID populatedAgencyId = UUID.randomUUID();
    UserClientResponse creator = new UserClientResponse();
    creator.setId(creatorId);
    creator.setPermissions(Set.of("REPORT_GENERATE_ALL_SCOPES"));
    when(userServiceClient.getUserById(creatorId)).thenReturn(creator);
    when(userServiceClientService.getAgencies()).thenReturn(
      List.of(
        agency(populatedAgencyId, "Agence active"),
        agency(UUID.randomUUID(), "Agence vide")
      )
    );
    when(
      incidentServiceClientService.getIncidentsAsMaps(
        anyString(),
        anyList(),
        anyList(),
        anyList(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull()
      )
    ).thenReturn(
      List.of(
        Map.of(
          "status",
          "OPEN",
          "type",
          Map.of("id", "cash", "displayName", "Caisse"),
          "agency",
          Map.of("id", populatedAgencyId.toString())
        )
      )
    );
    GeneratedReport report = new GeneratedReport();
    report.setCreatedBy(creatorId);
    report.setContentType(ReportContentType.INCIDENT_TYPE_ANALYSIS);

    service.populate(report, "agency");

    Map<String, Object> metrics = mapper.readValue(
      report.getMetrics(),
      new TypeReference<>() {}
    );
    assertThat((List<?>) metrics.get("groupedReports")).hasSize(1);
    assertThat(report.getMetrics())
      .contains("INCIDENT_TYPE_ANALYSIS", "Agence active")
      .doesNotContain("Agence vide");
    verify(incidentServiceClientService).getIncidentsAsMaps(
      anyString(),
      anyList(),
      anyList(),
      anyList(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull()
    );
  }

  private Map<String, Object> closedIncidentAssignedTo(String name) {
    return Map.of(
      "status",
      "CLOSED",
      "assignedTo",
      Map.of("name", name)
    );
  }

  private Map<String, Object> closedIncidentResolvedBy(
    String resolver,
    String closer
  ) {
    return Map.of(
      "status",
      "CLOSED",
      "assignedTo",
      Map.of("name", "Ancien traitant"),
      "history",
      List.of(
        Map.of(
          "action",
          "STATUS_CHANGE",
          "newValue",
          "RESOLVED",
          "createdAt",
          "2026-06-10T10:00:00",
          "user",
          Map.of("name", resolver)
        ),
        Map.of(
          "action",
          "STATUS_CHANGE",
          "newValue",
          "CLOSED",
          "createdAt",
          "2026-06-11T10:00:00",
          "user",
          Map.of("name", closer)
        )
      )
    );
  }

  @Test
  void populate_AgencyGrouping_CreatesOneSectionPerAgencyWithSingleIncidentLoad()
    throws Exception {
    JsonMapper mapper = new JsonMapper();
    AutomaticReportGroupingService service = new AutomaticReportGroupingService(
      incidentServiceClientService,
      userServiceClientService,
      userServiceClient,
      mapper,
      messageSource,
      new IncidentTypeReportBuilder(),
      new IncidentStatusReportBuilder()
    );
    UUID creatorId = UUID.randomUUID();
    UUID agencyOneId = UUID.randomUUID();
    UUID agencyTwoId = UUID.randomUUID();
    UserClientResponse creator = new UserClientResponse();
    creator.setId(creatorId);
    creator.setPermissions(Set.of("REPORT_GENERATE_ALL_SCOPES"));
    when(userServiceClient.getUserById(creatorId)).thenReturn(creator);
    when(userServiceClientService.getAgencies()).thenReturn(
      List.of(
        agency(agencyOneId, "Agence Centre"),
        agency(agencyTwoId, "Agence Nord")
      )
    );
    when(
      incidentServiceClientService.getIncidentsAsMaps(
        anyString(),
        anyList(),
        anyList(),
        anyList(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        anyString(),
        anyString()
      )
    ).thenReturn(
      List.of(
        Map.of(
          "status",
          "OPEN",
          "criticality",
          "HIGH",
          "type",
          Map.of("displayName", "Caisse"),
          "agency",
          Map.of("id", agencyOneId.toString(), "name", "Agence Centre")
        )
      )
    );

    GeneratedReport report = new GeneratedReport();
    report.setCreatedBy(creatorId);
    report.setPeriodStart(LocalDateTime.of(2026, 6, 1, 0, 0));
    report.setPeriodEnd(LocalDateTime.of(2026, 6, 30, 23, 59));

    service.populate(report, "agency");

    Map<String, Object> metrics = mapper.readValue(
      report.getMetrics(),
      new TypeReference<>() {}
    );
    List<?> groups = (List<?>) metrics.get("groupedReports");
    assertThat(groups).hasSize(2);
    assertThat(report.getMetrics()).contains(
      "Agence Centre",
      "Agence Nord",
      "totalIncidents"
    );
    verify(incidentServiceClientService).getIncidentsAsMaps(
      anyString(),
      anyList(),
      anyList(),
      anyList(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      anyString(),
      anyString()
    );
  }

  @Test
  void populate_GroupedOperationalReport_KeepsOnlyRequestedAvailableMetrics()
    throws Exception {
    JsonMapper mapper = new JsonMapper();
    AutomaticReportGroupingService service = new AutomaticReportGroupingService(
      incidentServiceClientService,
      userServiceClientService,
      userServiceClient,
      mapper,
      messageSource,
      new IncidentTypeReportBuilder(),
      new IncidentStatusReportBuilder()
    );
    UUID creatorId = UUID.randomUUID();
    UUID agencyId = UUID.randomUUID();
    UserClientResponse creator = new UserClientResponse();
    creator.setId(creatorId);
    creator.setPermissions(Set.of("REPORT_GENERATE_ALL_SCOPES"));
    when(userServiceClient.getUserById(creatorId)).thenReturn(creator);
    when(userServiceClientService.getAgencies()).thenReturn(
      List.of(agency(agencyId, "Agence Centre"))
    );
    when(
      incidentServiceClientService.getIncidentsAsMaps(
        anyString(),
        anyList(),
        anyList(),
        anyList(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull()
      )
    ).thenReturn(
      List.of(
        Map.of(
          "status",
          "OPEN",
          "criticality",
          "HIGH",
          "agency",
          Map.of("id", agencyId.toString())
        )
      )
    );
    GeneratedReport report = new GeneratedReport();
    report.setCreatedBy(creatorId);
    report.setFilters(
      mapper.writeValueAsString(
        Map.of(
          "requestedMetrics",
          List.of("totalIncidents", "medianResolutionHours")
        )
      )
    );

    service.populate(report, "agency");

    Map<String, Object> metrics = mapper.readValue(
      report.getMetrics(),
      new TypeReference<>() {}
    );
    @SuppressWarnings("unchecked")
    Map<String, Object> group = (Map<String, Object>) ((List<?>) metrics.get(
        "groupedReports"
      )).getFirst();
    @SuppressWarnings("unchecked")
    Map<String, Object> groupMetrics = (Map<String, Object>) group.get(
      "metrics"
    );
    assertThat(groupMetrics)
      .containsKeys("totalIncidents", "incidentsList")
      .doesNotContainKeys("distributionByType", "medianResolutionHours");
    assertThat(metrics.get("unavailableMetrics")).isEqualTo(
      List.of("medianResolutionHours")
    );
  }

  @Test
  void populate_ServiceGrouping_ForAgencyAccess_ExcludesServicesOutsideAgency()
    throws Exception {
    JsonMapper mapper = new JsonMapper();
    AutomaticReportGroupingService service = new AutomaticReportGroupingService(
      incidentServiceClientService,
      userServiceClientService,
      userServiceClient,
      mapper,
      messageSource,
      new IncidentTypeReportBuilder(),
      new IncidentStatusReportBuilder()
    );
    UUID creatorId = UUID.randomUUID();
    UUID agencyId = UUID.randomUUID();
    UUID permittedServiceId = UUID.randomUUID();
    UUID otherServiceId = UUID.randomUUID();
    UserClientResponse creator = user(creatorId, agencyId, permittedServiceId);
    creator.setPermissions(Set.of("REPORT_VIEW_AGENCY"));
    when(userServiceClient.getUserById(creatorId)).thenReturn(creator);
    when(userServiceClientService.getServices()).thenReturn(
      List.of(
        service(permittedServiceId, "Comptabilite"),
        service(otherServiceId, "Juridique")
      )
    );
    when(userServiceClientService.getReportSubjects()).thenReturn(
      List.of(creator)
    );
    when(
      incidentServiceClientService.getIncidentsAsMaps(
        eq("agency"),
        anyList(),
        anyList(),
        anyList(),
        isNull(),
        isNull(),
        isNull(),
        eq(agencyId.toString()),
        isNull(),
        isNull(),
        isNull()
      )
    ).thenReturn(List.of(Map.of("status", "OPEN")));
    when(
      messageSource.getMessage(
        eq("report.group.unassignedService"),
        isNull(),
        eq("report.group.unassignedService"),
        any()
      )
    ).thenReturn("Sans service");

    GeneratedReport report = new GeneratedReport();
    report.setCreatedBy(creatorId);
    service.populate(report, "service");

    Map<String, Object> metrics = mapper.readValue(
      report.getMetrics(),
      new TypeReference<>() {}
    );
    List<?> groups = (List<?>) metrics.get("groupedReports");
    assertThat(groups).hasSize(2);
    assertThat(report.getMetrics())
      .contains("Comptabilite", "Sans service")
      .doesNotContain("Juridique");
  }

  @Test
  void populate_UserGrouping_IncludesHistoryParticipantsWithoutExtraIncidentLoads()
    throws Exception {
    JsonMapper mapper = new JsonMapper();
    AutomaticReportGroupingService service = new AutomaticReportGroupingService(
      incidentServiceClientService,
      userServiceClientService,
      userServiceClient,
      mapper,
      messageSource,
      new IncidentTypeReportBuilder(),
      new IncidentStatusReportBuilder()
    );
    UUID creatorId = UUID.randomUUID();
    UUID validatorId = UUID.randomUUID();
    UserClientResponse creator = user(creatorId, null, null);
    creator.setPermissions(Set.of("REPORT_GENERATE_ALL_SCOPES"));
    UserClientResponse validator = user(validatorId, null, null);
    validator.setUsername("validator");
    when(userServiceClient.getUserById(creatorId)).thenReturn(creator);
    when(userServiceClientService.getReportSubjects()).thenReturn(
      List.of(creator, validator)
    );
    when(
      incidentServiceClientService.getIncidentsAsMaps(
        anyString(),
        anyList(),
        anyList(),
        anyList(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull(),
        isNull()
      )
    ).thenReturn(
      List.of(
        Map.of(
          "status",
          "OPEN",
          "participantUserIds",
          List.of(validatorId.toString())
        )
      )
    );

    GeneratedReport report = new GeneratedReport();
    report.setCreatedBy(creatorId);

    service.populate(report, "user");

    Map<String, Object> metrics = mapper.readValue(
      report.getMetrics(),
      new TypeReference<>() {}
    );
    List<?> groups = (List<?>) metrics.get("groupedReports");
    assertThat(groups).hasSize(2);
    assertThat(report.getMetrics()).contains(
      "validator",
      "\"totalIncidents\":1"
    );
    verify(incidentServiceClientService).getIncidentsAsMaps(
      anyString(),
      anyList(),
      anyList(),
      anyList(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull(),
      isNull()
    );
  }

  private AgencyClientResponse agency(UUID id, String name) {
    AgencyClientResponse agency = new AgencyClientResponse();
    agency.setId(id);
    agency.setName(name);
    agency.setActive(true);
    return agency;
  }

  private ServiceClientResponse service(UUID id, String name) {
    ServiceClientResponse service = new ServiceClientResponse();
    service.setId(id);
    service.setName(name);
    service.setActive(true);
    return service;
  }

  private UserClientResponse user(UUID id, UUID agencyId, UUID serviceId) {
    UserClientResponse user = new UserClientResponse();
    user.setId(id);
    user.setUsername("manager");
    user.setAgencyId(agencyId);
    user.setServiceId(serviceId);
    user.setActive(true);
    return user;
  }
}
