// Definitions des URL des endpoints du microservice d'audit.
import { SERVICE_BASES } from "../../base";

// Expose la constante AUDIT_ENDPOINTS utilisee par endpoints.
export const AUDIT_ENDPOINTS = {
  AUDIT_LOGS: {
    BASE: `${SERVICE_BASES.AUDIT}/audit-logs`,
    ALL: `${SERVICE_BASES.AUDIT}/audit-logs`,
    BY_ID: (id: string) => `${SERVICE_BASES.AUDIT}/audit-logs/${id}`,
    BY_USER: (userId: string) =>
      `${SERVICE_BASES.AUDIT}/audit-logs/user/${userId}`,
    BY_ACTION: (action: string) =>
      `${SERVICE_BASES.AUDIT}/audit-logs/action/${action}`,
    BY_RESOURCE_TYPE: (resourceType: string) =>
      `${SERVICE_BASES.AUDIT}/audit-logs/resource/${resourceType}`,
    BY_RESOURCE: (resourceType: string, resourceId: string) =>
      `${SERVICE_BASES.AUDIT}/audit-logs/resource/${resourceType}/${resourceId}`,
    BY_STATUS: (status: string) =>
      `${SERVICE_BASES.AUDIT}/audit-logs/status/${status}`,
    BY_RANGE: `${SERVICE_BASES.AUDIT}/audit-logs/range`,
  },
  ENUMS: {
    AUDIT_ACTIONS: `${SERVICE_BASES.AUDIT}/enums/audit-actions`,
    AUDIT_STATUSES: `${SERVICE_BASES.AUDIT}/enums/audit-statuses`,
  },
} as const;
