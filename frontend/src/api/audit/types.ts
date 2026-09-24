// Centralise la logique d'interface liee a audit action.
export type AuditAction =
  | "LOGIN_SUCCESS"
  | "LOGIN_FAILURE"
  | "LOGOUT"
  | "INCIDENT_CREATE"
  | "INCIDENT_UPDATE"
  | "INCIDENT_VALIDATE"
  | "INCIDENT_SUBMIT_SOLUTION"
  | "INCIDENT_DIRECTION_VALIDATE"
  | "INCIDENT_DIRECTION_REJECT"
  | "INCIDENT_TRANSFER"
  | "INCIDENT_STATUS_CHANGE"
  | "INCIDENT_RESOLVE"
  | "INCIDENT_CLOSE"
  | "INCIDENT_DELETE"
  | "INCIDENT_SLA_REMINDER"
  | "USER_CREATE"
  | "USER_UPDATE"
  | "USER_DELETE"
  | "USER_PASSWORD_CHANGE"
  | "USER_PASSWORD_RESET"
  | "ROLE_CREATE"
  | "ROLE_UPDATE"
  | "ROLE_DELETE"
  | "DEPARTMENT_CREATE"
  | "DEPARTMENT_UPDATE"
  | "DEPARTMENT_DELETE"
  | "DEPARTMENT_ASSIGN_HEAD"
  | "AGENCY_CREATE"
  | "AGENCY_UPDATE"
  | "AGENCY_DELETE"
  | "AGENCY_ASSIGN_HEAD"
  | "REPORT_ACCESS"
  | "REPORT_EXPORT"
  | "REPORT_DELETE"
  | "REPORT_RERUN"
  | "AUDIT_EXPORT"
  | "AUDIT_PURGE"
  | "CACHE_INVALIDATION"
  | "JOB_TRIGGER"
  | "ESCALATION_RULE_TOGGLE"
  | "ESCALATION_RULE_TRIGGER"
  | "SETTINGS_CHANGE";

export const ALL_AUDIT_ACTIONS: AuditAction[] = [
  "LOGIN_SUCCESS",
  "LOGIN_FAILURE",
  "LOGOUT",
  "INCIDENT_CREATE",
  "INCIDENT_UPDATE",
  "INCIDENT_VALIDATE",
  "INCIDENT_SUBMIT_SOLUTION",
  "INCIDENT_DIRECTION_VALIDATE",
  "INCIDENT_DIRECTION_REJECT",
  "INCIDENT_TRANSFER",
  "INCIDENT_STATUS_CHANGE",
  "INCIDENT_RESOLVE",
  "INCIDENT_CLOSE",
  "INCIDENT_DELETE",
  "INCIDENT_SLA_REMINDER",
  "USER_CREATE",
  "USER_UPDATE",
  "USER_DELETE",
  "USER_PASSWORD_CHANGE",
  "USER_PASSWORD_RESET",
  "ROLE_CREATE",
  "ROLE_UPDATE",
  "ROLE_DELETE",
  "DEPARTMENT_CREATE",
  "DEPARTMENT_UPDATE",
  "DEPARTMENT_DELETE",
  "DEPARTMENT_ASSIGN_HEAD",
  "AGENCY_CREATE",
  "AGENCY_UPDATE",
  "AGENCY_DELETE",
  "AGENCY_ASSIGN_HEAD",
  "REPORT_ACCESS",
  "REPORT_EXPORT",
  "REPORT_DELETE",
  "REPORT_RERUN",
  "AUDIT_EXPORT",
  "AUDIT_PURGE",
  "CACHE_INVALIDATION",
  "JOB_TRIGGER",
  "ESCALATION_RULE_TOGGLE",
  "ESCALATION_RULE_TRIGGER",
  "SETTINGS_CHANGE",
];

// Centralise la logique d'interface liee a audit statut.
export type AuditStatus = "SUCCESS" | "FAILURE";

// Definit les constantes audit status colors.
export const AUDIT_STATUS_COLORS: Record<AuditStatus, string> = {
  SUCCESS: "green",
  FAILURE: "red",
};

// Determine audit statut color e partir du contexte fourni.
export const getAuditStatusColor = (status: string | undefined): string => {
  if (status === "SUCCESS") return "green";
  if (status === "FAILURE") return "red";
  return "default";
};

// Modele le resume utilisateur retourne par l'API.
export interface UserSummaryResponse {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email?: string;
}

// Centralise la logique d'interface liee a audit enum response.
export interface AuditEnumResponse {
  code: string;
  name: string;
  description: string;
}

// Centralise la logique d'interface liee a audit log response.
export interface AuditLogResponse {
  id: string;
  timestamp: string;
  user?: UserSummaryResponse;
  username: string;
  roles: string[];
  action: AuditAction;
  resourceType: string;
  resourceId?: string;
  ipAddress?: string;
  userAgent?: string;
  status: AuditStatus;
  details?: Record<string, unknown>;
}

// Centralise la logique d'interface liee a audit logs params.
export interface AuditLogsParams {
  page?: number;
  size?: number;
  sort?: string;
  keyword?: string;
  action?: string;
  status?: string;
  resourceType?: string;
  from?: string;
  to?: string;
}

// Centralise la logique d'interface liee a paged audit response.
export interface PagedAuditResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
