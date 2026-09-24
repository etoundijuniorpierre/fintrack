package com.fintrack.notification.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.notification.TestcontainersConfiguration;
import com.fintrack.notification.client.incident.IncidentServiceClient;
import com.fintrack.notification.client.incident.dto.IncidentClientResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IncidentServiceClientWireMockTest {

  // Port dynamique : évite tout conflit avec un port fixe déjà occupé sur la machine.
  private static final WireMockServer WIREMOCK = new WireMockServer(
    WireMockConfiguration.options().dynamicPort()
  );

  static {
    WIREMOCK.start();
  }

  @DynamicPropertySource
  static void registerWireMockUrl(DynamicPropertyRegistry registry) {
    String url = "http://localhost:" + WIREMOCK.port();
    registry.add("incident.service.url", () -> url);
    registry.add(
      "spring.cloud.openfeign.client.config.incident-service.url",
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
  private IncidentServiceClient incidentServiceClient;

  @Test
  @DisplayName("getIncidentById - Returns incident data from incident-service")
  void getIncidentById_NominalCase_ReturnsIncidentData() {
    UUID incidentId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/incidentService/incidents/" + incidentId)
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "id": "%s",
              "title": "Server Down",
              "status": "OPEN"
            }
            """.formatted(incidentId)
          )
      )
    );

    IncidentClientResponse result = incidentServiceClient.getIncidentById(
      incidentId
    );

    assertThat(result.getId()).isEqualTo(incidentId);
    assertThat(result.getTitle()).isEqualTo("Server Down");
    assertThat(result.getStatus()).isEqualTo("OPEN");
  }

  @Test
  @DisplayName(
    "getIncidentById - Ignores unknown JSON fields (tolerant deserialization)"
  )
  void getIncidentById_UnknownFields_DeserializesCorrectly() {
    UUID incidentId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/incidentService/incidents/" + incidentId)
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "id": "%s",
              "title": "Server Down",
              "status": "OPEN",
              "unknownField": "should be ignored",
              "anotherUnknown": 42
            }
            """.formatted(incidentId)
          )
      )
    );

    IncidentClientResponse result = incidentServiceClient.getIncidentById(
      incidentId
    );

    assertThat(result.getId()).isEqualTo(incidentId);
    assertThat(result.getTitle()).isEqualTo("Server Down");
  }

  @Test
  @DisplayName(
    "getIncidentById - Returns fallback when incident-service returns 404"
  )
  @Order(100)
  void getIncidentById_NotFound_ReturnsFallback() {
    UUID incidentId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/incidentService/incidents/" + incidentId)
      ).willReturn(aResponse().withStatus(404))
    );

    IncidentClientResponse result = incidentServiceClient.getIncidentById(
      incidentId
    );

    assertThat(result.getId()).isEqualTo(incidentId);
    assertThat(result.getTitle()).isEqualTo("unknown");
    assertThat(result.getStatus()).isEqualTo("UNKNOWN");
  }

  @Test
  @DisplayName(
    "getIncidentById - Returns fallback when incident-service returns 500"
  )
  @Order(101)
  void getIncidentById_ServerError_ReturnsFallback() {
    UUID incidentId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/incidentService/incidents/" + incidentId)
      ).willReturn(aResponse().withStatus(500))
    );

    IncidentClientResponse result = incidentServiceClient.getIncidentById(
      incidentId
    );

    assertThat(result.getId()).isEqualTo(incidentId);
    assertThat(result.getTitle()).isEqualTo("unknown");
  }
}
