// Constantes metier : centralise les valeurs stables liees a audit action.

package com.fintrack.audit.model.constant;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Actions auditables dans le système FinTrack.
 * Chaque valeur porte un libellé i18n (nameKey) et une description i18n (descriptionKey).
 */
@Getter
@RequiredArgsConstructor
// Modelise la responsabilite applicative liee a journal d'audit.
public enum AuditAction implements LocalizableEnum {
  // Authentification
  LOGIN_SUCCESS(
    "LOGIN_SUCCESS",
    "enum.audit_action.LOGIN_SUCCESS.name",
    "enum.audit_action.LOGIN_SUCCESS.description"
  ),
  LOGIN_FAILURE(
    "LOGIN_FAILURE",
    "enum.audit_action.LOGIN_FAILURE.name",
    "enum.audit_action.LOGIN_FAILURE.description"
  ),
  LOGOUT(
    "LOGOUT",
    "enum.audit_action.LOGOUT.name",
    "enum.audit_action.LOGOUT.description"
  ),

  INCIDENT_CREATE(
    "INCIDENT_CREATE",
    "enum.audit_action.INCIDENT_CREATE.name",
    "enum.audit_action.INCIDENT_CREATE.description"
  ),
  INCIDENT_UPDATE(
    "INCIDENT_UPDATE",
    "enum.audit_action.INCIDENT_UPDATE.name",
    "enum.audit_action.INCIDENT_UPDATE.description"
  ),
  INCIDENT_VALIDATE(
    "INCIDENT_VALIDATE",
    "enum.audit_action.INCIDENT_VALIDATE.name",
    "enum.audit_action.INCIDENT_VALIDATE.description"
  ),
  INCIDENT_SUBMIT_SOLUTION(
    "INCIDENT_SUBMIT_SOLUTION",
    "enum.audit_action.INCIDENT_SUBMIT_SOLUTION.name",
    "enum.audit_action.INCIDENT_SUBMIT_SOLUTION.description"
  ),
  INCIDENT_DIRECTION_VALIDATE(
    "INCIDENT_DIRECTION_VALIDATE",
    "enum.audit_action.INCIDENT_DIRECTION_VALIDATE.name",
    "enum.audit_action.INCIDENT_DIRECTION_VALIDATE.description"
  ),
  INCIDENT_DIRECTION_REJECT(
    "INCIDENT_DIRECTION_REJECT",
    "enum.audit_action.INCIDENT_DIRECTION_REJECT.name",
    "enum.audit_action.INCIDENT_DIRECTION_REJECT.description"
  ),
  INCIDENT_TRANSFER(
    "INCIDENT_TRANSFER",
    "enum.audit_action.INCIDENT_TRANSFER.name",
    "enum.audit_action.INCIDENT_TRANSFER.description"
  ),
  INCIDENT_STATUS_CHANGE(
    "INCIDENT_STATUS_CHANGE",
    "enum.audit_action.INCIDENT_STATUS_CHANGE.name",
    "enum.audit_action.INCIDENT_STATUS_CHANGE.description"
  ),
  INCIDENT_RESOLVE(
    "INCIDENT_RESOLVE",
    "enum.audit_action.INCIDENT_RESOLVE.name",
    "enum.audit_action.INCIDENT_RESOLVE.description"
  ),
  INCIDENT_CLOSE(
    "INCIDENT_CLOSE",
    "enum.audit_action.INCIDENT_CLOSE.name",
    "enum.audit_action.INCIDENT_CLOSE.description"
  ),
  INCIDENT_DELETE(
    "INCIDENT_DELETE",
    "enum.audit_action.INCIDENT_DELETE.name",
    "enum.audit_action.INCIDENT_DELETE.description"
  ),
  INCIDENT_SLA_REMINDER(
    "INCIDENT_SLA_REMINDER",
    "enum.audit_action.INCIDENT_SLA_REMINDER.name",
    "enum.audit_action.INCIDENT_SLA_REMINDER.description"
  ),

  USER_CREATE(
    "USER_CREATE",
    "enum.audit_action.USER_CREATE.name",
    "enum.audit_action.USER_CREATE.description"
  ),
  USER_UPDATE(
    "USER_UPDATE",
    "enum.audit_action.USER_UPDATE.name",
    "enum.audit_action.USER_UPDATE.description"
  ),
  USER_DELETE(
    "USER_DELETE",
    "enum.audit_action.USER_DELETE.name",
    "enum.audit_action.USER_DELETE.description"
  ),
  USER_PASSWORD_CHANGE(
    "USER_PASSWORD_CHANGE",
    "enum.audit_action.USER_PASSWORD_CHANGE.name",
    "enum.audit_action.USER_PASSWORD_CHANGE.description"
  ),
  USER_PASSWORD_RESET(
    "USER_PASSWORD_RESET",
    "enum.audit_action.USER_PASSWORD_RESET.name",
    "enum.audit_action.USER_PASSWORD_RESET.description"
  ),

  ROLE_CREATE(
    "ROLE_CREATE",
    "enum.audit_action.ROLE_CREATE.name",
    "enum.audit_action.ROLE_CREATE.description"
  ),
  ROLE_UPDATE(
    "ROLE_UPDATE",
    "enum.audit_action.ROLE_UPDATE.name",
    "enum.audit_action.ROLE_UPDATE.description"
  ),
  ROLE_DELETE(
    "ROLE_DELETE",
    "enum.audit_action.ROLE_DELETE.name",
    "enum.audit_action.ROLE_DELETE.description"
  ),

  DEPARTMENT_CREATE(
    "DEPARTMENT_CREATE",
    "enum.audit_action.DEPARTMENT_CREATE.name",
    "enum.audit_action.DEPARTMENT_CREATE.description"
  ),
  DEPARTMENT_UPDATE(
    "DEPARTMENT_UPDATE",
    "enum.audit_action.DEPARTMENT_UPDATE.name",
    "enum.audit_action.DEPARTMENT_UPDATE.description"
  ),
  DEPARTMENT_DELETE(
    "DEPARTMENT_DELETE",
    "enum.audit_action.DEPARTMENT_DELETE.name",
    "enum.audit_action.DEPARTMENT_DELETE.description"
  ),
  DEPARTMENT_ASSIGN_HEAD(
    "DEPARTMENT_ASSIGN_HEAD",
    "enum.audit_action.DEPARTMENT_ASSIGN_HEAD.name",
    "enum.audit_action.DEPARTMENT_ASSIGN_HEAD.description"
  ),

  AGENCY_CREATE(
    "AGENCY_CREATE",
    "enum.audit_action.AGENCY_CREATE.name",
    "enum.audit_action.AGENCY_CREATE.description"
  ),
  AGENCY_UPDATE(
    "AGENCY_UPDATE",
    "enum.audit_action.AGENCY_UPDATE.name",
    "enum.audit_action.AGENCY_UPDATE.description"
  ),
  AGENCY_DELETE(
    "AGENCY_DELETE",
    "enum.audit_action.AGENCY_DELETE.name",
    "enum.audit_action.AGENCY_DELETE.description"
  ),
  AGENCY_ASSIGN_HEAD(
    "AGENCY_ASSIGN_HEAD",
    "enum.audit_action.AGENCY_ASSIGN_HEAD.name",
    "enum.audit_action.AGENCY_ASSIGN_HEAD.description"
  ),

  REPORT_ACCESS(
    "REPORT_ACCESS",
    "enum.audit_action.REPORT_ACCESS.name",
    "enum.audit_action.REPORT_ACCESS.description"
  ),
  REPORT_EXPORT(
    "REPORT_EXPORT",
    "enum.audit_action.REPORT_EXPORT.name",
    "enum.audit_action.REPORT_EXPORT.description"
  ),
  REPORT_DELETE(
    "REPORT_DELETE",
    "enum.audit_action.REPORT_DELETE.name",
    "enum.audit_action.REPORT_DELETE.description"
  ),
  REPORT_RERUN(
    "REPORT_RERUN",
    "enum.audit_action.REPORT_RERUN.name",
    "enum.audit_action.REPORT_RERUN.description"
  ),

  AUDIT_EXPORT(
    "AUDIT_EXPORT",
    "enum.audit_action.AUDIT_EXPORT.name",
    "enum.audit_action.AUDIT_EXPORT.description"
  ),
  AUDIT_PURGE(
    "AUDIT_PURGE",
    "enum.audit_action.AUDIT_PURGE.name",
    "enum.audit_action.AUDIT_PURGE.description"
  ),
  CACHE_INVALIDATION(
    "CACHE_INVALIDATION",
    "enum.audit_action.CACHE_INVALIDATION.name",
    "enum.audit_action.CACHE_INVALIDATION.description"
  ),
  JOB_TRIGGER(
    "JOB_TRIGGER",
    "enum.audit_action.JOB_TRIGGER.name",
    "enum.audit_action.JOB_TRIGGER.description"
  ),
  ESCALATION_RULE_TOGGLE(
    "ESCALATION_RULE_TOGGLE",
    "enum.audit_action.ESCALATION_RULE_TOGGLE.name",
    "enum.audit_action.ESCALATION_RULE_TOGGLE.description"
  ),
  ESCALATION_RULE_TRIGGER(
    "ESCALATION_RULE_TRIGGER",
    "enum.audit_action.ESCALATION_RULE_TRIGGER.name",
    "enum.audit_action.ESCALATION_RULE_TRIGGER.description"
  ),

  SETTINGS_CHANGE(
    "SETTINGS_CHANGE",
    "enum.audit_action.SETTINGS_CHANGE.name",
    "enum.audit_action.SETTINGS_CHANGE.description"
  );

  /** Identifiant technique stable (stocké en base). */
  private final String name;
  /** Clé i18n pour le libellé affiché. */
  private final String nameKey;
  /** Clé i18n pour la description affichée. */
  private final String descriptionKey;

  // Fournit all names a la couche appelante.

  public static String[] getAllNames() {
    return Arrays.stream(values())
      .map(AuditAction::getName)
      .toArray(String[]::new);
  }
}
