// Tests unitaires : verifie la projection des notifications vers leur contrat API.

package com.fintrack.notification.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fintrack.notification.model.dto.response.NotificationResponse;
import com.fintrack.notification.model.entity.Notification;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

// Verifie que les donnees techniques et traduites restent correctement separees.
class NotificationMapperTest {

  private final NotificationMapper mapper = Mappers.getMapper(
    NotificationMapper.class
  );

  // Conserve le code enum pour la synchronisation temps reel.
  @Test
  void toResponse_LocalizedStatus_UsesStatusCode() {
    Notification notification = new Notification();
    notification.setIncidentId(UUID.randomUUID());
    notification.setTemplateParams(
      Map.of(
        "incident_title",
        "Incident de caisse",
        "incident_status",
        "Bloque",
        "incident_status_code",
        "BLOCKED",
        "incident_reference",
        "FT-I-2026-0001"
      )
    );

    NotificationResponse response = mapper.toResponse(notification);

    assertThat(response.getIncidentId().getStatus()).isEqualTo("BLOCKED");
    assertThat(response.getIncidentId().getReference()).isEqualTo(
      "FT-I-2026-0001"
    );
  }
}
