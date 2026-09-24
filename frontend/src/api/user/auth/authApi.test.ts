// Tests frontend : verifie le comportement de authentification api.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import apiClient from "../../client";
import { authApi } from "./authApi";
import { API_ROUTES } from "../../routes";
import type { LoginCredentials } from "../types";

vi.mock("../../client", () => ({
  default: {
    post: vi.fn(),
  },
}));

const mockPost = vi.mocked(apiClient.post);

const TEST_CREDENTIALS: LoginCredentials = {
  username: "jdoe",
  password: "Password1@",
};

const TEST_AUTH_RESPONSE = {
  token: "mock-jwt-token",
  id: "user-1",
  username: "jdoe",
  roles: ["ADMIN"],
  permissions: ["READ_USER", "CREATE_INCIDENT"],
};

describe("authApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("login", () => {
    it("should call POST on the login endpoint with credentials and return the auth response", async () => {
      mockPost.mockResolvedValueOnce({ data: TEST_AUTH_RESPONSE });

      const result = await authApi.login(TEST_CREDENTIALS);

      expect(mockPost).toHaveBeenCalledWith(
        API_ROUTES.AUTH.LOGIN,
        TEST_CREDENTIALS,
      );
      expect(result).toEqual(TEST_AUTH_RESPONSE);
    });

    it("should throw when the API call rejects", async () => {
      mockPost.mockRejectedValueOnce(new Error("Invalid credentials"));
      await expect(authApi.login(TEST_CREDENTIALS)).rejects.toThrow(
        "Invalid credentials",
      );
    });
  });

  describe("logout", () => {
    it("should call POST on the logout endpoint", async () => {
      mockPost.mockResolvedValueOnce({});

      await authApi.logout();

      expect(mockPost).toHaveBeenCalledWith(API_ROUTES.AUTH.LOGOUT);
    });
  });

  describe("refreshToken", () => {
    it("should call POST on the refresh endpoint and return the auth response", async () => {
      mockPost.mockResolvedValueOnce({ data: TEST_AUTH_RESPONSE });

      const result = await authApi.refreshToken();

      expect(mockPost).toHaveBeenCalledWith(API_ROUTES.AUTH.REFRESH);
      expect(result).toEqual(TEST_AUTH_RESPONSE);
    });
  });

  describe("changePassword", () => {
    it("should call POST on the change password endpoint with the provided payload", async () => {
      const payload = {
        currentPassword: "OldPass1@",
        newPassword: "NewPass1@",
      };
      mockPost.mockResolvedValueOnce({});

      await authApi.changePassword("user-1", payload);

      expect(mockPost).toHaveBeenCalledWith(
        API_ROUTES.AUTH.CHANGE_PASSWORD("user-1"),
        payload,
      );
    });

    it("should call POST without currentPassword when it is a first login", async () => {
      const payload = { newPassword: "NewPass1@" };
      mockPost.mockResolvedValueOnce({});

      await authApi.changePassword("user-1", payload);

      expect(mockPost).toHaveBeenCalledWith(
        API_ROUTES.AUTH.CHANGE_PASSWORD("user-1"),
        payload,
      );
    });
  });
});
