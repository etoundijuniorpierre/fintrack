// Client inter-services : communique avec les services externes lies a audit action.

package com.fintrack.user.client.audit.constant;

/**
 * Actions d'audit générées par le user-service.
 * Les valeurs correspondent aux enums AuditAction de l'audit-service.
 */
// Modelise la responsabilite applicative liee a journal d'audit.
public enum AuditAction {
  LOGIN_SUCCESS("LOGIN_SUCCESS"),
  LOGIN_FAILURE("LOGIN_FAILURE"),
  LOGOUT("LOGOUT"),
  USER_CREATE("USER_CREATE"),
  USER_UPDATE("USER_UPDATE"),
  USER_DELETE("USER_DELETE"),
  ROLE_CREATE("ROLE_CREATE"),
  ROLE_UPDATE("ROLE_UPDATE"),
  ROLE_DELETE("ROLE_DELETE"),
  DEPARTMENT_DELETE("DEPARTMENT_DELETE"),
  DEPARTMENT_ASSIGN_HEAD("DEPARTMENT_ASSIGN_HEAD"),
  DEPARTMENT_UPDATE("DEPARTMENT_UPDATE"),
  DEPARTMENT_CREATE("DEPARTMENT_CREATE"),
  AGENCY_CREATE("AGENCY_CREATE"),
  AGENCY_UPDATE("AGENCY_UPDATE"),
  AGENCY_ASSIGN_HEAD("AGENCY_ASSIGN_HEAD"),
  AGENCY_DELETE("AGENCY_DELETE"),
  USER_PASSWORD_CHANGE("USER_PASSWORD_CHANGE"),
  USER_PASSWORD_RESET("USER_PASSWORD_RESET");

  private final String name;

  AuditAction(String name) {
    this.name = name;
  }

  // Fournit name a la couche appelante.

  public String getName() {
    return name;
  }
}
