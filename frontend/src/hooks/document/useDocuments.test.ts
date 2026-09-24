// Tests : hooks de gestion des pieces jointes (chargement, upload, suppression).

import { renderHook, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import {
  useIncidentAttachments,
  useUploadAttachment,
  useDeleteAttachment,
} from "./useDocuments";
import { documentApi } from "../../api/document/documentApi";

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

vi.mock("../../api/document/documentApi", () => ({
  documentApi: { getByIncidentId: vi.fn(), upload: vi.fn(), delete: vi.fn() },
}));

const api = vi.mocked(documentApi);

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

describe("useIncidentAttachments", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should load attachments when an incidentId is provided", async () => {
    const attachments = [{ id: "a1" }] as never;
    api.getByIncidentId.mockResolvedValue(attachments);

    const { result } = renderHook(() => useIncidentAttachments("inc-1"), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(api.getByIncidentId).toHaveBeenCalledWith("inc-1");
    expect(result.current.data).toBe(attachments);
  });

  it("should stay disabled (no call) without an incidentId", () => {
    const { result } = renderHook(() => useIncidentAttachments(undefined), {
      wrapper: createWrapper(),
    });
    expect(result.current.fetchStatus).toBe("idle");
    expect(api.getByIncidentId).not.toHaveBeenCalled();
  });
});

describe("useUploadAttachment", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should upload the file to the target incident", async () => {
    api.upload.mockResolvedValue({ id: "att-1" } as never);
    const file = new File(["x"], "doc.pdf");

    const { result } = renderHook(() => useUploadAttachment("inc-9"), {
      wrapper: createWrapper(),
    });
    act(() => result.current.mutate({ file }));

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(api.upload).toHaveBeenCalledWith(file, "inc-9", {
      category: undefined,
    });
  });
});

describe("useDeleteAttachment", () => {
  beforeEach(() => vi.clearAllMocks());

  it("should delete the attachment by its id", async () => {
    api.delete.mockResolvedValue(undefined);

    const { result } = renderHook(() => useDeleteAttachment("inc-9"), {
      wrapper: createWrapper(),
    });
    act(() => result.current.mutate("att-5"));

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(api.delete).toHaveBeenCalledWith("att-5");
  });
});
