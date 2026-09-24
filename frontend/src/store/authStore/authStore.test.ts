// Tests frontend : verifie le comportement de authentification store.test.

import { describe, it, expect, beforeEach, vi } from "vitest";
import { useAuthStore, type AuthUser } from "./authStore";
import { tokenManager } from "../../utils/tokenManager/tokenManager";

vi.mock("../../utils/tokenManager/tokenManager", () => ({
  tokenManager: {
    setToken: vi.fn(),
    removeToken: vi.fn(),
  },
}));

const TEST_TOKEN = "mock-jwt-token";

const TEST_USER: AuthUser = {
  id: "user-1",
  username: "jdoe",
  roles: ["ADMIN", "EDITOR"],
  permissions: ["READ_USER", "CREATE_INCIDENT", "DELETE_INCIDENT"],
};

describe("authStore", () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, isAuthenticated: false });
    vi.clearAllMocks();
  });

  describe("initial state", () => {
    it("should have user as null by default", () => {
      expect(useAuthStore.getState().user).toBeNull();
    });

    it("should have isAuthenticated as false by default", () => {
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
    });
  });

  describe("login", () => {
    it("should set the user in state when login is called", () => {
      useAuthStore.getState().login(TEST_TOKEN, TEST_USER);
      expect(useAuthStore.getState().user).toEqual(TEST_USER);
    });

    it("should set isAuthenticated to true when login is called", () => {
      useAuthStore.getState().login(TEST_TOKEN, TEST_USER);
      expect(useAuthStore.getState().isAuthenticated).toBe(true);
    });

    it("should call tokenManager.setToken with the provided token when login is called", () => {
      useAuthStore.getState().login(TEST_TOKEN, TEST_USER);
      expect(tokenManager.setToken).toHaveBeenCalledWith(TEST_TOKEN);
    });
  });

  describe("logout", () => {
    it("should clear the user from state when logout is called", () => {
      useAuthStore.setState({ user: TEST_USER, isAuthenticated: true });
      useAuthStore.getState().logout();
      expect(useAuthStore.getState().user).toBeNull();
    });

    it("should set isAuthenticated to false when logout is called", () => {
      useAuthStore.setState({ user: TEST_USER, isAuthenticated: true });
      useAuthStore.getState().logout();
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
    });

    it("should call tokenManager.removeToken when logout is called", () => {
      useAuthStore.setState({ user: TEST_USER, isAuthenticated: true });
      useAuthStore.getState().logout();
      expect(tokenManager.removeToken).toHaveBeenCalled();
    });
  });

  describe("hasRole", () => {
    it("should return true when the user has the specified role", () => {
      useAuthStore.setState({ user: TEST_USER, isAuthenticated: true });
      expect(useAuthStore.getState().hasRole("ADMIN")).toBe(true);
    });

    it("should return true when the role check is case-insensitive", () => {
      useAuthStore.setState({ user: TEST_USER, isAuthenticated: true });
      expect(useAuthStore.getState().hasRole("admin")).toBe(true);
    });

    it("should return false when the user does not have the specified role", () => {
      useAuthStore.setState({ user: TEST_USER, isAuthenticated: true });
      expect(useAuthStore.getState().hasRole("SUPER_ADMIN")).toBe(false);
    });

    it("should return false when the user is null", () => {
      expect(useAuthStore.getState().hasRole("ADMIN")).toBe(false);
    });
  });

  describe("hasPermission", () => {
    it("should return true when the user has the specified permission", () => {
      useAuthStore.setState({ user: TEST_USER, isAuthenticated: true });
      expect(useAuthStore.getState().hasPermission("CREATE_INCIDENT")).toBe(
        true,
      );
    });

    it("should return true when the permission check is case-insensitive", () => {
      useAuthStore.setState({ user: TEST_USER, isAuthenticated: true });
      expect(useAuthStore.getState().hasPermission("create_incident")).toBe(
        true,
      );
    });

    it("should return false when the user does not have the specified permission", () => {
      useAuthStore.setState({ user: TEST_USER, isAuthenticated: true });
      expect(useAuthStore.getState().hasPermission("UPDATE_ANY")).toBe(false);
    });

    it("should return false when the user is null", () => {
      expect(useAuthStore.getState().hasPermission("CREATE_INCIDENT")).toBe(
        false,
      );
    });
  });
});
