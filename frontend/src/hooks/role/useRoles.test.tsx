// Tests frontend : verifie le comportement de use roles.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import { useRoles } from "./useRoles";
import * as roleApi from "../../api/user/role/roleApi";
import { makeRoles } from "../../mocks";

vi.mock("../../api/user/role/roleApi", () => ({
  roleApi: {
    getAll: vi.fn(),
  },
}));

// Fabrique une fixture de test pour use roles.test.
const makeRoleApi = vi.mocked(roleApi.roleApi);

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

// Definit les donnees de test test roles.
const TEST_ROLES = makeRoles(2);

describe("useRoles", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return roles data when the API call succeeds", async () => {
    makeRoleApi.getAll.mockResolvedValue(TEST_ROLES);

    const { result } = renderHook(() => useRoles(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(TEST_ROLES);
    expect(result.current.isError).toBe(false);
  });

  it("should expose isError when the API call rejects", async () => {
    makeRoleApi.getAll.mockRejectedValue(new Error("Failed to fetch roles"));

    const { result } = renderHook(() => useRoles(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });

  it("should not call the API when disabled", () => {
    const { result } = renderHook(() => useRoles(false), {
      wrapper: createWrapper(),
    });

    expect(makeRoleApi.getAll).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});
