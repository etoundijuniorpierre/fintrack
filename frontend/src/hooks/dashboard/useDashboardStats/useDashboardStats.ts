// Hook React pour charger les graphiques statistiques du tableau de bord.

import { useQuery } from "@tanstack/react-query";
import { incidentStatsApi } from "../../../api/incident/incidentStatsApi/incidentStatsApi";
import { userStatsApi } from "../../../api/user/userStatsApi/userStatsApi";
import { QUERY_KEYS } from "../../../utils/constants";
import { PeriodType, type PeriodFilter } from "../../../types/dashboard";

// Charge les indicateurs affiches par l'ecran.
export const useDashboardStats = (filter: PeriodFilter) => {
  const isEnabled =
    filter.period !== PeriodType.CUSTOM ||
    (!!filter.dateFrom && !!filter.dateTo);

  const { data, isLoading, isError } = useQuery({
    queryKey: QUERY_KEYS.DASHBOARD.STATS(
      filter as unknown as Record<string, unknown>,
    ),
    queryFn: ({ signal }) => incidentStatsApi.getStats(filter, signal),
    staleTime: 60 * 1000,
    enabled: isEnabled,
  });

  return { data, isLoading: isLoading && isEnabled, isError };
};

// Charge les indicateurs affiches par l'ecran.
export const useUserStats = (filter: PeriodFilter, enabled = true) => {
  const isEnabled =
    filter.period !== PeriodType.CUSTOM ||
    (!!filter.dateFrom && !!filter.dateTo);

  const { data, isLoading, isError } = useQuery({
    queryKey: QUERY_KEYS.DASHBOARD.USER_STATS(
      filter as unknown as Record<string, unknown>,
    ),
    queryFn: ({ signal }) => userStatsApi.getUserStats(filter, signal),
    staleTime: 60 * 1000,
    enabled: enabled && isEnabled,
  });

  return { data, isLoading: isLoading && enabled && isEnabled, isError };
};
