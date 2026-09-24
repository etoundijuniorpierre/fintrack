// Client inter-services : communique avec les services externes lies a audit action.

package com.fintrack.reporting.client.audit.constant;

// Actions auditees emises par le reporting-service.
public enum AuditAction {
  REPORT_ACCESS("REPORT_ACCESS"),
  REPORT_EXPORT("REPORT_EXPORT"),
  REPORT_DELETE("REPORT_DELETE"),
  REPORT_RERUN("REPORT_RERUN"),
  AUDIT_EXPORT("AUDIT_EXPORT"),
  SETTINGS_CHANGE("SETTINGS_CHANGE"),
  ESCALATION_RULE_TOGGLE("ESCALATION_RULE_TOGGLE"),
  ESCALATION_RULE_TRIGGER("ESCALATION_RULE_TRIGGER"),
  CACHE_INVALIDATION("CACHE_INVALIDATION"),
  JOB_TRIGGER("JOB_TRIGGER"),
  SYSTEM_BACKUP("SYSTEM_BACKUP");

  private final String name;

  AuditAction(String name) {
    this.name = name;
  }

  // Fournit name a la couche appelante.

  public String getName() {
    return name;
  }
}
