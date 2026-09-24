// Jeux de donnees simules pour notification.

import type {
  NotificationResponse,
  PagedNotificationResponse,
  NotificationType,
  NotificationStatus,
  NotificationEnumResponse,
  IncidentSummaryResponse as NotificationIncidentSummary,
} from "../../api/notification/types";

// Fabrique une fixture de test pour notification fixtures.
export const makeNotification = (
  overrides: Partial<NotificationResponse> = {},
): NotificationResponse => ({
  id: "notif-1",
  type: "EMAIL" as NotificationType,
  recipient: "john.doe@finstar.com",
  status: "PENDING" as NotificationStatus,
  createdAt: "2024-01-01T08:00:00Z",
  updatedAt: "2024-01-01T08:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour notification fixtures.
export const makePagedNotificationResponse = <T>(
  items: T[],
  overrides: Partial<PagedNotificationResponse<T>> = {},
): PagedNotificationResponse<T> => ({
  content: items,
  totalElements: items.length,
  totalPages: 1,
  size: items.length || 10,
  number: 0,
  ...overrides,
});

// Fabrique une fixture de test pour notification fixtures.
export const makeNotificationEnumResponse = (
  overrides: Partial<NotificationEnumResponse> = {},
): NotificationEnumResponse => ({
  code: "EMAIL",
  name: "Email",
  description: "Email notification",
  ...overrides,
});

// Fabrique une fixture de test pour notification fixtures.
export const makeNotificationIncidentSummary = (
  overrides: Partial<NotificationIncidentSummary> = {},
): NotificationIncidentSummary => ({
  id: "incident-1",
  title: "Sample Incident",
  status: "OPEN",
  ...overrides,
});
