// Definitions des URL des endpoints du microservice notification.
import { SERVICE_BASES } from "../../base";

// Expose la constante NOTIFICATION_ENDPOINTS utilisee par endpoints.
export const NOTIFICATION_ENDPOINTS = {
  NOTIFICATIONS: {
    BASE: `${SERVICE_BASES.NOTIFICATION}/notifications`,
    ALL: `${SERVICE_BASES.NOTIFICATION}/notifications`,
    ALL_LIST: `${SERVICE_BASES.NOTIFICATION}/notifications/all`,
    BY_ID: (id: string) => `${SERVICE_BASES.NOTIFICATION}/notifications/${id}`,
    BY_STATUS: (status: string) =>
      `${SERVICE_BASES.NOTIFICATION}/notifications/status/${status}`,
    BY_INCIDENT: (incidentId: string) =>
      `${SERVICE_BASES.NOTIFICATION}/notifications/incident/${incidentId}`,
    BY_RECIPIENT: (recipient: string) =>
      `${SERVICE_BASES.NOTIFICATION}/notifications/recipient/${recipient}`,
    BY_RECIPIENT_PAGE: (recipient: string) =>
      `${SERVICE_BASES.NOTIFICATION}/notifications/recipient/${recipient}/page`,
    READ: (id: string) =>
      `${SERVICE_BASES.NOTIFICATION}/notifications/${id}/read`,
    READ_ALL: `${SERVICE_BASES.NOTIFICATION}/notifications/read-all`,
    BULK: `${SERVICE_BASES.NOTIFICATION}/notifications/bulk`,
  },
  ENUMS: {
    NOTIFICATION_TYPES: `${SERVICE_BASES.NOTIFICATION}/enums/notification-types`,
    NOTIFICATION_STATUSES: `${SERVICE_BASES.NOTIFICATION}/enums/notification-statuses`,
  },
  PRESENCE: `${SERVICE_BASES.NOTIFICATION}/presence`,
} as const;
