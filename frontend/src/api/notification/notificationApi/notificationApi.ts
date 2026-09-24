// Service API du microservice notification : consultation, envoi et gestion des notifications.
import apiClient from "../../client";
import { NOTIFICATION_ENDPOINTS } from "../endpoints/endpoints";
import type {
  NotificationResponse,
  NotificationRequest,
  NotificationsParams,
  NotificationEnumResponse,
  PagedNotificationResponse,
  PresenceResponse,
} from "../types";

// Prepare notification api pour notification API.
export const notificationApi = {
  getAll: async (
    params?: NotificationsParams,
    signal?: AbortSignal,
  ): Promise<PagedNotificationResponse<NotificationResponse>> => {
    const { data } = await apiClient.get<
      PagedNotificationResponse<NotificationResponse>
    >(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.ALL, { params, signal });
    return data;
  },

  getAllList: async (signal?: AbortSignal): Promise<NotificationResponse[]> => {
    const { data } = await apiClient.get<NotificationResponse[]>(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.ALL_LIST,
      { signal },
    );
    return data;
  },

  // Etat initial de presence ; les mises a jour arrivent ensuite en WebSocket.
  getPresence: async (signal?: AbortSignal): Promise<PresenceResponse> => {
    const { data } = await apiClient.get<PresenceResponse>(
      NOTIFICATION_ENDPOINTS.PRESENCE,
      { signal },
    );
    return data;
  },

  getById: async (
    id: string,
    signal?: AbortSignal,
  ): Promise<NotificationResponse> => {
    const { data } = await apiClient.get<NotificationResponse>(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_ID(id),
      { signal },
    );
    return data;
  },

  getByStatus: async (
    status: string,
    signal?: AbortSignal,
  ): Promise<NotificationResponse[]> => {
    const { data } = await apiClient.get<NotificationResponse[]>(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_STATUS(status),
      { signal },
    );
    return data;
  },

  getByIncidentId: async (
    incidentId: string,
    signal?: AbortSignal,
  ): Promise<NotificationResponse[]> => {
    const { data } = await apiClient.get<NotificationResponse[]>(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_INCIDENT(incidentId),
      { signal },
    );
    return data;
  },

  getByRecipient: async (
    recipient: string,
    signal?: AbortSignal,
  ): Promise<NotificationResponse[]> => {
    const { data } = await apiClient.get<NotificationResponse[]>(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_RECIPIENT(recipient),
      { signal },
    );
    return data;
  },

  getByRecipientPage: async (
    recipient: string,
    params?: NotificationsParams,
    signal?: AbortSignal,
  ): Promise<PagedNotificationResponse<NotificationResponse>> => {
    const { data } = await apiClient.get<
      PagedNotificationResponse<NotificationResponse>
    >(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_RECIPIENT_PAGE(recipient), {
      params,
      signal,
    });
    return data;
  },

  create: async (data: NotificationRequest): Promise<NotificationResponse> => {
    const { data: response } = await apiClient.post<NotificationResponse>(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BASE,
      data,
    );
    return response;
  },

  markAsRead: async (id: string): Promise<NotificationResponse> => {
    const { data } = await apiClient.patch<NotificationResponse>(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.READ(id),
    );
    return data;
  },

  markAllAsRead: async (): Promise<NotificationResponse[]> => {
    const { data } = await apiClient.patch<NotificationResponse[]>(
      NOTIFICATION_ENDPOINTS.NOTIFICATIONS.READ_ALL,
    );
    return data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BY_ID(id));
  },

  bulkDelete: async (ids: string[]): Promise<void> => {
    await apiClient.delete(NOTIFICATION_ENDPOINTS.NOTIFICATIONS.BULK, {
      data: ids,
    });
  },

  getNotificationTypes: async (
    signal?: AbortSignal,
  ): Promise<NotificationEnumResponse[]> => {
    const { data } = await apiClient.get<NotificationEnumResponse[]>(
      NOTIFICATION_ENDPOINTS.ENUMS.NOTIFICATION_TYPES,
      { signal },
    );
    return data;
  },

  getNotificationStatuses: async (
    signal?: AbortSignal,
  ): Promise<NotificationEnumResponse[]> => {
    const { data } = await apiClient.get<NotificationEnumResponse[]>(
      NOTIFICATION_ENDPOINTS.ENUMS.NOTIFICATION_STATUSES,
      { signal },
    );
    return data;
  },
};
