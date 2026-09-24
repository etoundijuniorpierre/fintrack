// Tests frontend : verifie le comportement de permission api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import apiClient from "../../client";
import { permissionApi } from "./permissionApi";
import { API_ROUTES } from "../../routes";
import { makePermission } from "../../../mocks";

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);

describe("permissionApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getAll", () => {
    it("should call GET on the permissions all endpoint and return the list", async () => {
      const permissions = [
        makePermission({ id: "perm-1", name: "USER_VIEW_ALL" }),
        makePermission({ id: "perm-2", name: "INCIDENT_CREATE" }),
      ];
      mockGet.mockResolvedValueOnce({ data: permissions });

      const result = await permissionApi.getAll();

      expect(mockGet).toHaveBeenCalledWith(
        API_ROUTES.PERMISSIONS.ALL,
        expect.anything(),
      );
      expect(result).toEqual(permissions);
    });

    it("should return an empty array when the backend returns an empty list", async () => {
      mockGet.mockResolvedValueOnce({ data: [] });

      const result = await permissionApi.getAll();

      expect(result).toEqual([]);
    });

    it("should pass the AbortSignal to the request", async () => {
      const controller = new AbortController();
      mockGet.mockResolvedValueOnce({ data: [] });

      await permissionApi.getAll(controller.signal);

      expect(mockGet).toHaveBeenCalledWith(API_ROUTES.PERMISSIONS.ALL, {
        signal: controller.signal,
      });
    });

    it("should throw when the API call rejects", async () => {
      mockGet.mockRejectedValueOnce(new Error("Network error"));
      await expect(permissionApi.getAll()).rejects.toThrow("Network error");
    });
  });

  describe("getById", () => {
    it("should call GET on the permission detail endpoint and return the permission", async () => {
      const permission = makePermission({ id: "perm-1" });
      mockGet.mockResolvedValueOnce({ data: permission });

      const result = await permissionApi.getById("perm-1");

      expect(mockGet).toHaveBeenCalledWith(
        `${API_ROUTES.PERMISSIONS.BASE}/perm-1`,
        expect.anything(),
      );
      expect(result).toEqual(permission);
    });
  });
});
