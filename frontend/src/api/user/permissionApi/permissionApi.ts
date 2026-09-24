// Service API de consultation des permissions.
import apiClient from "../../client";
import { API_ROUTES } from "../../routes";
import type { Permission } from "../types";

// Prepare permission api pour permission API.
export const permissionApi = {
  getAll: async (signal?: AbortSignal): Promise<Permission[]> => {
    const { data } = await apiClient.get<Permission[]>(
      API_ROUTES.PERMISSIONS.ALL,
      { signal },
    );
    return data;
  },

  getById: async (id: string, signal?: AbortSignal): Promise<Permission> => {
    const { data } = await apiClient.get<Permission>(
      `${API_ROUTES.PERMISSIONS.BASE}/${id}`,
      { signal },
    );
    return data;
  },
};
