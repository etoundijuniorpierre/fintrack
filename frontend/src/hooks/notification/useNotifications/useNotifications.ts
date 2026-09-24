// Hooks de notifications : consultation, lecture et suppression des messages utilisateur.

import {
  keepPreviousData,
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";
import { App } from "antd";
import { notificationApi } from "../../../api/notification";
import { QUERY_KEYS } from "../../../utils/constants";
import { getApiErrorMessage } from "../../../utils/apiMessages/apiMessages";
import type {
  NotificationsParams,
  NotificationRequest,
} from "../../../api/notification";
import { useTranslation } from "react-i18next";

// Charge le flux pagine ou filtre des notifications.
export const useNotifications = (params?: NotificationsParams) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.NOTIFICATIONS.ALL, params],
    queryFn: ({ signal }) => notificationApi.getAll(params, signal),
    placeholderData: keepPreviousData,
    staleTime: 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
};

// Charge le detail d'une notification selectionnee.
export const useNotification = (id: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.NOTIFICATIONS.DETAIL(id!),
    queryFn: ({ signal }) => notificationApi.getById(id!, signal),
    enabled: !!id,
  });
};

// Charge les notifications correspondant a un statut donne.
export const useNotificationsByStatus = (
  status: string | undefined,
  enabled = true,
) => {
  return useQuery({
    queryKey: QUERY_KEYS.NOTIFICATIONS.BY_STATUS(status!),
    queryFn: ({ signal }) => notificationApi.getByStatus(status!, signal),
    enabled: enabled && !!status,
  });
};

// Charge les notifications liees a un incident.
export const useNotificationsByIncident = (incidentId: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.NOTIFICATIONS.BY_INCIDENT(incidentId!),
    queryFn: ({ signal }) =>
      notificationApi.getByIncidentId(incidentId!, signal),
    enabled: !!incidentId,
  });
};

// Charge les notifications destinees a un utilisateur ou canal.
export const useNotificationsByRecipient = (recipient: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.NOTIFICATIONS.BY_RECIPIENT(recipient!),
    queryFn: ({ signal }) => notificationApi.getByRecipient(recipient!, signal),
    enabled: !!recipient,
    placeholderData: keepPreviousData,
    staleTime: 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
};

// Charge les notifications destinees a un utilisateur avec pagination serveur.
export const useNotificationsByRecipientPage = (
  recipient: string | undefined,
  params?: NotificationsParams,
) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.NOTIFICATIONS.BY_RECIPIENT(recipient!), params],
    queryFn: ({ signal }) =>
      notificationApi.getByRecipientPage(recipient!, params, signal),
    enabled: !!recipient,
    placeholderData: keepPreviousData,
    staleTime: 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });
};

// Charge les types de notifications exposes par le backend.
export const useNotificationTypes = () => {
  return useQuery({
    queryKey: QUERY_KEYS.NOTIFICATIONS.TYPES,
    queryFn: ({ signal }) => notificationApi.getNotificationTypes(signal),
    staleTime: 10 * 60 * 1000,
  });
};

// Charge les statuts de notification exposes par le backend.
export const useNotificationStatuses = () => {
  return useQuery({
    queryKey: QUERY_KEYS.NOTIFICATIONS.STATUSES,
    queryFn: ({ signal }) => notificationApi.getNotificationStatuses(signal),
    staleTime: 10 * 60 * 1000,
  });
};

// Cree une notification manuelle puis rafraichit le flux.
export const useCreateNotification = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: NotificationRequest) => notificationApi.create(data),
    onSuccess: () => {
      message.success(t("notifications.messages.create_success"));
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATIONS.ALL });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.NOTIFICATIONS.RECIPIENTS,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Marque une notification comme lue sans bruit utilisateur inutile.
export const useMarkNotificationRead = () => {
  const { message } = App.useApp();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATIONS.ALL });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.NOTIFICATIONS.RECIPIENTS,
      });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.NOTIFICATIONS.DETAIL(id),
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Marque tout le flux comme lu et confirme l'action a l'utilisateur.
export const useMarkAllNotificationsRead = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      message.success(t("notifications.messages.markAllReadSuccess"));
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATIONS.ALL });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.NOTIFICATIONS.RECIPIENTS,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Supprime une notification et rafraichit le flux.
export const useDeleteNotification = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => notificationApi.delete(id),
    onSuccess: () => {
      message.success(t("notifications.messages.delete_success"));
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATIONS.ALL });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.NOTIFICATIONS.RECIPIENTS,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};

// Supprime une selection de notifications et rafraichit le flux.
export const useBulkDeleteNotifications = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (ids: string[]) => notificationApi.bulkDelete(ids),
    onSuccess: () => {
      message.success(t("notifications.messages.bulk_delete_success"));
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.NOTIFICATIONS.ALL });
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.NOTIFICATIONS.RECIPIENTS,
      });
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });
};
