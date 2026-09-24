// Tests frontend : verifie le comportement de use tableau de bord stats.test.

import { renderHook, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useDashboardStats, useUserStats } from "./useDashboardStats";
import { incidentStatsApi } from "../../../api/incident/incidentStatsApi/incidentStatsApi";
import { userStatsApi } from "../../../api/user/userStatsApi/userStatsApi";
import type {
  PeriodFilter,
  IncidentStatsResponse,
  UserStatsResponse,
} from "../../../types/dashboard";

vi.mock("../../../api/incident/incidentStatsApi/incidentStatsApi", () => ({
  incidentStatsApi: {
    getStats: vi.fn(),
  },
}));

vi.mock("../../../api/user/userStatsApi/userStatsApi", () => ({
  userStatsApi: {
    getUserStats: vi.fn(),
  },
}));

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

const MOCK_INCIDENT_STATS: IncidentStatsResponse = {
  openCount: 5,
  pendingCount: 3,
  resolvedCount: 20,
  totalCount: 28,
  typeDistribution: [{ typeId: "type-1", typeName: "Bug", count: 10 }],
  openCountPreviousPeriod: 4,
  pendingCountPreviousPeriod: 2,
  resolvedCountPreviousPeriod: 18,
  period: "LAST_30_DAYS",
};

const MOCK_USER_STATS: UserStatsResponse = {
  totalCount: 100,
  activeCount: 80,
  inactiveCount: 20,
  connectedCount: 5,
  neverConnectedCount: 10,
  lockedCount: 2,
  firstLoginPendingCount: 3,
  recentlyActiveCount: 15,
  newUsersInPeriod: 7,
};

const FILTER_30_DAYS: PeriodFilter = { period: "LAST_30_DAYS" };
const FILTER_7_DAYS: PeriodFilter = { period: "LAST_7_DAYS" };

describe("useDashboardStats", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return isLoading true when the API call is pending", () => {
    vi.mocked(incidentStatsApi.getStats).mockReturnValue(new Promise(() => {}));
    const { result } = renderHook(() => useDashboardStats(FILTER_30_DAYS), {
      wrapper: createWrapper(),
    });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it("should return data when the API call resolves successfully", async () => {
    vi.mocked(incidentStatsApi.getStats).mockResolvedValueOnce(
      MOCK_INCIDENT_STATS,
    );
    const { result } = renderHook(() => useDashboardStats(FILTER_30_DAYS), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(MOCK_INCIDENT_STATS);
    expect(result.current.isError).toBe(false);
  });

  it("should return isError true when the API call rejects", async () => {
    vi.mocked(incidentStatsApi.getStats).mockRejectedValueOnce(
      new Error("Network error"),
    );
    const { result } = renderHook(() => useDashboardStats(FILTER_30_DAYS), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });

  it("should return updated data when the filter changes", async () => {
    vi.mocked(incidentStatsApi.getStats)
      .mockResolvedValueOnce(MOCK_INCIDENT_STATS)
      .mockResolvedValueOnce({
        ...MOCK_INCIDENT_STATS,
        openCount: 99,
        period: "LAST_7_DAYS",
      });

    const wrapper = createWrapper();
    const { result, rerender } = renderHook(
      ({ filter }: { filter: PeriodFilter }) => useDashboardStats(filter),
      { wrapper, initialProps: { filter: FILTER_30_DAYS } },
    );

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    rerender({ filter: FILTER_7_DAYS });

    await waitFor(() =>
      expect(result.current.data?.period).toBe("LAST_7_DAYS"),
    );
    expect(incidentStatsApi.getStats).toHaveBeenCalledTimes(2);
  });
});

describe("useUserStats", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return isLoading true when the API call is pending", () => {
    vi.mocked(userStatsApi.getUserStats).mockReturnValue(new Promise(() => {}));
    const { result } = renderHook(() => useUserStats(FILTER_30_DAYS), {
      wrapper: createWrapper(),
    });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeUndefined();
  });

  it("should return data when the API call resolves successfully", async () => {
    vi.mocked(userStatsApi.getUserStats).mockResolvedValueOnce(MOCK_USER_STATS);
    const { result } = renderHook(() => useUserStats(FILTER_30_DAYS), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(MOCK_USER_STATS);
    expect(result.current.isError).toBe(false);
  });

  it("should return isError true when the API call rejects", async () => {
    vi.mocked(userStatsApi.getUserStats).mockRejectedValueOnce(
      new Error("Forbidden"),
    );
    const { result } = renderHook(() => useUserStats(FILTER_30_DAYS), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });
});
