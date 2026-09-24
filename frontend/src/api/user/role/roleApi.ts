// Service API de gestion des roles.
import apiClient from "../../client";
import { API_ROUTES } from "../../routes";
import type { Role, RoleRequest } from "../types";

// Prepare role api pour role API.
export const roleApi = {
  getAll: async (signal?: AbortSignal): Promise<Role[]> => {
    const { data } = await apiClient.get<Role[]>(API_ROUTES.ROLES.ALL, {
      signal,
    });
    return data;
  },

  getById: async (id: string, signal?: AbortSignal): Promise<Role> => {
    const { data } = await apiClient.get<Role>(
      `${API_ROUTES.ROLES.BASE}/${id}`,
      { signal },
    );
    return data;
  },

  create: async (role: RoleRequest): Promise<Role> => {
    const { data } = await apiClient.post<Role>(API_ROUTES.ROLES.BASE, role);
    return data;
  },
};
