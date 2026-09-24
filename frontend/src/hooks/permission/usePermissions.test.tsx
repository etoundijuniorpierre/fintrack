// Tests frontend : verifie le comportement de use permissions.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import { usePermissions } from "./usePermissions";
import * as permissionApiModule from "../../api/user/permissionApi/permissionApi";
import { makePermission } from "../../mocks";

vi.mock("../../api/user/permissionApi/permissionApi", () => ({
  permissionApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
  },
}));

// Fabrique une fixture de test pour use permissions.test.
const makePermissionApi = vi.mocked(permissionApiModule.permissionApi);

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

// Definit les donnees de test test permissions.
const TEST_PERMISSIONS = [
  makePermission({ id: "perm-1", name: "USER_VIEW_ALL" }),
  makePermission({ id: "perm-2", name: "INCIDENT_CREATE" }),
];

describe("usePermissions", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should return permissions data when the API call succeeds", async () => {
    makePermissionApi.getAll.mockResolvedValue(TEST_PERMISSIONS);

    const { result } = renderHook(() => usePermissions(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(TEST_PERMISSIONS);
    expect(result.current.isError).toBe(false);
  });

  it("should expose isError when the API call rejects", async () => {
    makePermissionApi.getAll.mockRejectedValue(new Error("Network error"));

    const { result } = renderHook(() => usePermissions(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });

  it("should not call the API when disabled", () => {
    const { result } = renderHook(() => usePermissions(false), {
      wrapper: createWrapper(),
    });

    expect(makePermissionApi.getAll).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });
});
