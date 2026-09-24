// Tests du mapping des configurations de type d'incident et de leurs valeurs par defaut.

package com.fintrack.incident.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.fintrack.incident.model.dto.request.IncidentTypeConfigRequest;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

// Verifie que les indicateurs metier ne sont ni perdus ni reactives implicitement.
class IncidentTypeConfigMapperTest {

  private final IncidentTypeConfigMapper mapper = Mappers.getMapper(
    IncidentTypeConfigMapper.class
  );

  @Test
  @DisplayName("toEntity - Applies channel and active defaults on creation")
  void toEntity_MissingFlags_AppliesDefaults() {
    IncidentTypeConfigRequest request = IncidentTypeConfigRequest.builder()
      .name("cash-incident")
      .displayName("Cash incident")
      .build();

    IncidentTypeConfig result = mapper.toEntity(request);

    assertThat(result.isActive()).isTrue();
    assertThat(result.isEmailNotificationsEnabled()).isFalse();
    assertThat(result.isInAppNotificationsEnabled()).isTrue();
    assertThat(result.getSlaHours()).isNull();
  }

  @Test
  @DisplayName("toEntity - Preserves explicitly disabled active status")
  void toEntity_DisabledRequest_RemainsDisabled() {
    IncidentTypeConfigRequest request = IncidentTypeConfigRequest.builder()
      .name("cash-incident")
      .displayName("Cash incident")
      .active(false)
      .emailNotificationsEnabled(true)
      .build();

    IncidentTypeConfig result = mapper.toEntity(request);

    assertThat(result.isActive()).isFalse();
    assertThat(result.isEmailNotificationsEnabled()).isTrue();
    assertThat(result.isInAppNotificationsEnabled()).isTrue();
  }

  @Test
  @DisplayName("updateEntityFromRequest - Preserves status when omitted")
  void updateEntityFromRequest_MissingActive_PreservesExistingStatus() {
    IncidentTypeConfig entity = new IncidentTypeConfig();
    entity.setActive(false);
    entity.setInAppNotificationsEnabled(false);
    IncidentTypeConfigRequest request = IncidentTypeConfigRequest.builder()
      .name("cash-incident")
      .displayName("Cash incident")
      .emailNotificationsEnabled(true)
      .active(null)
      .build();

    mapper.updateEntityFromRequest(request, entity);

    assertThat(entity.isActive()).isFalse();
    assertThat(entity.isEmailNotificationsEnabled()).isTrue();
    assertThat(entity.isInAppNotificationsEnabled()).isTrue();
  }

  @Test
  @DisplayName("updateEntityFromRequest - Resets target service and user to null when requested")
  void updateEntityFromRequest_NullTargetServiceAndUser_ResetsToNull() {
    UUID oldServiceId = UUID.randomUUID();
    UUID oldUserId = UUID.randomUUID();
    IncidentTypeConfig entity = new IncidentTypeConfig();
    entity.setDefaultTargetServiceId(oldServiceId);
    entity.setDefaultTargetUserId(oldUserId);

    IncidentTypeConfigRequest request = IncidentTypeConfigRequest.builder()
      .name("cash-incident")
      .displayName("Cash incident")
      .defaultTargetServiceId(null)
      .defaultTargetUserId(null)
      .build();

    mapper.updateEntityFromRequest(request, entity);

    assertThat(entity.getDefaultTargetServiceId()).isNull();
    assertThat(entity.getDefaultTargetUserId()).isNull();
  }
}

