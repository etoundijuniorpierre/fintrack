// Definition des constantes d'endpoints d'API.

import { SERVICE_BASES } from "../../base";

// Expose la constante REPORTING_ENDPOINTS utilisee par endpoints.
export const REPORTING_ENDPOINTS = {
  REPORTS: {
    BASE: `${SERVICE_BASES.REPORTING}/reports`,
    GENERATE: `${SERVICE_BASES.REPORTING}/reports/generate`,
    RETRY: (id: string) => `${SERVICE_BASES.REPORTING}/reports/${id}/retry`,
    DOWNLOAD: (id: string) =>
      `${SERVICE_BASES.REPORTING}/reports/${id}/download`,
    SEND_EMAIL: (id: string) =>
      `${SERVICE_BASES.REPORTING}/reports/${id}/send-email`,
    DELETE: (id: string) => `${SERVICE_BASES.REPORTING}/reports/${id}`,
  },
} as const;
