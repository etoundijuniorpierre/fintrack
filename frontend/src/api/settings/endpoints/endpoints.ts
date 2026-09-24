// Endpoints de la section parametres (types d'incident, agences, services, roles, planifications).
import { SERVICE_BASES } from "../../base";

// Expose la constante SETTINGS_ENDPOINTS utilisee par endpoints.
export const SETTINGS_ENDPOINTS = {
  INCIDENT_TYPES: {
    BASE: `${SERVICE_BASES.INCIDENT}/incident-type-configs`,
    ALL: `${SERVICE_BASES.INCIDENT}/incident-type-configs/all`,
    BY_ID: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incident-type-configs/${id}`,
  },
  AGENCIES: {
    BASE: `${SERVICE_BASES.USER}/agencies`,
    ALL: `${SERVICE_BASES.USER}/agencies/all`,
    BY_ID: (id: string) => `${SERVICE_BASES.USER}/agencies/${id}`,
    ASSIGN_HEAD: (id: string, userId: string) =>
      `${SERVICE_BASES.USER}/agencies/${id}/head/${userId}`,
  },
  SERVICES: {
    BASE: `${SERVICE_BASES.USER}/departments`,
    ALL: `${SERVICE_BASES.USER}/departments/all`,
    BY_ID: (id: string) => `${SERVICE_BASES.USER}/departments/${id}`,
    ASSIGN_HEAD: (id: string, userId: string) =>
      `${SERVICE_BASES.USER}/departments/${id}/head/${userId}`,
  },
  ROLES: {
    BASE: `${SERVICE_BASES.USER}/roles`,
    ALL: `${SERVICE_BASES.USER}/roles/all`,
    BY_ID: (id: string) => `${SERVICE_BASES.USER}/roles/${id}`,
  },
  PERMISSIONS: {
    BASE: `${SERVICE_BASES.USER}/permissions`,
    ALL: `${SERVICE_BASES.USER}/permissions/all`,
  },
  REPORT_SCHEDULES: {
    BASE: `${SERVICE_BASES.REPORTING}/report-schedules`,
    ALL: `${SERVICE_BASES.REPORTING}/report-schedules/all`,
    BY_ID: (id: string) => `${SERVICE_BASES.REPORTING}/report-schedules/${id}`,
    TOGGLE: (id: string) =>
      `${SERVICE_BASES.REPORTING}/report-schedules/${id}/toggle`,
  },
} as const;
