// Definitions des URL des endpoints du microservice incident.
import { SERVICE_BASES } from "../../base";

// Expose la constante INCIDENT_SERVICE_ENDPOINTS utilisee par endpoints.
export const INCIDENT_SERVICE_ENDPOINTS = {
  INCIDENTS: {
    BASE: `${SERVICE_BASES.INCIDENT}/incidents`,
    BY_ID: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}`,
    BY_REFERENCE: (reference: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/by-reference/${encodeURIComponent(reference)}`,
    REPORT: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/report`,
    VALIDATE: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/validate`,
    REJECT: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/reject`,
    TRANSFER: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/transfer`,
    ASSIGN: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/assign`,
    START: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/start`,
    BLOCK: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/block`,
    RESUME: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/resume`,
    REQUEST_CONFIRMATION: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/request-confirmation`,
    CONFIRM_RELEVANCE: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/confirm-relevance`,
    TREAT: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/treat`,
    RESOLVE: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/resolve`,
    MARK_UNRESOLVED: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/mark-unresolved`,
    CLOSE: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/close`,
    REOPEN: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/reopen`,
    CLONE: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/clone`,
    RESUBMIT: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/resubmit`,
    SUBMIT_SOLUTION: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/submit-solution`,
    DIRECTION_VALIDATE: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/direction-validate`,
    DIRECTION_REJECT: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/direction-reject`,
    CANCEL: (id: string) => `${SERVICE_BASES.INCIDENT}/incidents/${id}/cancel`,
    COMMENTS: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/comments`,
    HISTORY: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incidents/${id}/history`,
    STATS: `${SERVICE_BASES.INCIDENT}/incidents/stats`,
  },
  ENUMS: {
    STATUSES: `${SERVICE_BASES.INCIDENT}/enums/incident-statuses`,
    CRITICALITIES: `${SERVICE_BASES.INCIDENT}/enums/criticalities`,
    ACTION_TYPES: `${SERVICE_BASES.INCIDENT}/enums/action-types`,
    PERIOD_TYPES: `${SERVICE_BASES.INCIDENT}/enums/period-types`,
    CAUSES: `${SERVICE_BASES.INCIDENT}/enums/incident-causes`,
  },
  DASHBOARD: {
    METRICS: `${SERVICE_BASES.INCIDENT}/dashboard/metrics`,
    SYNTHESIS: `${SERVICE_BASES.INCIDENT}/dashboard/synthesis`,
    COMPARISON: `${SERVICE_BASES.INCIDENT}/dashboard/metrics/comparison`,
  },
  INCIDENT_TYPE_CONFIGS: {
    ALL: `${SERVICE_BASES.INCIDENT}/incident-type-configs/all`,
    BASE: `${SERVICE_BASES.INCIDENT}/incident-type-configs`,
    BY_ID: (id: string) =>
      `${SERVICE_BASES.INCIDENT}/incident-type-configs/${id}`,
  },
} as const;
