package com.fintrack.reporting.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.reporting.client.user.dto.UserClientResponse;
import com.fintrack.reporting.client.user.fallback.UserServiceClientFallback;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserServiceClientFallbackTest {

  private UserServiceClientFallback fallback;

  @BeforeEach
  void setUp() {
    fallback = new UserServiceClientFallback();
  }

  @Test
  @DisplayName("getUserById - Returns fallback with id and 'unknown' username")
  void getUserById_ReturnsFallback() {
    UUID id = UUID.randomUUID();

    UserClientResponse result = fallback.getUserById(id);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getUsername()).isEqualTo("unknown");
  }

  @Test
  @DisplayName("getUserById - Fallback preserves the requested ID")
  void getUserById_PreservesId() {
    UUID id = UUID.randomUUID();

    UserClientResponse result = fallback.getUserById(id);

    assertThat(result.getId()).isEqualTo(id);
  }

  @Test
  @DisplayName("getUserById - Different IDs produce different fallback bodies")
  void getUserById_DifferentIds_ProduceDifferentBodies() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    UserClientResponse r1 = fallback.getUserById(id1);
    UserClientResponse r2 = fallback.getUserById(id2);

    assertThat(r1.getId()).isNotEqualTo(r2.getId());
    assertThat(r1.getUsername()).isEqualTo(r2.getUsername());
  }
}
