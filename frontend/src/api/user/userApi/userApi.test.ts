// Tests frontend : verifie le comportement de utilisateur api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import apiClient from "../../client";
import { userApi } from "./userApi";
import { API_ROUTES } from "../../routes";
import { makeUser, makeUsers } from "../../../mocks";
import type { CreateUserRequest } from "../types";

vi.mock("../../client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  },
}));

const mockGet = vi.mocked(apiClient.get);
const mockPost = vi.mocked(apiClient.post);
const mockPut = vi.mocked(apiClient.put);
const mockDelete = vi.mocked(apiClient.delete);
const mockPatch = vi.mocked(apiClient.patch);

describe("userApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getAll", () => {
    it("should call GET on the users all endpoint and return the list", async () => {
      const users = makeUsers(3);
      mockGet.mockResolvedValueOnce({ data: users });

      const result = await userApi.getAll();

      expect(mockGet).toHaveBeenCalledWith(
        API_ROUTES.USERS.ALL,
        expect.anything(),
      );
      expect(result).toEqual(users);
    });

    it("should return an empty array when the backend returns an empty list", async () => {
      mockGet.mockResolvedValueOnce({ data: [] });
      expect(await userApi.getAll()).toEqual([]);
    });

    it("should pass the AbortSignal to the request", async () => {
      const controller = new AbortController();
      mockGet.mockResolvedValueOnce({ data: [] });

      await userApi.getAll(controller.signal);

      expect(mockGet).toHaveBeenCalledWith(API_ROUTES.USERS.ALL, {
        signal: controller.signal,
      });
    });

    it("should throw when the API call rejects", async () => {
      mockGet.mockRejectedValueOnce(new Error("Network error"));
      await expect(userApi.getAll()).rejects.toThrow("Network error");
    });
  });

  describe("getAssignable", () => {
    it("should call GET on the assignable endpoint with the service scope", async () => {
      const users = makeUsers(2);
      mockGet.mockResolvedValueOnce({ data: users });

      const result = await userApi.getAssignable({ serviceId: "service-1" });

      expect(mockGet).toHaveBeenCalledWith(API_ROUTES.USERS.ASSIGNABLE, {
        params: { serviceId: "service-1" },
        signal: undefined,
      });
      expect(result).toEqual(users);
    });

    it("should pass the AbortSignal and omit params when none are given", async () => {
      const controller = new AbortController();
      mockGet.mockResolvedValueOnce({ data: [] });

      await userApi.getAssignable(undefined, controller.signal);

      expect(mockGet).toHaveBeenCalledWith(API_ROUTES.USERS.ASSIGNABLE, {
        params: undefined,
        signal: controller.signal,
      });
    });
  });

  describe("getById", () => {
    it("should call GET on the user detail endpoint and return the user", async () => {
      const user = makeUser({ id: "user-1" });
      mockGet.mockResolvedValueOnce({ data: user });

      const result = await userApi.getById("user-1");

      expect(mockGet).toHaveBeenCalledWith(
        `${API_ROUTES.USERS.BASE}/user-1`,
        expect.anything(),
      );
      expect(result).toEqual(user);
    });
  });

  describe("getPaginated", () => {
    it("should call GET on the users base endpoint with params and signal", async () => {
      const controller = new AbortController();
      const response = { content: makeUsers(2), totalElements: 2 };
      mockGet.mockResolvedValueOnce({ data: response });

      const result = await userApi.getPaginated(
        { page: 1, size: 20 },
        controller.signal,
      );

      expect(mockGet).toHaveBeenCalledWith(API_ROUTES.USERS.BASE, {
        params: { page: 1, size: 20 },
        signal: controller.signal,
      });
      expect(result).toEqual(response);
    });
  });

  describe("create", () => {
    it("should call POST on the users base endpoint with the payload and return the created user", async () => {
      const payload: CreateUserRequest = {
        username: "newuser",
        email: "new@finstar.com",
        firstName: "New",
        lastName: "User",
        roleIds: ["role-1"],
      };
      const created = makeUser({ id: "user-3", username: "newuser" });
      mockPost.mockResolvedValueOnce({ data: created });

      const result = await userApi.create(payload);

      expect(mockPost).toHaveBeenCalledWith(API_ROUTES.USERS.BASE, payload);
      expect(result.username).toBe("newuser");
    });
  });

  describe("update", () => {
    it("should call PUT on the user detail endpoint with the partial payload and return the updated user", async () => {
      const updated = makeUser({ id: "user-1", firstName: "Updated" });
      mockPut.mockResolvedValueOnce({ data: updated });

      const result = await userApi.update("user-1", { firstName: "Updated" });

      expect(mockPut).toHaveBeenCalledWith(`${API_ROUTES.USERS.BASE}/user-1`, {
        firstName: "Updated",
      });
      expect(result.firstName).toBe("Updated");
    });
  });

  describe("delete", () => {
    it("should call DELETE on the user detail endpoint", async () => {
      mockDelete.mockResolvedValueOnce({});

      await userApi.delete("user-1");

      expect(mockDelete).toHaveBeenCalledWith(
        `${API_ROUTES.USERS.BASE}/user-1`,
      );
    });
  });

  describe("toggleStatus", () => {
    it("should call PATCH on the user status endpoint and return the updated user", async () => {
      const toggled = makeUser({ id: "user-1", isActive: false });
      mockPatch.mockResolvedValueOnce({ data: toggled });

      const result = await userApi.toggleStatus("user-1");

      expect(mockPatch).toHaveBeenCalledWith(
        `${API_ROUTES.USERS.BASE}/user-1/status`,
      );
      expect(result.isActive).toBe(false);
    });
  });

  describe("regeneratePassword", () => {
    it("should call POST on the regenerate-password endpoint and return the user", async () => {
      const user = makeUser({ id: "user-1" });
      mockPost.mockResolvedValueOnce({ data: user });

      const result = await userApi.regeneratePassword("user-1");

      expect(mockPost).toHaveBeenCalledWith(
        `${API_ROUTES.USERS.BASE}/user-1/regenerate-password`,
      );
      expect(result).toEqual(user);
    });
  });

  describe("changePassword", () => {
    it("should call POST on the change-password endpoint with the request payload", async () => {
      const user = makeUser({ id: "user-1" });
      const request = { currentPassword: "old", newPassword: "new" };
      mockPost.mockResolvedValueOnce({ data: user });

      const result = await userApi.changePassword("user-1", request);

      expect(mockPost).toHaveBeenCalledWith(
        `${API_ROUTES.USERS.BASE}/user-1/change-password`,
        request,
      );
      expect(result).toEqual(user);
    });
  });
});
