// Tests frontend : verifie le comportement de use tableau de bord metrics.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import { useDashboardMetrics } from "./useDashboardMetrics";
import { incidentStatsApi } from "../../../api/incident/incidentStatsApi/incidentStatsApi";
import type { DashboardMetricsResponse } from "../../../types/dashboard";

vi.mock("../../../api/incident/incidentStatsApi/incidentStatsApi", () => ({
  incidentStatsApi: {
    getDashboardMetrics: vi.fn(),
  },
}));

const mockIncidentStatsApi = vi.mocked(incidentStatsApi);

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

const TEST_METRICS: Partial<DashboardMetricsResponse> = {
  activeIncidents: 10,
  closedIncidents: 5,
};

describe("useDashboardMetrics", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return metrics data when the API call succeeds", async () => {
    mockIncidentStatsApi.getDashboardMetrics.mockResolvedValue(
      TEST_METRICS as DashboardMetricsResponse,
    );

    const { result } = renderHook(
      () => useDashboardMetrics("own", { period: "LAST_30_DAYS" }),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(TEST_METRICS);
    expect(result.current.isError).toBe(false);
    expect(mockIncidentStatsApi.getDashboardMetrics).toHaveBeenCalledWith(
      "own",
      { period: "LAST_30_DAYS" },
      expect.any(AbortSignal),
    );
  });

  it("should expose isError when the API call rejects", async () => {
    mockIncidentStatsApi.getDashboardMetrics.mockRejectedValue(
      new Error("Failed to fetch metrics"),
    );

    const { result } = renderHook(() => useDashboardMetrics("own"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });

  it("should not call the API for an incomplete custom period", () => {
    const { result } = renderHook(
      () =>
        useDashboardMetrics("own", {
          period: "CUSTOM",
          dateFrom: "2026-06-01T00:00:00.000Z",
        }),
      { wrapper: createWrapper() },
    );

    expect(result.current.fetchStatus).toBe("idle");
    expect(mockIncidentStatsApi.getDashboardMetrics).not.toHaveBeenCalled();
  });
});
