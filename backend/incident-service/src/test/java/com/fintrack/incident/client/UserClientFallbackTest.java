package com.fintrack.incident.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.incident.client.user.dto.AgencyClientResponse;
import com.fintrack.incident.client.user.dto.ServiceClientResponse;
import com.fintrack.incident.client.user.dto.UserClientResponse;
import com.fintrack.incident.client.user.fallback.UserClientFallback;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserClientFallbackTest {

  private UserClientFallback fallback;

  @BeforeEach
  void setUp() {
    fallback = new UserClientFallback();
  }

  @Test
  @DisplayName("getHealth - Returns unknown status")
  void getHealth_ReturnsUnknownStatus() {
    Map<String, Object> result = fallback.getHealth();

    assertThat(result).containsEntry("status", "UNKNOWN");
  }

  @Test
  @DisplayName("getUserById - Returns fallback with id and unknown username")
  void getUserById_ReturnsFallback() {
    UUID id = UUID.randomUUID();

    UserClientResponse result = fallback.getUserById(id);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getUsername()).isEqualTo("unknown");
    assertThat(result.getRoles()).isEmpty();
    assertThat(result.getPermissions()).isEmpty();
  }

  @Test
  @DisplayName("getUserById - Fallback preserves the requested ID")
  void getUserById_PreservesId() {
    UUID id = UUID.randomUUID();

    UserClientResponse result = fallback.getUserById(id);

    assertThat(result.getId()).isEqualTo(id);
  }

  @Test
  @DisplayName("getAgencyById - Returns fallback with id and unknown name")
  void getAgencyById_ReturnsFallback() {
    UUID id = UUID.randomUUID();

    AgencyClientResponse result = fallback.getAgencyById(id);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getName()).isEqualTo("unknown");
  }

  @Test
  @DisplayName("getAgencyById - Fallback preserves the requested ID")
  void getAgencyById_PreservesId() {
    UUID id = UUID.randomUUID();

    AgencyClientResponse result = fallback.getAgencyById(id);

    assertThat(result.getId()).isEqualTo(id);
  }

  @Test
  @DisplayName("getServiceById - Returns fallback with id and unknown name")
  void getServiceById_ReturnsFallback() {
    UUID id = UUID.randomUUID();

    ServiceClientResponse result = fallback.getServiceById(id);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getName()).isEqualTo("unknown");
  }

  @Test
  @DisplayName("getServiceById - Fallback preserves the requested ID")
  void getServiceById_PreservesId() {
    UUID id = UUID.randomUUID();

    ServiceClientResponse result = fallback.getServiceById(id);

    assertThat(result.getId()).isEqualTo(id);
  }
}
