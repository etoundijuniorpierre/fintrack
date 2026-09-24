// Client inter-services : communique avec les services externes lies a audit action.

package com.fintrack.incident.client.audit.constant;

// Enumeration des types d'actions auditables sur les incidents
public enum AuditAction {
  INCIDENT_CREATE("INCIDENT_CREATE"),
  INCIDENT_UPDATE("INCIDENT_UPDATE"),
  INCIDENT_VALIDATE("INCIDENT_VALIDATE"),
  INCIDENT_SUBMIT_SOLUTION("INCIDENT_SUBMIT_SOLUTION"),
  INCIDENT_DIRECTION_VALIDATE("INCIDENT_DIRECTION_VALIDATE"),
  INCIDENT_DIRECTION_REJECT("INCIDENT_DIRECTION_REJECT"),
  INCIDENT_TRANSFER("INCIDENT_TRANSFER"),
  INCIDENT_STATUS_CHANGE("INCIDENT_STATUS_CHANGE"),
  INCIDENT_RESOLVE("INCIDENT_RESOLVE"),
  INCIDENT_CLOSE("INCIDENT_CLOSE"),
  INCIDENT_DELETE("INCIDENT_DELETE"),
  INCIDENT_SLA_REMINDER("INCIDENT_SLA_REMINDER"),
  SETTINGS_CHANGE("SETTINGS_CHANGE");

  private final String name;

  AuditAction(String name) {
    this.name = name;
  }

  // Fournit name a la couche appelante.

  public String getName() {
    return name;
  }
}
