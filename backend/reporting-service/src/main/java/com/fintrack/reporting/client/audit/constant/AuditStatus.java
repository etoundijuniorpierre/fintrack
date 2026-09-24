// Client inter-services : communique avec les services externes lies a audit status.

package com.fintrack.reporting.client.audit.constant;

// Enumeration du statut retourne par le client audit.

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
