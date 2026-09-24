// Tests frontend : verifie le comportement de use auth.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { useLogin, useLogout } from "./useAuth";
import { authApi } from "../../api/user/auth/authApi";
import { useAuthStore } from "../../store/authStore/authStore";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import type { AuthResponse } from "../../api/user/types";
vi.mock("../../api/user/auth/authApi", () => ({
  authApi: {
    login: vi.fn(),
    logout: vi.fn(),
  },
}));

import { MemoryRouter } from "react-router-dom";
import { App } from "antd";

// Couvre les comportements du module teste.
// Renvoie aussi le client pour pouvoir inspecter le cache apres coup.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <App>
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          {children}
        </QueryClientProvider>
      </MemoryRouter>
    </App>
  );
  wrapper.queryClient = queryClient;
  return wrapper;
};

describe("useAuth", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().logout();
  });

  describe("useLogin", () => {
    it("should login successfully and update the store", async () => {
      const mockResponse: AuthResponse = {
        token: "fake-token",
        id: "user-1",
        username: "testuser",
        roles: ["ROLE_ADMIN"],
        permissions: ["READ"],
        isFirstLogin: false,
        isActive: true,
      };

      vi.mocked(authApi.login).mockResolvedValue(mockResponse);

      const { result } = renderHook(() => useLogin(), {
        wrapper: createWrapper(),
      });

      result.current.mutate({ username: "testuser", password: "password" });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      const state = useAuthStore.getState();
      expect(state.isAuthenticated).toBe(true);
      expect(state.user?.username).toBe("testuser");
      expect(state.user?.roles).toContain("ROLE_ADMIN");
    });

    it("should set sessionStorage firstLogin flag when isFirstLogin is true", async () => {
      const mockResponse: AuthResponse = {
        token: "fake-token",
        id: "user-1",
        username: "testuser",
        roles: ["ROLE_AGENT"],
        permissions: [],
        isFirstLogin: true,
        isActive: false,
      };

      vi.mocked(authApi.login).mockResolvedValue(mockResponse);
      sessionStorage.removeItem("firstLogin");

      const { result } = renderHook(() => useLogin(), {
        wrapper: createWrapper(),
      });

      result.current.mutate({ username: "testuser", password: "temppass" });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(sessionStorage.getItem("firstLogin")).toBe("true");
    });

    it("should also enforce first login when a legacy account is still active", async () => {
      const mockResponse: AuthResponse = {
        token: "fake-token",
        id: "user-1",
        username: "testuser",
        roles: ["ROLE_AGENT"],
        permissions: [],
        isFirstLogin: true,
        isActive: true,
      };

      vi.mocked(authApi.login).mockResolvedValue(mockResponse);
      sessionStorage.removeItem("firstLogin");

      const { result } = renderHook(() => useLogin(), {
        wrapper: createWrapper(),
      });

      result.current.mutate({ username: "testuser", password: "password" });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(sessionStorage.getItem("firstLogin")).toBe("true");
      expect(useAuthStore.getState().user?.isFirstLogin).toBe(true);
    });

    it("should NOT set sessionStorage firstLogin flag when isFirstLogin is false", async () => {
      const mockResponse: AuthResponse = {
        token: "fake-token",
        id: "user-1",
        username: "testuser",
        roles: ["ROLE_AGENT"],
        permissions: [],
        isFirstLogin: false,
        isActive: true,
      };

      vi.mocked(authApi.login).mockResolvedValue(mockResponse);
      sessionStorage.removeItem("firstLogin");

      const { result } = renderHook(() => useLogin(), {
        wrapper: createWrapper(),
      });

      result.current.mutate({ username: "testuser", password: "password" });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(sessionStorage.getItem("firstLogin")).toBeNull();
    });

    it("should handle login error", async () => {
      vi.mocked(authApi.login).mockRejectedValue(
        new Error("Invalid credentials"),
      );

      const { result } = renderHook(() => useLogin(), {
        wrapper: createWrapper(),
      });

      result.current.mutate({ username: "wrong", password: "wrong" });

      await waitFor(() => expect(result.current.isError).toBe(true));
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
    });
  });

  describe("useLogout", () => {
    it("should logout, clear the store, and remove sessionStorage firstLogin flag", async () => {
      useAuthStore.getState().login("token", {
        id: "user-1",
        username: "u",
        roles: [],
        permissions: [],
      });
      sessionStorage.setItem("firstLogin", "true");

      vi.mocked(authApi.logout).mockResolvedValue(undefined);

      const { result } = renderHook(() => useLogout(), {
        wrapper: createWrapper(),
      });

      result.current.mutate();

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(useAuthStore.getState().isAuthenticated).toBe(false);
      expect(useAuthStore.getState().user).toBeNull();
      expect(sessionStorage.getItem("firstLogin")).toBeNull();
    });

    it("should drop the server cache so the next session starts clean", async () => {
      useAuthStore.getState().login("token", {
        id: "user-1",
        username: "u",
        roles: [],
        permissions: [],
      });
      vi.mocked(authApi.logout).mockResolvedValue(undefined);

      const wrapper = createWrapper();
      // Donnees de la session en cours : sans purge, elles survivraient a la
      // deconnexion et s'afficheraient au prochain utilisateur.
      wrapper.queryClient.setQueryData(["incidents"], [{ id: "inc-1" }]);

      const { result } = renderHook(() => useLogout(), { wrapper });
      result.current.mutate();

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(wrapper.queryClient.getQueryData(["incidents"])).toBeUndefined();
      expect(wrapper.queryClient.getQueryCache().getAll()).toHaveLength(0);
    });
  });
});
