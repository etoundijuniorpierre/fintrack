package com.fintrack.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fintrack.notification.client.incident.IncidentServiceClient;
import com.fintrack.notification.client.incident.IncidentServiceClientService;
import com.fintrack.notification.client.incident.dto.IncidentClientResponse;
import com.fintrack.notification.model.dto.response.IncidentSummaryResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IncidentServiceClientServiceTest {

  private IncidentServiceClient incidentServiceClient;
  private IncidentServiceClientService service;

  @BeforeEach
  void setUp() {
    incidentServiceClient = mock(IncidentServiceClient.class);
    service = new IncidentServiceClientService(incidentServiceClient);
  }

  @Test
  @DisplayName("resolveIncident - Returns null for null id")
  void resolveIncident_NullId_ReturnsNull() {
    assertThat(service.resolveIncident(null)).isNull();
    verifyNoInteractions(incidentServiceClient);
  }

  @Test
  @DisplayName("resolveIncident - Maps incident-service response")
  void resolveIncident_MapsResponse() {
    UUID id = UUID.randomUUID();
    IncidentClientResponse response = new IncidentClientResponse();
    response.setId(id);
    response.setTitle("Incident title");
    response.setStatus("IN_PROGRESS");

    when(incidentServiceClient.getIncidentById(id)).thenReturn(response);

    IncidentSummaryResponse result = service.resolveIncident(id);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getTitle()).isEqualTo("Incident title");
    assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
  }

  @Test
  @DisplayName(
    "resolveIncident - Falls back when incident-service lookup fails"
  )
  void resolveIncident_LookupFails_ReturnsFallbackSummary() {
    UUID id = UUID.randomUUID();
    when(incidentServiceClient.getIncidentById(id)).thenThrow(
      new RuntimeException("Unauthorized")
    );

    IncidentSummaryResponse result = service.resolveIncident(id);

    assertThat(result.getId()).isEqualTo(id);
    assertThat(result.getTitle()).isEqualTo("unknown");
    assertThat(result.getStatus()).isEqualTo("UNKNOWN");
  }
}
