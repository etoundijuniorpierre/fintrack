// Tests frontend : verifie le comportement de use notifications.test.

import { renderHook, waitFor, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import {
  useNotifications,
  useNotification,
  useNotificationsByStatus,
  useNotificationsByRecipientPage,
  useCreateNotification,
  useMarkNotificationRead,
  useDeleteNotification,
  useBulkDeleteNotifications,
  useNotificationTypes,
  useNotificationStatuses,
} from "./useNotifications";
import { notificationApi } from "../../../api/notification";
import type {
  NotificationResponse,
  NotificationEnumResponse,
  PagedNotificationResponse,
} from "../../../api/notification";

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

vi.mock("../../../api/notification", () => ({
  notificationApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    getByStatus: vi.fn(),
    getByIncidentId: vi.fn(),
    getByRecipient: vi.fn(),
    getByRecipientPage: vi.fn(),
    create: vi.fn(),
    markAsRead: vi.fn(),
    delete: vi.fn(),
    bulkDelete: vi.fn(),
    getNotificationTypes: vi.fn(),
    getNotificationStatuses: vi.fn(),
  },
}));

// Couvre les comportements du module teste.
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

const MOCK_NOTIFICATION: NotificationResponse = {
  id: "notif-1",
  type: "EMAIL",
  recipient: "user@example.com",
  status: "PENDING",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const MOCK_PAGED: PagedNotificationResponse<NotificationResponse> = {
  content: [MOCK_NOTIFICATION],
  totalElements: 1,
  totalPages: 1,
  size: 10,
  number: 0,
};
const MOCK_ENUM: NotificationEnumResponse[] = [
  { code: "EMAIL", name: "Email", description: "" },
];

describe("useNotifications hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("useNotifications uses QUERY_KEYS.NOTIFICATIONS.ALL and returns data", async () => {
    vi.mocked(notificationApi.getAll).mockResolvedValueOnce(MOCK_PAGED);
    const { result } = renderHook(() => useNotifications(), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(MOCK_PAGED);
  });

  it("useNotification is disabled when id is undefined", () => {
    const { result } = renderHook(() => useNotification(undefined), {
      wrapper: createWrapper(),
    });
    expect(result.current.fetchStatus).toBe("idle");
    expect(notificationApi.getById).not.toHaveBeenCalled();
  });

  it("useNotification fetches when id is provided", async () => {
    vi.mocked(notificationApi.getById).mockResolvedValueOnce(MOCK_NOTIFICATION);
    const { result } = renderHook(() => useNotification("notif-1"), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(notificationApi.getById).toHaveBeenCalledWith(
      "notif-1",
      expect.anything(),
    );
  });

  it("useNotificationsByStatus is disabled when status is undefined", () => {
    const { result } = renderHook(() => useNotificationsByStatus(undefined), {
      wrapper: createWrapper(),
    });
    expect(result.current.fetchStatus).toBe("idle");
    expect(notificationApi.getByStatus).not.toHaveBeenCalled();
  });

  it("useNotificationsByRecipientPage uses server pagination params", async () => {
    vi.mocked(notificationApi.getByRecipientPage).mockResolvedValueOnce(
      MOCK_PAGED,
    );
    const params = { page: 0, size: 10, sort: "createdAt,desc" };
    const { result } = renderHook(
      () => useNotificationsByRecipientPage("seed.agent.01", params),
      { wrapper: createWrapper() },
    );
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(notificationApi.getByRecipientPage).toHaveBeenCalledWith(
      "seed.agent.01",
      params,
      expect.anything(),
    );
  });

  it("useCreateNotification invalidates QUERY_KEYS.NOTIFICATIONS.ALL on success", async () => {
    vi.mocked(notificationApi.create).mockResolvedValueOnce(MOCK_NOTIFICATION);
    const wrapper = createWrapper();
    const { result } = renderHook(() => useCreateNotification(), { wrapper });
    await act(async () => {
      result.current.mutate({ type: "EMAIL", recipient: "user@example.com" });
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(notificationApi.create).toHaveBeenCalledWith({
      type: "EMAIL",
      recipient: "user@example.com",
    });
  });

  it("useMarkNotificationRead calls notificationApi.markAsRead with id", async () => {
    vi.mocked(notificationApi.markAsRead).mockResolvedValueOnce({
      ...MOCK_NOTIFICATION,
      status: "READ",
    } as NotificationResponse);
    const { result } = renderHook(() => useMarkNotificationRead(), {
      wrapper: createWrapper(),
    });
    await act(async () => {
      result.current.mutate("notif-1");
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(notificationApi.markAsRead).toHaveBeenCalledWith("notif-1");
  });

  it("useDeleteNotification calls notificationApi.delete with id", async () => {
    vi.mocked(notificationApi.delete).mockResolvedValueOnce(undefined);
    const { result } = renderHook(() => useDeleteNotification(), {
      wrapper: createWrapper(),
    });
    await act(async () => {
      result.current.mutate("notif-1");
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(notificationApi.delete).toHaveBeenCalledWith("notif-1");
  });

  it("useBulkDeleteNotifications calls notificationApi.bulkDelete with ids", async () => {
    vi.mocked(notificationApi.bulkDelete).mockResolvedValueOnce(undefined);
    const { result } = renderHook(() => useBulkDeleteNotifications(), {
      wrapper: createWrapper(),
    });
    await act(async () => {
      result.current.mutate(["notif-1", "notif-2"]);
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(notificationApi.bulkDelete).toHaveBeenCalledWith([
      "notif-1",
      "notif-2",
    ]);
  });

  it("useNotificationTypes uses staleTime of 10 minutes", async () => {
    vi.mocked(notificationApi.getNotificationTypes).mockResolvedValueOnce(
      MOCK_ENUM,
    );
    const { result } = renderHook(() => useNotificationTypes(), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(MOCK_ENUM);
    expect(notificationApi.getNotificationTypes).toHaveBeenCalledTimes(1);
  });

  it("useNotificationStatuses uses staleTime of 10 minutes", async () => {
    vi.mocked(notificationApi.getNotificationStatuses).mockResolvedValueOnce(
      MOCK_ENUM,
    );
    const { result } = renderHook(() => useNotificationStatuses(), {
      wrapper: createWrapper(),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual(MOCK_ENUM);
    expect(notificationApi.getNotificationStatuses).toHaveBeenCalledTimes(1);
  });

  it("useCreateNotification calls message.error on failure", async () => {
    const errorMessage = "Network error";
    vi.mocked(notificationApi.create).mockRejectedValueOnce(
      new Error(errorMessage),
    );
    const { result } = renderHook(() => useCreateNotification(), {
      wrapper: createWrapper(),
    });
    await act(async () => {
      result.current.mutate({ type: "EMAIL", recipient: "user@example.com" });
    });
    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(notificationApi.create).toHaveBeenCalledTimes(1);
  });
});
