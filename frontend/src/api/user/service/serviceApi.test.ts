// Tests frontend : verifie le comportement de service api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import apiClient from "../../client";
import { serviceApi } from "./serviceApi";
import { API_ROUTES } from "../../routes";
import { makeService, makeServices } from "../../../mocks";

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);

describe("serviceApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getAll", () => {
    it("should call GET on the services all endpoint and return the list", async () => {
      const services = makeServices(2);
      mockGet.mockResolvedValueOnce({ data: services });

      const result = await serviceApi.getAll();

      expect(mockGet).toHaveBeenCalledWith(API_ROUTES.SERVICES.ALL, {
        signal: undefined,
      });
      expect(result).toEqual(services);
    });

    it("should throw when the API call rejects", async () => {
      mockGet.mockRejectedValueOnce(new Error("Network error"));
      await expect(serviceApi.getAll()).rejects.toThrow("Network error");
    });
  });

  describe("getById", () => {
    it("should call GET on the service detail endpoint and return the service", async () => {
      const service = makeService({ id: "service-1" });
      mockGet.mockResolvedValueOnce({ data: service });

      const result = await serviceApi.getById("service-1");

      expect(mockGet).toHaveBeenCalledWith(
        `${API_ROUTES.SERVICES.BASE}/service-1`,
        { signal: undefined },
      );
      expect(result).toEqual(service);
    });
  });
});
