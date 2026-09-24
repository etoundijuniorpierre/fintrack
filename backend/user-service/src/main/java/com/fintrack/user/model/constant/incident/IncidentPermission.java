// Constantes metier : centralise les valeurs stables liees a incident permission.

package com.fintrack.user.model.constant.incident;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Definition des permissions liees a la gestion des incidents.

@Getter
@RequiredArgsConstructor
public enum IncidentPermission {
  INCIDENT_CREATE("INCIDENT_CREATE"),
  INCIDENT_UPDATE("INCIDENT_UPDATE"),
  INCIDENT_REOPEN("INCIDENT_REOPEN"),
  INCIDENT_VIEW_OWN("INCIDENT_VIEW_OWN"),
  INCIDENT_VIEW_AGENCY("INCIDENT_VIEW_AGENCY"),
  INCIDENT_VIEW_SERVICE("INCIDENT_VIEW_SERVICE"),
  INCIDENT_VIEW_ALL("INCIDENT_VIEW_ALL"),
  INCIDENT_VALIDATE("INCIDENT_VALIDATE"),
  INCIDENT_TRANSFER("INCIDENT_TRANSFER"),
  INCIDENT_AUTO_TRANSFER("INCIDENT_AUTO_TRANSFER"),
  INCIDENT_TRANSFER_WITH_REASON("INCIDENT_TRANSFER_WITH_REASON"),
  INCIDENT_TREAT("INCIDENT_TREAT"),
  INCIDENT_ASSIGN("INCIDENT_ASSIGN"),
  INCIDENT_RESOLVE("INCIDENT_RESOLVE"),
  INCIDENT_CLOSE("INCIDENT_CLOSE"),
  INCIDENT_CANCEL("INCIDENT_CANCEL"),
  INCIDENT_REJECT("INCIDENT_REJECT"),
  INCIDENT_DELETE("INCIDENT_DELETE"),
  VALIDATION_DIRECTION("VALIDATION_DIRECTION");

  private final String name;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(IncidentPermission::getName)
      .toArray(String[]::new);
  }
}
