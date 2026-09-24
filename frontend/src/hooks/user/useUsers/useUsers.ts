// Hooks utilisateurs : recherche, consultation et actions d'administration.

import {
  keepPreviousData,
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";
import { useMemo, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { userApi } from "../../../api/user/userApi/userApi";
import { QUERY_KEYS } from "../../../utils/constants";
import { App } from "antd";
import {
  getApiErrorMessage,
  getApiSuccessMessage,
} from "../../../utils/apiMessages/apiMessages";
import type {
  CreateUserRequest,
  ChangePasswordRequest,
} from "../../../api/user/types";

// Charge les utilisateurs et les presente dans un ordre stable pour l'administration.
export const useUsers = (options?: { enabled?: boolean }) => {
  const query = useQuery({
    queryKey: QUERY_KEYS.USERS.ALL,
    queryFn: ({ signal }) => userApi.getAll(signal),
    enabled: options?.enabled ?? true,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  const optimizedData = useMemo(() => {
    if (!query.data) return [];

    return [...query.data].sort((a, b) => a.username.localeCompare(b.username));
  }, [query.data]);

  return {
    ...query,
    data: optimizedData,
  };
};

// Charge les utilisateurs assignables au traitement/resolution d'un incident.
export const useAssignableUsers = (
  params?: { serviceId?: string },
  options?: { enabled?: boolean },
) => {
  const query = useQuery({
    queryKey: [...QUERY_KEYS.USERS.ALL, "assignable", params],
    queryFn: ({ signal }) => userApi.getAssignable(params, signal),
    enabled: options?.enabled ?? true,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
  });

  const optimizedData = useMemo(() => {
    if (!query.data) return [];
    return [...query.data].sort((a, b) =>
      a.username.localeCompare(b.username),
    );
  }, [query.data]);

  return {
    ...query,
    data: optimizedData,
  };
};

// Charge une page d'utilisateurs avec les criteres fournis par l'ecran.
export const usePaginatedUsers = (
  params?: Record<string, unknown>,
  realtimeKey?: readonly unknown[],
  options?: { enabled?: boolean },
) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.USERS.ALL, "paginated", params, realtimeKey],
    queryFn: ({ signal }) => userApi.getPaginated(params, signal),
    enabled: options?.enabled ?? true,
    placeholderData: keepPreviousData,
    staleTime: 5 * 60 * 1000,
    gcTime: 15 * 60 * 1000,
  });
};

// Charge le detail d'un utilisateur lorsque son identifiant est connu.
export const useUser = (id: string | undefined) => {
  return useQuery({
    queryKey: QUERY_KEYS.USERS.DETAIL(id || ""),
    queryFn: ({ signal }) =>
      id
        ? userApi.getById(id, signal)
        : Promise.reject(new Error("Identifiant utilisateur absent")),
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
    gcTime: 15 * 60 * 1000,
  });
};

// Met a jour un utilisateur et synchronise les caches concernes.
export const useUpdateUser = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const invalidateQueries = useCallback(
    (userId?: string) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.USERS.ALL });
      if (userId) {
        queryClient.invalidateQueries({
          queryKey: QUERY_KEYS.USERS.DETAIL(userId),
        });
      }
    },
    [queryClient],
  );

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: Partial<CreateUserRequest>;
    }) => userApi.update(id, data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("users.form.messages.update_success");
      message.success(successMessage);
      invalidateQueries(response?.id);
    },
    onError: (error) => {
      const errorMessage = getApiErrorMessage(error);
      message.error(errorMessage);
    },
  });
};

// Cree un utilisateur et recharge la liste d'administration.
export const useCreateUser = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateUserRequest) => userApi.create(data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("users.form.messages.create_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.USERS.ALL });
    },
    onError: (error) => {
      const errorMessage = getApiErrorMessage(error);
      message.error(errorMessage);
    },
  });
};

// Supprime un utilisateur et recharge la liste d'administration.
export const useDeleteUser = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => userApi.delete(id),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("users.form.messages.delete_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.USERS.ALL });
    },
    onError: (error) => {
      const errorMessage = getApiErrorMessage(error);
      message.error(errorMessage);
    },
  });
};

// Active ou desactive un compte utilisateur.
export const useToggleUserStatus = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => userApi.toggleStatus(id),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("users.form.messages.status_update_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.USERS.ALL });
      if (response?.id) {
        queryClient.invalidateQueries({
          queryKey: QUERY_KEYS.USERS.DETAIL(response.id),
        });
      }
    },
    onError: (error) => {
      const errorMessage = getApiErrorMessage(error);
      message.error(errorMessage);
    },
  });
};

// Regenere un mot de passe temporaire pour un utilisateur.
export const useRegeneratePassword = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => userApi.regeneratePassword(id),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("users.form.messages.password_reset_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.USERS.ALL });
      if (response && response.id) {
        queryClient.invalidateQueries({
          queryKey: QUERY_KEYS.USERS.DETAIL(response.id),
        });
      }
    },
    onError: (error) => {
      const errorMessage = getApiErrorMessage(error);
      message.error(errorMessage);
    },
  });
};

// Change le mot de passe d'un utilisateur apres validation de securite.
export const useChangePassword = () => {
  const { message } = App.useApp();
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: ChangePasswordRequest }) =>
      userApi.changePassword(id, data),
    onSuccess: (response) => {
      const successMessage =
        getApiSuccessMessage(response) ||
        t("users.form.messages.password_change_success");
      message.success(successMessage);
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.USERS.ALL });
      if (response && response.id) {
        queryClient.invalidateQueries({
          queryKey: QUERY_KEYS.USERS.DETAIL(response.id),
        });
      }
    },
    onError: (error) => {
      const errorMessage = getApiErrorMessage(error);
      message.error(errorMessage);
    },
  });
};

// Charge la liste des utilisateurs disposant de la permission VALIDATION_DIRECTION.
export const useDirectionValidators = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: [...QUERY_KEYS.USERS.ALL, "validators"],
    queryFn: () => userApi.getValidators(),
    enabled: options?.enabled ?? true,
    staleTime: 5 * 60 * 1000,
  });
};

