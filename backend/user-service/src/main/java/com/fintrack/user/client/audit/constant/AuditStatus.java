// Client inter-services : communique avec les services externes lies a audit status.

package com.fintrack.user.client.audit.constant;

/**
 * Statuts d'audit utilisés par le user-service.
 * Les valeurs correspondent aux enums AuditStatus de l'audit-service.
 */
// Modelise la responsabilite applicative liee a journal d'audit.
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
