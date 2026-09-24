// Tests frontend : verifie le comportement de use services.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import { useServices } from "./useServices";
import * as serviceApi from "../../api/user/service/serviceApi";
import { makeServices } from "../../mocks";

vi.mock("../../api/user/service/serviceApi", () => ({
  serviceApi: {
    getAll: vi.fn(),
  },
}));

// Fabrique une fixture de test pour use services.test.
const makeServiceApi = vi.mocked(serviceApi.serviceApi);

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

// Definit les donnees de test test services.
const TEST_SERVICES = makeServices(2);

describe("useServices", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return services data when the API call succeeds", async () => {
    makeServiceApi.getAll.mockResolvedValue(TEST_SERVICES);

    const { result } = renderHook(() => useServices(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(TEST_SERVICES);
    expect(result.current.isError).toBe(false);
  });

  it("should expose isError when the API call rejects", async () => {
    makeServiceApi.getAll.mockRejectedValue(
      new Error("Failed to fetch services"),
    );

    const { result } = renderHook(() => useServices(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.data).toBeUndefined();
  });

  it("should not call the API when disabled", () => {
    renderHook(() => useServices({ enabled: false }), {
      wrapper: createWrapper(),
    });

    expect(makeServiceApi.getAll).not.toHaveBeenCalled();
  });
});
