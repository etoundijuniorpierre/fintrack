// Service API de consultation des services (departements).
import apiClient from "../../client";
import { API_ROUTES } from "../../routes";
import type { Service } from "../types";

// Prepare service api pour service API.
export const serviceApi = {
  getAll: async (signal?: AbortSignal): Promise<Service[]> => {
    const { data } = await apiClient.get<Service[]>(API_ROUTES.SERVICES.ALL, {
      signal,
    });
    return data;
  },

  getById: async (id: string, signal?: AbortSignal): Promise<Service> => {
    const { data } = await apiClient.get<Service>(
      `${API_ROUTES.SERVICES.BASE}/${id}`,
      { signal },
    );
    return data;
  },
};
