// Tests frontend : verifie le comportement de agence api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import apiClient from "../../client";
import { agencyApi } from "./agencyApi";
import { API_ROUTES } from "../../routes";
import { makeAgency, makeAgencies } from "../../../mocks";

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);

describe("agencyApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getAll", () => {
    it("should call GET on the agencies all endpoint and return the list", async () => {
      const agencies = makeAgencies(2);
      mockGet.mockResolvedValueOnce({ data: agencies });

      const result = await agencyApi.getAll();

      expect(mockGet).toHaveBeenCalledWith(API_ROUTES.AGENCIES.ALL, {
        signal: undefined,
      });
      expect(result).toEqual(agencies);
    });

    it("should throw when the API call rejects", async () => {
      mockGet.mockRejectedValueOnce(new Error("Network error"));
      await expect(agencyApi.getAll()).rejects.toThrow("Network error");
    });
  });

  describe("getById", () => {
    it("should call GET on the agency detail endpoint and return the agency", async () => {
      const agency = makeAgency({ id: "agency-1" });
      mockGet.mockResolvedValueOnce({ data: agency });

      const result = await agencyApi.getById("agency-1");

      expect(mockGet).toHaveBeenCalledWith(
        `${API_ROUTES.AGENCIES.BASE}/agency-1`,
        { signal: undefined },
      );
      expect(result).toEqual(agency);
    });
  });
});
