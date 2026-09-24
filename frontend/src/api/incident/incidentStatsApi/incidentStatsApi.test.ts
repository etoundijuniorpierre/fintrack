// Tests frontend : verifie le comportement de incident statistiques api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { incidentStatsApi } from "./incidentStatsApi";
import apiClient from "../../client";
import { INCIDENT_SERVICE_ENDPOINTS } from "../endpoints/endpoints";
import type { IncidentStatsResponse } from "../../../types/dashboard";

vi.mock("../../client", () => ({
  default: { get: vi.fn() },
}));

const mockGet = vi.mocked(apiClient.get);

const MOCK_RESPONSE: IncidentStatsResponse = {
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

const MOCK_DASHBOARD_RESPONSE = {
  activeIncidents: 1,
  closedIncidents: 3,
  rejectedIncidents: 2,
  blockedIncidents: 0,
  totalIncidents: 6,
  avgClosureHours: 12.5,
  avgResolutionHours: 10.5,
  distributionByType: {},
  distributionByCriticality: {},
  recentActivities: [],
  assignedToMe: 0,
};

describe("incidentStatsApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("calls the correct endpoint with period param", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });

    await incidentStatsApi.getStats({ period: "LAST_30_DAYS" });

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.STATS,
      { params: { period: "LAST_30_DAYS" }, signal: undefined },
    );
  });

  it("returns the response data", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });

    const result = await incidentStatsApi.getStats({ period: "ALL" });

    expect(result).toEqual(MOCK_RESPONSE);
  });

  it("passes signal to the API client", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });
    const signal = new AbortController().signal;

    await incidentStatsApi.getStats({ period: "TODAY" }, signal);

    expect(mockGet).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({ signal }),
    );
  });

  it("includes dateFrom and dateTo for CUSTOM period", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });

    await incidentStatsApi.getStats({
      period: "CUSTOM",
      dateFrom: "2024-01-01T00:00:00.000Z",
      dateTo: "2024-03-31T23:59:59.000Z",
    });

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.INCIDENTS.STATS,
      {
        params: {
          period: "CUSTOM",
          dateFrom: "2024-01-01T00:00:00.000Z",
          dateTo: "2024-03-31T23:59:59.000Z",
        },
        signal: undefined,
      },
    );
  });

  it("does not include dateFrom/dateTo for non-CUSTOM periods", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });

    await incidentStatsApi.getStats({
      period: "LAST_7_DAYS",
      dateFrom: "2024-01-01T00:00:00.000Z",
      dateTo: "2024-01-07T23:59:59.000Z",
    });

    const calledParams = mockGet.mock.calls[0][1] as {
      params: Record<string, string>;
    };
    expect(calledParams.params).not.toHaveProperty("dateFrom");
    expect(calledParams.params).not.toHaveProperty("dateTo");
  });

  it("should throw when the API call rejects", async () => {
    mockGet.mockRejectedValueOnce(new Error("Network error"));
    await expect(incidentStatsApi.getStats({ period: "ALL" })).rejects.toThrow(
      "Network error",
    );
  });

  it("gets dashboard metrics through centralized endpoint", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_DASHBOARD_RESPONSE });

    const result = await incidentStatsApi.getDashboardMetrics("all");

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.DASHBOARD.METRICS,
      { params: { view: "all" }, signal: undefined },
    );
    expect(result).toEqual(MOCK_DASHBOARD_RESPONSE);
  });

  it("gets comparison metrics for several entities of the same type", async () => {
    mockGet.mockResolvedValueOnce({
      data: { entityType: "SERVICE", entries: [] },
    });

    await incidentStatsApi.getComparison("SERVICE", ["s1", "s2"], {
      period: "LAST_30_DAYS",
    });

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.DASHBOARD.COMPARISON,
      expect.objectContaining({
        params: expect.objectContaining({
          entityType: "SERVICE",
          ids: ["s1", "s2"],
          period: "LAST_30_DAYS",
        }),
      }),
    );
  });

  it("gets dashboard synthesis metrics", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_DASHBOARD_RESPONSE });

    const result = await incidentStatsApi.getSynthesisMetrics({ year: 2026 });

    expect(mockGet).toHaveBeenCalledWith(
      INCIDENT_SERVICE_ENDPOINTS.DASHBOARD.SYNTHESIS,
      { params: { year: 2026 }, signal: undefined },
    );
    expect(result).toEqual(MOCK_DASHBOARD_RESPONSE);
  });
});
