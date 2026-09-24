// Service API de consultation des agences.
import apiClient from "../../client";
import { API_ROUTES } from "../../routes";
import type { Agency } from "../types";

// Prepare agency api pour agence API.
export const agencyApi = {
  getAll: async (signal?: AbortSignal): Promise<Agency[]> => {
    const { data } = await apiClient.get<Agency[]>(API_ROUTES.AGENCIES.ALL, {
      signal,
    });
    return data;
  },

  getById: async (id: string, signal?: AbortSignal): Promise<Agency> => {
    const { data } = await apiClient.get<Agency>(
      `${API_ROUTES.AGENCIES.BASE}/${id}`,
      { signal },
    );
    return data;
  },
};
