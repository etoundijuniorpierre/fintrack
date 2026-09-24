// Client inter-services : communique avec les services externes lies a audit status.

package com.fintrack.incident.client.audit.constant;

// Enumeration du resultat (succes ou echec) d'une action auditee
public enum AuditStatus {
  SUCCESS("SUCCESS"),
  FAILURE("FAILURE");

  private final String name;

  AuditStatus(String name) {
    this.name = name;
  }

  // Fournit name a la couche appelante.

  public String getName() {
    return name;
  }
}
