package com.fintrack.reporting.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.reporting.client.incident.IncidentServiceClient;
import com.fintrack.reporting.client.incident.dto.IncidentClientResponse;
import com.fintrack.reporting.client.incident.dto.IncidentsPageClientResponse;
import com.fintrack.reporting.client.superadmin.SuperAdminAuditClient;
import com.fintrack.reporting.client.superadmin.SuperAdminNotificationClient;
import com.fintrack.reporting.client.superadmin.dto.AuditLogsPageClientResponse;
import com.fintrack.reporting.client.superadmin.dto.NotificationStatsClientResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Filet anti-drift : vérifie que les clients typés du Super Admin désérialisent
// réellement le JSON des producteurs — en particulier les champs régressés
// (subject des notifications, creatorServiceId des incidents) et le jeu complet
// des métriques dashboard via la variante Map.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SuperAdminClientsWireMockTest {

  private static final WireMockServer WIREMOCK = new WireMockServer(
    WireMockConfiguration.options().dynamicPort()
  );

  static {
    WIREMOCK.start();
  }

  @DynamicPropertySource
  static void registerUrls(DynamicPropertyRegistry registry) {
    String url = "http://localhost:" + WIREMOCK.port();
    registry.add("incident.service.url", () -> url);
    registry.add("notification.service.url", () -> url);
    registry.add("audit.service.url", () -> url);
    registry.add(
      "spring.cloud.openfeign.client.config.incident-service.url",
      () -> url
    );
    registry.add(
      "spring.cloud.openfeign.client.config.notification-service.url",
      () -> url
    );
    registry.add(
      "spring.cloud.openfeign.client.config.audit-service.url",
      () -> url
    );
  }

  @AfterAll
  static void stopWireMock() {
    WIREMOCK.stop();
  }

  @BeforeEach
  void resetWireMock() {
    WIREMOCK.resetAll();
  }

  @Autowired
  private SuperAdminNotificationClient notificationClient;

  @Autowired
  private IncidentServiceClient incidentServiceClient;

  @Autowired
  private SuperAdminAuditClient auditClient;

  @Test
  @DisplayName(
    "getStats deserializes failedSample[].subject (guards SuperAdmin notifications regression)"
  )
  void getStats_deserializesSubject() {
    WIREMOCK.stubFor(
      get(
        urlPathEqualTo("/api/v1/notificationService/notifications/stats")
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "total": 5, "sent": 3, "failed": 2, "pending": 0,
              "byStatus": {"SENT": 3, "FAILED": 2},
              "byType": {"EMAIL": 5},
              "invalidRecipientCount": 1,
              "sentWithoutTraceCount": 0,
              "failedSample": [
                {"id":"n1","type":"EMAIL","recipient":"bad@x.com","retryCount":2,"nextRetry":null,"status":"FAILED","createdAt":"2026-06-01T10:00:00","subject":"Echec rapport mensuel"}
              ],
              "invalidRecipientsSample": [],
              "sentWithoutTraceSample": [],
              "byTypeAndStatus": {"EMAIL": {"SENT": 3, "FAILED": 2}}
            }
            """
          )
      )
    );

    NotificationStatsClientResponse stats = notificationClient.getStats(
      null,
      null,
      20
    );

    assertThat(stats.getFailed()).isEqualTo(2L);
    assertThat(stats.getFailedSample()).hasSize(1);
    assertThat(stats.getFailedSample().get(0).getSubject()).isEqualTo(
      "Echec rapport mensuel"
    );
  }

  @Test
  @DisplayName(
    "getIncidents deserializes content[].creatorServiceId (guards incidents-without-service fix)"
  )
  void getIncidents_deserializesCreatorServiceId() {
    WIREMOCK.stubFor(
      get(urlPathEqualTo("/api/v1/incidentService/incidents")).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "content": [
                {
                  "id": "11111111-1111-1111-1111-111111111111",
                  "title": "inc", "description": "d",
                  "type": {"id":"22222222-2222-2222-2222-222222222222","name":"BUG","displayName":"Bug","description":"x","active":null,"slaHours":24},
                  "criticality": "LOW", "status": "IN_PROGRESS",
                  "createdBy": null, "assignedTo": null,
                  "transferredToService": {"id":"44444444-4444-4444-4444-444444444444","name":"IT","isActive":null},
                  "agency": {"id":"55555555-5555-5555-5555-555555555555","name":"Alpha","isActive":null},
                  "createdAt": "2026-06-01T10:00:00", "resolvedAt": null,
                  "creatorServiceId": "33333333-3333-3333-3333-333333333333",
                  "participantUserIds": ["66666666-6666-6666-6666-666666666666"]
                }
              ],
              "totalElements": 1, "totalPages": 1, "size": 1000, "number": 0
            }
            """
          )
      )
    );

    IncidentsPageClientResponse page = incidentServiceClient.getIncidents(
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
      null,
      null,
      null,
      null,
      0,
      1000
    );

    assertThat(page.getContent()).hasSize(1);
    IncidentClientResponse incident = page.getContent().get(0);
    assertThat(incident.getCreatorServiceId()).isNotNull();
    assertThat(incident.getType().getActive()).isNull();
    assertThat(incident.getTransferredToService()).isNotNull();
    assertThat(incident.getTransferredToService().getActive()).isNull();
    assertThat(incident.getAgency().getActive()).isNull();
    assertThat(incident.getReopenCount()).isNull();
    assertThat(incident.getTransferCount()).isNull();
    assertThat(incident.getParticipantUserIds()).hasSize(1);
  }

  @Test
  @DisplayName(
    "getDashboardMetricsRaw exposes the full metric set (guards report-hydration regression)"
  )
  void getDashboardMetricsRaw_exposesFullMetricSet() {
    WIREMOCK.stubFor(
      get(
        urlPathEqualTo("/api/v1/incidentService/dashboard/metrics")
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "totalIncidents": 10, "activeIncidents": 6, "closedIncidents": 4,
              "rejectedIncidents": 1, "avgClosureHours": 12.5,
              "slaComplianceRate": 95.5,
              "distributionByType": {"BUG": 3, "REQUEST": 7},
              "monthlyClosures": []
            }
            """
          )
      )
    );

    Map<String, Object> raw = incidentServiceClient.getDashboardMetricsRaw(
      "all",
      null,
      null,
      null,
      null,
      null,
      null
    );

    assertThat(raw).containsKeys(
      "closedIncidents",
      "slaComplianceRate",
      "distributionByType"
    );
    assertThat(raw.get("closedIncidents")).isEqualTo(4);
  }

  @Test
  @DisplayName(
    "getAuditLogs deserializes Mongo string ids (guards audit export empty file regression)"
  )
  void getAuditLogs_deserializesMongoStringIds() {
    WIREMOCK.stubFor(
      get(urlPathEqualTo("/api/v1/auditService/audit-logs")).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "content": [
                {
                  "id": "6671fbc5a5d9c4258a001234",
                  "timestamp": "2026-06-24T10:15:00",
                  "username": "root",
                  "userId": "11111111-1111-1111-1111-111111111111",
                  "action": "USER_CREATE",
                  "status": "SUCCESS",
                  "resourceType": "USER",
                  "resourceId": "seed.agent.01",
                  "ipAddress": "127.0.0.1"
                }
              ],
              "totalElements": 1, "totalPages": 1, "size": 500, "number": 0
            }
            """
          )
      )
    );

    AuditLogsPageClientResponse page = auditClient.getAuditLogs(
      0,
      500,
      "timestamp,desc",
      null,
      null,
      null,
      null
    );

    assertThat(page.getContent()).hasSize(1);
    assertThat(page.getContent().get(0).getId()).isEqualTo(
      "6671fbc5a5d9c4258a001234"
    );
  }
}
