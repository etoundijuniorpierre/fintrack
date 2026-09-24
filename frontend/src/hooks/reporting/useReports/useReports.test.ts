// Tests : hooks de generation/gestion des rapports (fetch, generate, delete, download).

import { renderHook, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import {
  useReports,
  useGenerateReport,
  useDeleteReport,
  useDownloadReport,
} from "./useReports";
import apiClient from "../../../api/client";

vi.mock("antd", async () => {
  const actual = await vi.importActual<typeof import("antd")>("antd");
  return {
    ...actual,
    App: {
      ...actual.App,
      useApp: () => ({ message: { success: vi.fn(), error: vi.fn() } }),
    },
  };
});

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../api/client", () => ({
  default: { get: vi.fn(), post: vi.fn(), delete: vi.fn() },
}));

const api = vi.mocked(apiClient);

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) =>
    React.createElement(QueryClientProvider, { client: queryClient }, children);
};

describe("useReports", () => {
  beforeEach(() => vi.clearAllMocks());

  it('should load the report page without a scope param for "all"', async () => {
    const page = { content: [{ id: "r1", status: "READY" }] };
    api.get.mockResolvedValue({ data: page });

    const { result } = renderHook(() => useReports("all"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(page);
    expect(api.get).toHaveBeenCalledWith(expect.any(String), { params: {} });
  });

  it("should pass the scope as a query param when it is specific", async () => {
    api.get.mockResolvedValue({ data: { content: [] } });

    const { result } = renderHook(() => useReports("agency"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(api.get).toHaveBeenCalledWith(expect.any(String), {
      params: { scope: "agency" },
    });
  });
});

describe("useGenerateReport", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should post the generation request and return the created report", async () => {
    const created = { id: "r-new", status: "PENDING" };
    api.post.mockResolvedValue({ data: created });

    const { result } = renderHook(() => useGenerateReport(), {
      wrapper: createWrapper(),
    });
    act(() => result.current.mutate({ scope: "all" } as never));

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(api.post).toHaveBeenCalled();
    expect(result.current.data).toEqual(created);
  });
});

describe("useDeleteReport", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should delete the report by its id", async () => {
    api.delete.mockResolvedValue({ data: undefined });

    const { result } = renderHook(() => useDeleteReport(), {
      wrapper: createWrapper(),
    });
    act(() => result.current.mutate("r-9"));

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(api.delete).toHaveBeenCalledWith(expect.stringContaining("r-9"));
  });
});

describe("useDownloadReport", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should download the report as a blob with fetch", async () => {
    const blob = new Blob(["pdf"]);
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      blob: () => Promise.resolve(blob),
    });
    vi.stubGlobal("fetch", mockFetch);

    const { result } = renderHook(() => useDownloadReport(), {
      wrapper: createWrapper(),
    });
    act(() => result.current.mutate("r-3"));

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining("r-3"),
      expect.any(Object),
    );
    expect(result.current.data).toBe(blob);

    vi.unstubAllGlobals();
  });
});
