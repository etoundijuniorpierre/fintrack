// Tests frontend : verifie le comportement de use users.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import {
  useUsers,
  useAssignableUsers,
  usePaginatedUsers,
  useCreateUser,
  useDeleteUser,
  useRegeneratePassword,
  useChangePassword,
  useDirectionValidators,
} from "./useUsers";
import { userApi } from "../../../api/user/userApi/userApi";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import { App } from "antd";
import { makeUsers, makeUser } from "../../../mocks";

vi.mock("../../../api/user/userApi/userApi", () => ({
  userApi: {
    getAll: vi.fn(),
    getAssignable: vi.fn(),
    getPaginated: vi.fn(),
    create: vi.fn(),
    delete: vi.fn(),
    toggleStatus: vi.fn(),
    regeneratePassword: vi.fn(),
    changePassword: vi.fn(),
    getValidators: vi.fn(),
  },
}));

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <App>
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </App>
  );
};

describe("useUsers hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("useUsers", () => {
    it("should return all users when the API call succeeds", async () => {
      const users = makeUsers(3);
      vi.mocked(userApi.getAll).mockResolvedValue(users);

      const { result } = renderHook(() => useUsers(), {
        wrapper: createWrapper(),
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toHaveLength(users.length);
    });

    it("should return users sorted by username", async () => {
      const unsortedUsers = [
        makeUser({ id: "user-1", username: "zebra" }),
        makeUser({ id: "user-2", username: "alpha" }),
        makeUser({ id: "user-3", username: "beta" }),
      ];
      vi.mocked(userApi.getAll).mockResolvedValue(unsortedUsers);

      const { result } = renderHook(() => useUsers(), {
        wrapper: createWrapper(),
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data?.[0].username).toBe("alpha");
      expect(result.current.data?.[1].username).toBe("beta");
      expect(result.current.data?.[2].username).toBe("zebra");
    });

    it("should return an empty array when the API returns no users", async () => {
      vi.mocked(userApi.getAll).mockResolvedValue([]);

      const { result } = renderHook(() => useUsers(), {
        wrapper: createWrapper(),
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual([]);
    });

    it("should expose isError when the API call rejects", async () => {
      vi.mocked(userApi.getAll).mockRejectedValue(new Error("Network error"));

      const { result } = renderHook(() => useUsers(), {
        wrapper: createWrapper(),
      });

      await waitFor(() => expect(result.current.isError).toBe(true));
    });
  });

  describe("useAssignableUsers", () => {
    it("should call getAssignable with the service scope and sort by username", async () => {
      const users = [
        { id: "user-1", username: "zebra", firstName: "Z", lastName: "A" },
        { id: "user-2", username: "alpha", firstName: "A", lastName: "B" },
      ];
      vi.mocked(userApi.getAssignable).mockResolvedValue(users);

      const { result } = renderHook(
        () => useAssignableUsers({ serviceId: "service-1" }),
        { wrapper: createWrapper() },
      );

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(userApi.getAssignable).toHaveBeenCalledWith(
        { serviceId: "service-1" },
        expect.any(AbortSignal),
      );
      expect(result.current.data?.[0].username).toBe("alpha");
      expect(result.current.data?.[1].username).toBe("zebra");
    });

    it("should not fetch when disabled", async () => {
      const { result } = renderHook(
        () => useAssignableUsers({ serviceId: "service-1" }, { enabled: false }),
        { wrapper: createWrapper() },
      );

      expect(result.current.fetchStatus).toBe("idle");
      expect(userApi.getAssignable).not.toHaveBeenCalled();
    });

    it("should return an empty array before data resolves", () => {
      vi.mocked(userApi.getAssignable).mockReturnValue(new Promise(() => {}));

      const { result } = renderHook(
        () => useAssignableUsers({ serviceId: "service-1" }),
        { wrapper: createWrapper() },
      );

      expect(result.current.data).toEqual([]);
    });
  });

  describe("usePaginatedUsers", () => {
    it("should call getPaginated with the correct parameters", async () => {
      const response = { content: makeUsers(2), totalElements: 2 };
      vi.mocked(userApi.getPaginated).mockResolvedValue(response);

      const params = { page: 0, size: 10, keyword: "test" };
      const { result } = renderHook(() => usePaginatedUsers(params), {
        wrapper: createWrapper(),
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(userApi.getPaginated).toHaveBeenCalledWith(
        params,
        expect.any(AbortSignal),
      );
      expect(result.current.data).toEqual(response);
    });
  });

  describe("useCreateUser", () => {
    it("should call userApi.create with the payload when mutate is called", async () => {
      const payload = {
        username: "newuser",
        email: "new@finstar.com",
        firstName: "New",
        lastName: "User",
        roleIds: ["role-1"],
      };
      vi.mocked(userApi.create).mockResolvedValue(makeUser());

      const { result } = renderHook(() => useCreateUser(), {
        wrapper: createWrapper(),
      });

      result.current.mutate(payload);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(userApi.create).toHaveBeenCalledWith(payload);
    });
  });

  describe("useDeleteUser", () => {
    it("should call userApi.delete with the user id when mutate is called", async () => {
      vi.mocked(userApi.delete).mockResolvedValue();

      const { result } = renderHook(() => useDeleteUser(), {
        wrapper: createWrapper(),
      });

      result.current.mutate("user-1");

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(userApi.delete).toHaveBeenCalledWith("user-1");
    });
  });

  describe("useRegeneratePassword", () => {
    it("should call userApi.regeneratePassword with the user id when mutate is called", async () => {
      vi.mocked(userApi.regeneratePassword).mockResolvedValue(makeUser());

      const { result } = renderHook(() => useRegeneratePassword(), {
        wrapper: createWrapper(),
      });

      result.current.mutate("user-1");

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(userApi.regeneratePassword).toHaveBeenCalledWith("user-1");
    });
  });

  describe("useChangePassword", () => {
    it("should call userApi.changePassword with the correct parameters", async () => {
      vi.mocked(userApi.changePassword).mockResolvedValue(
        makeUser({ id: "user-1" }),
      );

      const { result } = renderHook(() => useChangePassword(), {
        wrapper: createWrapper(),
      });

      const req = { currentPassword: "old", newPassword: "new" };
      result.current.mutate({ id: "user-1", data: req });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(userApi.changePassword).toHaveBeenCalledWith("user-1", req);
    });
  });

  describe("useDirectionValidators", () => {
    it("should call getValidators when query executes", async () => {
      const validators = [
        { id: "user-1", username: "validator1", firstName: "Val", lastName: "One" },
      ];
      vi.mocked(userApi.getValidators).mockResolvedValue(validators);

      const { result } = renderHook(() => useDirectionValidators(), {
        wrapper: createWrapper(),
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(userApi.getValidators).toHaveBeenCalled();
      expect(result.current.data).toEqual(validators);
    });
  });
});
