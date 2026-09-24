package com.fintrack.notification.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.notification.client.incident.dto.IncidentClientResponse;
import com.fintrack.notification.client.incident.fallback.IncidentServiceClientFallback;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IncidentServiceClientFallbackTest {

  private IncidentServiceClientFallback fallback;

  @BeforeEach
  void setUp() {
    fallback = new IncidentServiceClientFallback();
  }

  @Test
  @DisplayName("getIncidentById - Returns fallback with id and 'unknown' title")
  void getIncidentById_ReturnsFallback() {
    UUID id = UUID.randomUUID();

    IncidentClientResponse result = fallback.getIncidentById(id);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getTitle()).isEqualTo("unknown");
    assertThat(result.getStatus()).isEqualTo("UNKNOWN");
  }

  @Test
  @DisplayName("getIncidentById - Fallback preserves the requested ID")
  void getIncidentById_PreservesId() {
    UUID id = UUID.randomUUID();

    IncidentClientResponse result = fallback.getIncidentById(id);

    assertThat(result.getId()).isEqualTo(id);
  }

  @Test
  @DisplayName(
    "getIncidentById - Different IDs produce different fallback bodies"
  )
  void getIncidentById_DifferentIds_ProduceDifferentBodies() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();

    IncidentClientResponse r1 = fallback.getIncidentById(id1);
    IncidentClientResponse r2 = fallback.getIncidentById(id2);

    assertThat(r1.getId()).isNotEqualTo(r2.getId());
    assertThat(r1.getTitle()).isEqualTo(r2.getTitle());
    assertThat(r1.getStatus()).isEqualTo(r2.getStatus());
  }
}
