// Tests frontend : verifie le comportement de super-administration api.test.

import { describe, expect, it, vi, beforeEach } from "vitest";
import apiClient from "../client";
import { SERVICE_BASES } from "../base";
import { superAdminApi } from "./superAdminApi";

vi.mock("../client", () => ({
  default: {
    get: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);

describe("superAdminApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("getOverview calls the reporting-service Super Admin overview endpoint", async () => {
    const overview = {
      governance: { totalIncidents: 3 },
      systemHealth: [
        {
          key: "reporting",
          label: "Reporting service",
          status: "UP",
          endpoint: "local",
        },
      ],
      audit: {},
      permissions: {},
      systemConfig: {},
      dataQuality: {},
      reporting: {},
      notifications: {},
      meta: {},
    };
    mockGet.mockResolvedValueOnce({ data: overview });

    const result = await superAdminApi.getOverview();

    expect(mockGet).toHaveBeenCalledWith(
      `${SERVICE_BASES.REPORTING}/super-admin/overview`,
      { signal: undefined },
    );
    expect(result).toEqual(overview);
  });

  it("getAllServiceHealth keeps a DOWN row when a healthcheck fails", async () => {
    mockGet.mockResolvedValueOnce({ data: { status: "UP" } });
    mockGet.mockRejectedValue(new Error("down"));

    const result = await superAdminApi.getAllServiceHealth();

    expect(result[0]).toMatchObject({ key: "user", status: "UP" });
    expect(
      result.some((item) => item.key === "incident" && item.status === "DOWN"),
    ).toBe(true);
  });
});
