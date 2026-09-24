// Hook React pour charger les metriques cles du tableau de bord.
import { useQuery } from "@tanstack/react-query";
import { incidentStatsApi } from "../../../api/incident/incidentStatsApi/incidentStatsApi";
import {
  PeriodType,
  type DashboardMetricsFilters,
} from "../../../types/dashboard";

// Charge les indicateurs du tableau de bord.
export const useDashboardMetrics = (
  view: string,
  filters: DashboardMetricsFilters = {},
) => {
  const hasCompleteCustomRange =
    filters.period !== PeriodType.CUSTOM ||
    Boolean(filters.dateFrom && filters.dateTo);

  return useQuery({
    queryKey: [
      "dashboard",
      "metrics",
      view,
      filters.agencyId ?? null,
      filters.serviceId ?? null,
      filters.targetUserId ?? null,
      filters.year ?? null,
      filters.period ?? null,
      filters.dateFrom ?? null,
      filters.dateTo ?? null,
    ],
    queryFn: ({ signal }) =>
      incidentStatsApi.getDashboardMetrics(view, filters, signal),
    staleTime: 60 * 1000,
    enabled: hasCompleteCustomRange,
  });
};
