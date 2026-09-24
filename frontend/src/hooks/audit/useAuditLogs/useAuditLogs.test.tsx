// Tests frontend : verifie le comportement de use audit logs.test.

import { renderHook, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  useAuditLogs,
  useAuditLog,
  useAuditLogsByUser,
  useAuditLogsByAction,
  useAuditLogsByResourceType,
  useAuditLogsByStatus,
  useAuditActions,
  useAuditStatuses,
} from "./useAuditLogs";
import { auditApi } from "../../../api/audit";
import type {
  AuditLogResponse,
  AuditEnumResponse,
  PagedAuditResponse,
} from "../../../api/audit";

vi.mock("../../../api/audit", () => ({
  auditApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    getByUserId: vi.fn(),
    getByAction: vi.fn(),
    getByResourceType: vi.fn(),
    getByStatus: vi.fn(),
    getAuditActions: vi.fn(),
    getAuditStatuses: vi.fn(),
  },
}));

// Couvre les comportements du module teste.
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

const MOCK_PAGED: PagedAuditResponse<AuditLogResponse> = {
  content: [
    {
      id: "log-1",
      status: "SUCCESS",
      timestamp: "2024-01-01T00:00:00Z",
      username: "admin",
      roles: [],
      action: "LOGIN_SUCCESS",
      resourceType: "USER",
    },
  ],
  totalElements: 1,
  totalPages: 1,
  size: 10,
  number: 0,
};
const MOCK_ENUM: AuditEnumResponse[] = [
  { code: "LOGIN_SUCCESS", name: "Login Success", description: "" },
];

describe("useAuditLogs hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should return data when the API call succeeds", async () => {
    vi.mocked(auditApi.getAll).mockResolvedValueOnce(MOCK_PAGED);
    const { result } = renderHook(() => useAuditLogs(), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(MOCK_PAGED);
  });

  it("should expose isError when the API call rejects", async () => {
    vi.mocked(auditApi.getAll).mockRejectedValueOnce(
      new Error("Network error"),
    );
    const { result } = renderHook(() => useAuditLogs(), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });

  it("should not fetch when id is undefined", async () => {
    const { result } = renderHook(() => useAuditLog(undefined), {
      wrapper: createWrapper(),
    });
    expect(result.current.fetchStatus).toBe("idle");
    expect(auditApi.getById).not.toHaveBeenCalled();
  });

  it("should fetch and return data when id is provided", async () => {
    vi.mocked(auditApi.getById).mockResolvedValueOnce({
      id: "log-1",
    } as AuditLogResponse);
    const { result } = renderHook(() => useAuditLog("log-1"), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(auditApi.getById).toHaveBeenCalledWith("log-1", expect.anything());
  });

  it("should not fetch when userId is undefined", () => {
    const { result } = renderHook(() => useAuditLogsByUser(undefined), {
      wrapper: createWrapper(),
    });
    expect(result.current.fetchStatus).toBe("idle");
    expect(auditApi.getByUserId).not.toHaveBeenCalled();
  });

  it("should not fetch when action is undefined", () => {
    const { result } = renderHook(() => useAuditLogsByAction(undefined), {
      wrapper: createWrapper(),
    });
    expect(result.current.fetchStatus).toBe("idle");
    expect(auditApi.getByAction).not.toHaveBeenCalled();
  });

  it("should not fetch when resourceType is undefined", () => {
    const { result } = renderHook(() => useAuditLogsByResourceType(undefined), {
      wrapper: createWrapper(),
    });
    expect(result.current.fetchStatus).toBe("idle");
    expect(auditApi.getByResourceType).not.toHaveBeenCalled();
  });

  it("should not fetch when status is undefined", () => {
    const { result } = renderHook(() => useAuditLogsByStatus(undefined), {
      wrapper: createWrapper(),
    });
    expect(result.current.fetchStatus).toBe("idle");
    expect(auditApi.getByStatus).not.toHaveBeenCalled();
  });

  it("should return actions data when the API call succeeds", async () => {
    vi.mocked(auditApi.getAuditActions).mockResolvedValueOnce(MOCK_ENUM);
    const { result } = renderHook(() => useAuditActions(), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(MOCK_ENUM);
  });

  it("should return statuses data when the API call succeeds", async () => {
    vi.mocked(auditApi.getAuditStatuses).mockResolvedValueOnce(MOCK_ENUM);
    const { result } = renderHook(() => useAuditStatuses(), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(MOCK_ENUM);
  });
});
