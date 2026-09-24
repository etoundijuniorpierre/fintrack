// Tests frontend : verifie le comportement de role api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import apiClient from "../../client";
import { roleApi } from "./roleApi";
import { API_ROUTES } from "../../routes";
import type { RoleRequest } from "../types";
import { makeRoles, makeRole } from "../../../mocks";

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);
const mockPost = vi.mocked(apiClient.post);

describe("roleApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getAll", () => {
    it("should call GET on the roles all endpoint and return the list", async () => {
      const roles = makeRoles(2);
      mockGet.mockResolvedValueOnce({ data: roles });

      const result = await roleApi.getAll();

      expect(mockGet).toHaveBeenCalledWith(API_ROUTES.ROLES.ALL, {
        signal: undefined,
      });
      expect(result).toEqual(roles);
    });

    it("should throw when the API call rejects", async () => {
      mockGet.mockRejectedValueOnce(new Error("Network error"));
      await expect(roleApi.getAll()).rejects.toThrow("Network error");
    });
  });

  describe("getById", () => {
    it("should call GET on the role detail endpoint and return the role", async () => {
      const role = makeRole({ id: "role-1" });
      mockGet.mockResolvedValueOnce({ data: role });

      const result = await roleApi.getById("role-1");

      expect(mockGet).toHaveBeenCalledWith(`${API_ROUTES.ROLES.BASE}/role-1`, {
        signal: undefined,
      });
      expect(result).toEqual(role);
    });
  });

  describe("create", () => {
    it("should call POST on the roles base endpoint with the payload and return the created role", async () => {
      const payload: RoleRequest = {
        name: "MANAGER",
        description: "Manager role",
        isSystem: false,
        permissionIds: ["perm-1"],
      };
      const created = makeRole({ id: "role-3", name: "MANAGER" });
      mockPost.mockResolvedValueOnce({ data: created });

      const result = await roleApi.create(payload);

      expect(mockPost).toHaveBeenCalledWith(API_ROUTES.ROLES.BASE, payload);
      expect(result).toEqual(created);
    });
  });
});
