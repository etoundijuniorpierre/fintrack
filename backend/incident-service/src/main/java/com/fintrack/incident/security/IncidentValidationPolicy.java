package com.fintrack.incident.security;

import com.fintrack.incident.client.reporting.ReportingSystemConfigClientService;
import com.fintrack.incident.client.user.UserClientService;
import com.fintrack.incident.model.entity.Incident;
import com.fintrack.incident.model.entity.IncidentTypeConfig;
import com.fintrack.incident.model.constant.IncidentValidatorRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Regle commune au controle d'acces, au valideur affiche et aux notifications.
@Component
@RequiredArgsConstructor
public class IncidentValidationPolicy {

  private final ReportingSystemConfigClientService systemConfig;
  private final UserClientService userClientService;

  private boolean isSelfValidationAllowed() {
    return systemConfig.getThresholdLong(
      "serviceManagerSelfValidationEnabled", 0
    ) == 1;
  }

  public boolean isCreatedByTargetServiceHead(
    Incident incident, IncidentTypeConfig type
  ) {
    return type != null &&
      type.getDefaultTargetServiceId() != null &&
      incident.getCreatedBy() != null &&
      incident.getCreatedBy().equals(
        userClientService.resolveServiceHead(type.getDefaultTargetServiceId())
      );
  }

  public IncidentValidatorRole resolveOwnServiceValidator(
    Incident incident, IncidentTypeConfig type
  ) {
    if (!isCreatedByTargetServiceHead(incident, type)) {
      return null;
    }
    return isSelfValidationAllowed()
      ? IncidentValidatorRole.SERVICE_MANAGER
      : IncidentValidatorRole.ADMIN;
  }
}
