// Client API pour obtenir les statistiques utilisateur.

import apiClient from "../../client";
import { USER_SERVICE_ENDPOINTS } from "../endpoints/endpoints";
import {
  PeriodType,
  type PeriodFilter,
  type UserStatsResponse,
} from "../../../types/dashboard";

// Charge les indicateurs affiches par l'ecran.
export const userStatsApi = {
  getUserStats: async (
    filter: PeriodFilter,
    signal?: AbortSignal,
  ): Promise<UserStatsResponse> => {
    const params: Record<string, string> = {
      period: filter.period,
    };

    if (filter.period === PeriodType.CUSTOM) {
      if (filter.dateFrom) params.dateFrom = filter.dateFrom;
      if (filter.dateTo) params.dateTo = filter.dateTo;
    }

    if (filter.view) params.view = filter.view;
    if (filter.agencyId) params.agencyId = filter.agencyId;
    if (filter.serviceId) params.serviceId = filter.serviceId;

    const { data } = await apiClient.get<UserStatsResponse>(
      USER_SERVICE_ENDPOINTS.USERS.STATS,
      { params, signal },
    );
    return data;
  },
};
