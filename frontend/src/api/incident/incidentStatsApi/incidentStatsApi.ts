// Service API des statistiques d'incidents et des metriques du tableau de bord.
import apiClient from "../../client";
import { INCIDENT_SERVICE_ENDPOINTS } from "../endpoints/endpoints";
import type {
  ComparisonResponse,
  DashboardMetricsFilters,
  DashboardMetricsResponse,
  IncidentStatsResponse,
  PeriodFilter,
} from "../../../types/dashboard";
import { PeriodType } from "../../../types/dashboard";

// Prepare incident stats api pour incident statistiques API.
export const incidentStatsApi = {
  getStats: async (
    filter: PeriodFilter,
    signal?: AbortSignal,
  ): Promise<IncidentStatsResponse> => {
    const params: Record<string, string> = {
      period: filter.period,
    };

    // Periode personnalisee : ajoute les bornes de dates aux parametres.
    if (filter.period === PeriodType.CUSTOM) {
      if (filter.dateFrom) params.dateFrom = filter.dateFrom;
      if (filter.dateTo) params.dateTo = filter.dateTo;
    }

    const { data } = await apiClient.get<IncidentStatsResponse>(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.STATS,
      { params, signal },
    );
    return data;
  },

  getDashboardMetrics: async (
    view: string,
    filters: DashboardMetricsFilters = {},
    signal?: AbortSignal,
  ): Promise<DashboardMetricsResponse> => {
    const { data } = await apiClient.get<DashboardMetricsResponse>(
      INCIDENT_SERVICE_ENDPOINTS.DASHBOARD.METRICS,
      {
        params: {
          view,
          ...(filters.agencyId ? { agencyId: filters.agencyId } : {}),
          ...(filters.serviceId ? { serviceId: filters.serviceId } : {}),
          ...(filters.targetUserId
            ? { targetUserId: filters.targetUserId }
            : {}),
          ...(filters.year ? { year: filters.year } : {}),
          ...(filters.period ? { period: filters.period } : {}),
          ...(filters.dateFrom ? { dateFrom: filters.dateFrom } : {}),
          ...(filters.dateTo ? { dateTo: filters.dateTo } : {}),
        },
        signal,
      },
    );
    return data;
  },

  getComparison: async (
    entityType: "USER" | "SERVICE" | "AGENCY",
    ids: string[],
    filters: Pick<
      DashboardMetricsFilters,
      "year" | "period" | "dateFrom" | "dateTo"
    > = {},
    signal?: AbortSignal,
  ): Promise<ComparisonResponse> => {
    const { data } = await apiClient.get<ComparisonResponse>(
      INCIDENT_SERVICE_ENDPOINTS.DASHBOARD.COMPARISON,
      {
        params: {
          entityType,
          ids,
          ...(filters.year ? { year: filters.year } : {}),
          ...(filters.period ? { period: filters.period } : {}),
          ...(filters.dateFrom ? { dateFrom: filters.dateFrom } : {}),
          ...(filters.dateTo ? { dateTo: filters.dateTo } : {}),
        },
        paramsSerializer: { indexes: null },
        signal,
      },
    );
    return data;
  },

  getSynthesisMetrics: async (
    filters: Pick<
      DashboardMetricsFilters,
      "year" | "period" | "dateFrom" | "dateTo"
    > = {},
    signal?: AbortSignal,
  ): Promise<DashboardMetricsResponse> => {
    const { data } = await apiClient.get<DashboardMetricsResponse>(
      INCIDENT_SERVICE_ENDPOINTS.DASHBOARD.SYNTHESIS,
      {
        params: filters,
        signal,
      },
    );
    return data;
  },
};
