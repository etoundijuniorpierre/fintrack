package com.fintrack.incident.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.incident.client.user.UserClient;
import com.fintrack.incident.client.user.dto.AgencyClientResponse;
import com.fintrack.incident.client.user.dto.ServiceClientResponse;
import com.fintrack.incident.client.user.dto.UserClientResponse;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserClientWireMockTest {

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
    registry.add("user.service.url", () -> url);
    // L'override par client Feign prime sur l'URL du @FeignClient — il doit
    // aussi pointer vers WireMock.
    registry.add(
      "spring.cloud.openfeign.client.config.user-service.url",
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
  private UserClient userClient;

  @Test
  @DisplayName("getUserById - Returns user data from user-service")
  void getUserById_NominalCase_ReturnsUserData() {
    UUID userId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/userService/internal/users/" + userId)
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "id": "%s",
              "username": "jdoe",
              "firstName": "John",
              "lastName": "Doe",
              "email": "jdoe@example.com",
              "isActive": true,
              "roles": ["AGENT"],
              "permissions": ["INCIDENT_CREATE", "INCIDENT_VIEW_OWN"],
              "agencyId": null,
              "serviceId": null
            }
            """.formatted(userId)
          )
      )
    );

    UserClientResponse result = userClient.getUserById(userId);

    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getUsername()).isEqualTo("jdoe");
    assertThat(result.getFirstName()).isEqualTo("John");
    assertThat(result.getLastName()).isEqualTo("Doe");
    assertThat(result.isActive()).isTrue();
    assertThat(result.getRoles()).containsExactlyInAnyOrder("AGENT");
    assertThat(result.getPermissions()).containsExactlyInAnyOrder(
      "INCIDENT_CREATE",
      "INCIDENT_VIEW_OWN"
    );
  }

  @Test
  @DisplayName(
    "getUserById - Ignores unknown JSON fields (tolerant deserialization)"
  )
  void getUserById_UnknownFields_DeserializesCorrectly() {
    UUID userId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/userService/internal/users/" + userId)
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "id": "%s",
              "username": "jdoe",
              "firstName": "John",
              "lastName": "Doe",
              "email": "jdoe@example.com",
              "isActive": true,
              "roles": [],
              "permissions": [],
              "agencyId": null,
              "serviceId": null,
              "unknownField": "should be ignored",
              "anotherUnknown": 42
            }
            """.formatted(userId)
          )
      )
    );

    UserClientResponse result = userClient.getUserById(userId);

    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getUsername()).isEqualTo("jdoe");
  }

  @Test
  @Order(100)
  @DisplayName("getUserById - Returns fallback when user-service returns 404")
  void getUserById_NotFound_ReturnsFallback() {
    UUID userId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/userService/internal/users/" + userId)
      ).willReturn(aResponse().withStatus(404))
    );

    UserClientResponse result = userClient.getUserById(userId);

    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getUsername()).isEqualTo("unknown");
    assertThat(result.getRoles()).isEmpty();
    assertThat(result.getPermissions()).isEmpty();
  }

  @Test
  @Order(101)
  @DisplayName("getUserById - Returns fallback when user-service returns 500")
  void getUserById_ServerError_ReturnsFallback() {
    UUID userId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/userService/internal/users/" + userId)
      ).willReturn(aResponse().withStatus(500))
    );

    UserClientResponse result = userClient.getUserById(userId);

    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getUsername()).isEqualTo("unknown");
  }

  @Test
  @DisplayName("getAgencyById - Returns agency data from user-service")
  void getAgencyById_NominalCase_ReturnsAgencyData() {
    UUID agencyId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/userService/internal/agencies/" + agencyId)
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "id": "%s",
              "name": "Agence Paris",
              "code": "AG-PAR"
            }
            """.formatted(agencyId)
          )
      )
    );

    AgencyClientResponse result = userClient.getAgencyById(agencyId);

    assertThat(result.getId()).isEqualTo(agencyId);
    assertThat(result.getName()).isEqualTo("Agence Paris");
    assertThat(result.getCode()).isEqualTo("AG-PAR");
  }

  @Test
  @Order(102)
  @DisplayName("getAgencyById - Returns fallback when user-service returns 404")
  void getAgencyById_NotFound_ReturnsFallback() {
    UUID agencyId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/userService/internal/agencies/" + agencyId)
      ).willReturn(aResponse().withStatus(404))
    );

    AgencyClientResponse result = userClient.getAgencyById(agencyId);

    assertThat(result.getId()).isEqualTo(agencyId);
    assertThat(result.getName()).isEqualTo("unknown");
  }

  @Test
  @DisplayName("getServiceById - Returns service data from user-service")
  void getServiceById_NominalCase_ReturnsServiceData() {
    UUID serviceId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/userService/internal/departments/" + serviceId)
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "id": "%s",
              "name": "Service Informatique",
              "description": "Gestion des incidents IT"
            }
            """.formatted(serviceId)
          )
      )
    );

    ServiceClientResponse result = userClient.getServiceById(serviceId);

    assertThat(result.getId()).isEqualTo(serviceId);
    assertThat(result.getName()).isEqualTo("Service Informatique");
    assertThat(result.getDescription()).isEqualTo("Gestion des incidents IT");
  }

  @Test
  @Order(103)
  @DisplayName(
    "getServiceById - Returns fallback when user-service returns 404"
  )
  void getServiceById_NotFound_ReturnsFallback() {
    UUID serviceId = UUID.randomUUID();

    WIREMOCK.stubFor(
      get(
        urlEqualTo("/api/v1/userService/internal/departments/" + serviceId)
      ).willReturn(aResponse().withStatus(404))
    );

    ServiceClientResponse result = userClient.getServiceById(serviceId);

    assertThat(result.getId()).isEqualTo(serviceId);
    assertThat(result.getName()).isEqualTo("unknown");
  }
}
