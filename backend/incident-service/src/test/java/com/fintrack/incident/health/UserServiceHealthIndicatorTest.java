package com.fintrack.incident.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintrack.incident.client.user.UserClient;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@ExtendWith(MockitoExtension.class)
class UserServiceHealthIndicatorTest {

  @Mock
  private UserClient userClient;

  @Test
  @DisplayName("Health check uses user-service actuator health endpoint")
  void health_UsesActuatorHealthEndpoint_ReturnsUp() {
    UserServiceHealthIndicator indicator = new UserServiceHealthIndicator(
      userClient
    );
    when(userClient.getHealth()).thenReturn(Map.of("status", "UP"));

    Health health = indicator.checkNow();

    assertEquals(Status.UP, health.getStatus());
    assertEquals("/actuator/health", health.getDetails().get("endpoint"));
    verify(userClient).getHealth();
    verify(userClient, never()).getUserById(any(UUID.class));
  }

  @Test
  @DisplayName("Health check reports down when user-service actuator is not up")
  void health_UserServiceNotUp_ReturnsDown() {
    UserServiceHealthIndicator indicator = new UserServiceHealthIndicator(
      userClient
    );
    when(userClient.getHealth()).thenReturn(Map.of("status", "UNKNOWN"));

    Health health = indicator.checkNow();

    assertEquals(Status.DOWN, health.getStatus());
    assertEquals("UNKNOWN", health.getDetails().get("userServiceStatus"));
  }
}
