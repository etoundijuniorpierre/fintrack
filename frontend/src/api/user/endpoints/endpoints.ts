// Definition des constantes d'endpoints d'API.

import { SERVICE_BASES } from "../../base";

// Expose la constante USER_SERVICE_ENDPOINTS utilisee par endpoints.
export const USER_SERVICE_ENDPOINTS = {
  AUTH: {
    LOGIN: `${SERVICE_BASES.USER}/auth/login`,
    LOGOUT: `${SERVICE_BASES.USER}/auth/logout`,
    REFRESH: `${SERVICE_BASES.USER}/auth/refresh`,
    CHANGE_PASSWORD: (id: string) =>
      `${SERVICE_BASES.USER}/users/${id}/change-password`,
    CONTACT_ADMIN: `${SERVICE_BASES.USER}/auth/contact-admin`,
  },
  USERS: {
    BASE: `${SERVICE_BASES.USER}/users`,
    ALL: `${SERVICE_BASES.USER}/users/all`,
    ASSIGNABLE: `${SERVICE_BASES.USER}/users/assignable`,
    VALIDATORS: `${SERVICE_BASES.USER}/users/validators`,
    PROFILE: (id: string) => `${SERVICE_BASES.USER}/users/${id}/profile`,
    STATS: `${SERVICE_BASES.USER}/users/stats`,
  },
  ROLES: {
    BASE: `${SERVICE_BASES.USER}/roles`,
    ALL: `${SERVICE_BASES.USER}/roles/all`,
  },
  AGENCIES: {
    BASE: `${SERVICE_BASES.USER}/agencies`,
    ALL: `${SERVICE_BASES.USER}/agencies/all`,
  },
  SERVICES: {
    BASE: `${SERVICE_BASES.USER}/departments`,
    ALL: `${SERVICE_BASES.USER}/departments/all`,
  },
  PERMISSIONS: {
    BASE: `${SERVICE_BASES.USER}/permissions`,
    ALL: `${SERVICE_BASES.USER}/permissions/all`,
  },
} as const;
