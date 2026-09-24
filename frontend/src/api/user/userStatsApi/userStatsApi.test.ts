// Tests frontend : verifie le comportement de utilisateur statistiques api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { userStatsApi } from "./userStatsApi";
import apiClient from "../../client";
import { USER_SERVICE_ENDPOINTS } from "../endpoints/endpoints";
import type { UserStatsResponse } from "../../../types/dashboard";

vi.mock("../../client", () => ({
  default: { get: vi.fn() },
}));

const mockGet = vi.mocked(apiClient.get);

const MOCK_RESPONSE: UserStatsResponse = {
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

describe("userStatsApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("calls the correct endpoint with period param", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });

    await userStatsApi.getUserStats({ period: "LAST_30_DAYS" });

    expect(mockGet).toHaveBeenCalledWith(USER_SERVICE_ENDPOINTS.USERS.STATS, {
      params: { period: "LAST_30_DAYS" },
      signal: undefined,
    });
  });

  it("returns the response data", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });

    const result = await userStatsApi.getUserStats({ period: "ALL" });

    expect(result).toEqual(MOCK_RESPONSE);
  });

  it("passes signal to the API client", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });
    const signal = new AbortController().signal;

    await userStatsApi.getUserStats({ period: "TODAY" }, signal);

    expect(mockGet).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({ signal }),
    );
  });

  it("includes dateFrom and dateTo for CUSTOM period", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });

    await userStatsApi.getUserStats({
      period: "CUSTOM",
      dateFrom: "2024-01-01T00:00:00.000Z",
      dateTo: "2024-03-31T23:59:59.000Z",
    });

    expect(mockGet).toHaveBeenCalledWith(USER_SERVICE_ENDPOINTS.USERS.STATS, {
      params: {
        period: "CUSTOM",
        dateFrom: "2024-01-01T00:00:00.000Z",
        dateTo: "2024-03-31T23:59:59.000Z",
      },
      signal: undefined,
    });
  });

  it("does not include dateFrom/dateTo for non-CUSTOM periods", async () => {
    mockGet.mockResolvedValueOnce({ data: MOCK_RESPONSE });

    await userStatsApi.getUserStats({
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
    await expect(userStatsApi.getUserStats({ period: "ALL" })).rejects.toThrow(
      "Network error",
    );
  });
});
