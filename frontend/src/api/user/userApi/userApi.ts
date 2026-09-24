// Service API de gestion des utilisateurs : CRUD, activation et reinitialisation de mot de passe.
import apiClient from "../../client";
import { API_ROUTES } from "../../routes";
import type { User, UserSummary, CreateUserRequest, ChangePasswordRequest } from "../types";

// Expose les appels HTTP de gestion des utilisateurs.
export const userApi = {
  getAll: async (signal?: AbortSignal): Promise<User[]> => {
    const { data } = await apiClient.get<User[]>(API_ROUTES.USERS.ALL, {
      signal,
    });
    return data;
  },

  getAssignable: async (
    params?: { serviceId?: string;},
    signal?: AbortSignal,
  ): Promise<UserSummary[]> => {
    const { data } = await apiClient.get<UserSummary[]>(
      API_ROUTES.USERS.ASSIGNABLE,
      { params, signal },
    );
    return data;
  },

  getValidators: async (signal?: AbortSignal): Promise<UserSummary[]> => {
    const { data } = await apiClient.get<UserSummary[]>(
      API_ROUTES.USERS.VALIDATORS,
      { signal },
    );
    return data;
  },

  getPaginated: async (
    params?: Record<string, unknown>,
    signal?: AbortSignal,
  ): Promise<{ content: User[]; totalElements: number }> => {
    const { data } = await apiClient.get<{
      content: User[];
      totalElements: number;
    }>(API_ROUTES.USERS.BASE, { params, signal });
    return data;
  },

  getById: async (id: string, signal?: AbortSignal): Promise<User> => {
    const { data } = await apiClient.get<User>(
      `${API_ROUTES.USERS.BASE}/${id}`,
      { signal },
    );
    return data;
  },

  create: async (user: CreateUserRequest): Promise<User> => {
    const { data } = await apiClient.post<User>(API_ROUTES.USERS.BASE, user);
    return data;
  },

  update: async (
    id: string,
    user: Partial<CreateUserRequest>,
  ): Promise<User> => {
    const { data } = await apiClient.put<User>(
      `${API_ROUTES.USERS.BASE}/${id}`,
      user,
    );
    return data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`${API_ROUTES.USERS.BASE}/${id}`);
  },

  toggleStatus: async (id: string): Promise<User> => {
    const { data } = await apiClient.patch<User>(
      `${API_ROUTES.USERS.BASE}/${id}/status`,
    );
    return data;
  },

  regeneratePassword: async (id: string): Promise<User> => {
    const { data } = await apiClient.post<User>(
      `${API_ROUTES.USERS.BASE}/${id}/regenerate-password`,
    );
    return data;
  },

  changePassword: async (
    id: string,
    request: ChangePasswordRequest,
  ): Promise<User> => {
    const { data } = await apiClient.post<User>(
      `${API_ROUTES.USERS.BASE}/${id}/change-password`,
      request,
    );
    return data;
  },
};
