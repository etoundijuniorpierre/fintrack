// Tests du client incident : distingue une indisponibilite technique d'une liste vide metier.

package com.fintrack.reporting.client.incident;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import tools.jackson.databind.ObjectMapper;

// Verifie les garanties de fiabilite appliquees aux donnees des rapports.
class IncidentServiceClientServiceTest {

  // Refuse de convertir le fallback Feign en rapport vide disponible.
  @Test
  @DisplayName("getIncidentsAsMaps rejects an unavailable incident service")
  void getIncidentsAsMaps_rejectsUnavailableIncidentService() {
    IncidentServiceClient client = mock(IncidentServiceClient.class);
    when(
      client.getIncidents(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        anyInt(),
        anyInt()
      )
    ).thenReturn(null);
    ResourceBundleMessageSource messageSource =
      new ResourceBundleMessageSource();
    messageSource.setBasename("i18n/messages");
    messageSource.setDefaultEncoding("UTF-8");
    IncidentServiceClientService service = new IncidentServiceClientService(
      client,
      new ObjectMapper(),
      messageSource
    );

    assertThatThrownBy(() ->
      service.getIncidentsAsMaps(
        "all",
        List.of(),
        List.of(),
        List.of(),
        null,
        null,
        null,
        null,
        null,
        "2026-06-01",
        "2026-06-30"
      )
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("page d'incidents");
  }
}
