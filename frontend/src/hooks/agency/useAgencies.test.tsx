// Tests frontend : verifie le comportement de use agencies.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useAgencies } from "./useAgencies";
import * as agencyApi from "../../api/user/agency/agencyApi";
import { makeAgencies } from "../../mocks";

vi.mock("../../api/user/agency/agencyApi", () => ({
  agencyApi: {
    getAll: vi.fn(),
  },
}));

// Fabrique une fixture de test pour use agencies.test.
const makeAgencyApi = vi.mocked(agencyApi.agencyApi);

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

// Definit les donnees de test test agencies.
const TEST_AGENCIES = makeAgencies(2);

describe("useAgencies", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return agencies data when the API call succeeds", async () => {
    makeAgencyApi.getAll.mockResolvedValue(TEST_AGENCIES);

    const { result } = renderHook(() => useAgencies(), {
      wrapper: createWrapper(),
    });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.data).toEqual(TEST_AGENCIES);
      expect(result.current.isError).toBe(false);
    });
  });

  it("should expose isError when the API call rejects", async () => {
    makeAgencyApi.getAll.mockRejectedValue(
      new Error("Failed to fetch agencies"),
    );

    const { result } = renderHook(() => useAgencies(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
      expect(result.current.isError).toBe(true);
      expect(result.current.data).toBeUndefined();
    });
  });
});
