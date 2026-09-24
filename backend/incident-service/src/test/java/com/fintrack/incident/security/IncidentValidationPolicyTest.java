package com.fintrack.incident.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClient;
import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.model.constant.IncidentValidatorRole;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentValidationPolicyTest {
  @Mock private ReportingSystemConfigClient reporting;
  @Mock private UserClientService users;
  private IncidentValidationPolicy policy;
  private Incident incident;
  private IncidentTypeConfig type;
  private UUID serviceId;

  @BeforeEach
  void setUp() {
    policy = new IncidentValidationPolicy(new ReportingSystemConfigClientService(reporting), users);
    serviceId = UUID.randomUUID();
    incident = new Incident();
    incident.setCreatedBy(UUID.randomUUID());
    incident.setCreatorServiceId(serviceId);
    type = new IncidentTypeConfig();
    type.setDefaultTargetServiceId(serviceId);
  }

  @Test
  void defaultsToAdminOnlyWhenCreatorHeadsTheTypesHandlingService() {
    when(users.resolveServiceHead(serviceId)).thenReturn(incident.getCreatedBy());
    when(reporting.getThresholds()).thenReturn(Map.of());
    assertEquals(IncidentValidatorRole.ADMIN, policy.resolveOwnServiceValidator(incident, type));
  }

  @Test
  void explicitSettingAllowsTheCreatorServiceHead() {
    when(users.resolveServiceHead(serviceId)).thenReturn(incident.getCreatedBy());
    when(reporting.getThresholds()).thenReturn(Map.of("serviceManagerSelfValidationEnabled", 1));
    assertEquals(IncidentValidatorRole.SERVICE_MANAGER, policy.resolveOwnServiceValidator(incident, type));
  }

  @Test
  void unavailableConfigurationKeepsAdminValidation() {
    when(users.resolveServiceHead(serviceId)).thenReturn(incident.getCreatedBy());
    when(reporting.getThresholds()).thenThrow(new IllegalStateException("unavailable"));
    assertEquals(IncidentValidatorRole.ADMIN, policy.resolveOwnServiceValidator(incident, type));
  }

  @Test
  void declaringForAnotherServicesTypeDoesNotActivateTheRule() {
    UUID otherService = UUID.randomUUID();
    type.setDefaultTargetServiceId(otherService);
    incident.setTransferredToService(serviceId);
    when(users.resolveServiceHead(otherService)).thenReturn(UUID.randomUUID());
    assertNull(policy.resolveOwnServiceValidator(incident, type));
    verifyNoInteractions(reporting);
  }

  @Test
  void serviceMembershipAloneDoesNotActivateTheRule() {
    when(users.resolveServiceHead(serviceId)).thenReturn(UUID.randomUUID());
    assertNull(policy.resolveOwnServiceValidator(incident, type));
    verifyNoInteractions(reporting);
  }

  @Test
  void manuallySelectedServiceWithoutTypeTargetDoesNotActivateTheRule() {
    type.setDefaultTargetServiceId(null);
    incident.setTransferredToService(serviceId);
    assertNull(policy.resolveOwnServiceValidator(incident, type));
    assertNull(policy.resolveOwnServiceValidator(incident, null));
    verifyNoInteractions(reporting, users);
  }

  @Test
  void aSecondaryManagedServiceIsAlsoCovered() {
    incident.setCreatorServiceId(UUID.randomUUID());
    when(users.resolveServiceHead(serviceId)).thenReturn(incident.getCreatedBy());
    when(reporting.getThresholds()).thenReturn(Map.of());
    assertEquals(IncidentValidatorRole.ADMIN, policy.resolveOwnServiceValidator(incident, type));
  }
}
