package com.fintrack.reporting.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.reporting.client.user.UserServiceClient;
import com.fintrack.reporting.client.user.dto.UserClientResponse;
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
class UserServiceClientWireMockTest {

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
  private UserServiceClient userServiceClient;

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
              "isActive": true
            }
            """.formatted(userId)
          )
      )
    );

    UserClientResponse result = userServiceClient.getUserById(userId);

    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getUsername()).isEqualTo("jdoe");
    assertThat(result.getFirstName()).isEqualTo("John");
    assertThat(result.getLastName()).isEqualTo("Doe");
    assertThat(result.isActive()).isTrue();
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
              "unknownField": "should be ignored",
              "anotherUnknown": 42
            }
            """.formatted(userId)
          )
      )
    );

    UserClientResponse result = userServiceClient.getUserById(userId);

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

    UserClientResponse result = userServiceClient.getUserById(userId);

    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getUsername()).isEqualTo("unknown");
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

    UserClientResponse result = userServiceClient.getUserById(userId);

    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getUsername()).isEqualTo("unknown");
  }
}
