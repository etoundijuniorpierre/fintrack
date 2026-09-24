// Tests : hooks de configuration (tri des listes, requete conditionnelle, mutation create).

import { renderHook, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import {
  useSettingsIncidentTypes,
  useIncidentType,
  useCreateIncidentType,
} from "./useSettings";
import { settingsApi } from "../../../api/settings/settingsApi/settingsApi";

const messageSuccess = vi.fn();
const messageError = vi.fn();

vi.mock("antd", async () => {
  const actual = await vi.importActual<typeof import("antd")>("antd");
  return {
    ...actual,
    App: {
      ...actual.App,
      useApp: () => ({
        message: { success: messageSuccess, error: messageError },
      }),
    },
  };
});

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../api/settings/settingsApi/settingsApi", () => ({
  settingsApi: {
    getIncidentTypes: vi.fn(),
    getIncidentType: vi.fn(),
    createIncidentType: vi.fn(),
  },
}));

const api = vi.mocked(settingsApi);

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

describe("useSettingsIncidentTypes", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should sort incident types by most recent change", async () => {
    api.getIncidentTypes.mockResolvedValue([
      { id: "old", updatedAt: "2024-01-01T00:00:00Z" },
      { id: "new", updatedAt: "2024-06-01T00:00:00Z" },
    ] as never);

    const { result } = renderHook(() => useSettingsIncidentTypes(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.map((t: { id: string }) => t.id)).toEqual([
      "new",
      "old",
    ]);
  });

  it("should fall back to an empty array when the response is not an array", async () => {
    api.getIncidentTypes.mockResolvedValue(null as never);

    const { result } = renderHook(() => useSettingsIncidentTypes(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([]);
  });
});

describe("useIncidentType", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should stay disabled when the id is empty", () => {
    const { result } = renderHook(() => useIncidentType(""), {
      wrapper: createWrapper(),
    });
    expect(result.current.fetchStatus).toBe("idle");
    expect(api.getIncidentType).not.toHaveBeenCalled();
  });

  it("should load the detail when an id is provided", async () => {
    api.getIncidentType.mockResolvedValue({ id: "it-1" } as never);
    const { result } = renderHook(() => useIncidentType("it-1"), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(api.getIncidentType).toHaveBeenCalledWith("it-1", expect.anything());
  });
});

describe("useCreateIncidentType", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should create the type and notify success", async () => {
    api.createIncidentType.mockResolvedValue({ id: "it-new" } as never);

    const { result } = renderHook(() => useCreateIncidentType(), {
      wrapper: createWrapper(),
    });
    act(() => result.current.mutate({ name: "Fraude" } as never));

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(api.createIncidentType).toHaveBeenCalledWith({ name: "Fraude" });
    expect(messageSuccess).toHaveBeenCalled();
  });

  it("should notify an error when creation fails", async () => {
    api.createIncidentType.mockRejectedValue(new Error("boom"));

    const { result } = renderHook(() => useCreateIncidentType(), {
      wrapper: createWrapper(),
    });
    act(() => result.current.mutate({ name: "X" } as never));

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(messageError).toHaveBeenCalled();
  });
});
