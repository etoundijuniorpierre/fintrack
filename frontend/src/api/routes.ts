// Regroupe toutes les routes API des microservices et l'URL de base.
import { USER_SERVICE_ENDPOINTS } from "./user/endpoints/endpoints";
import { INCIDENT_SERVICE_ENDPOINTS } from "./incident/endpoints/endpoints";
import { SETTINGS_ENDPOINTS } from "./settings/endpoints/endpoints";
import { AUDIT_ENDPOINTS } from "./audit/endpoints";
import { DOCUMENT_ENDPOINTS } from "./document/endpoints/endpoints";
import { NOTIFICATION_ENDPOINTS } from "./notification/endpoints/endpoints";
import { REPORTING_ENDPOINTS } from "./reporting/endpoints/endpoints";

// Definit les constantes api routes.
export const API_ROUTES = {
  AUTH: USER_SERVICE_ENDPOINTS.AUTH,
  USERS: USER_SERVICE_ENDPOINTS.USERS,
  ROLES: USER_SERVICE_ENDPOINTS.ROLES,
  AGENCIES: USER_SERVICE_ENDPOINTS.AGENCIES,
  SERVICES: USER_SERVICE_ENDPOINTS.SERVICES,
  PERMISSIONS: USER_SERVICE_ENDPOINTS.PERMISSIONS,
  INCIDENTS: INCIDENT_SERVICE_ENDPOINTS.INCIDENTS,
  ENUMS: INCIDENT_SERVICE_ENDPOINTS.ENUMS,
  SETTINGS: SETTINGS_ENDPOINTS,
  AUDIT: AUDIT_ENDPOINTS,
  DOCUMENTS: DOCUMENT_ENDPOINTS,
  NOTIFICATIONS: NOTIFICATION_ENDPOINTS,
  REPORTS: REPORTING_ENDPOINTS.REPORTS,
} as const;

// Definit les constantes api base url.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
