// Tests frontend : verifie le comportement de use notification web socket.test.

import { renderHook } from "@testing-library/react";
import { useNotificationWebSocket } from "./useNotificationWebSocket";
import { useAuth } from "../../auth/useAuth";
import { tokenManager } from "../../../utils/tokenManager/tokenManager";
import { Client } from "@stomp/stompjs";
import { vi, describe, it, expect, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import type { AuthUser } from "../../../store/authStore/authStore";
import { APP_ROUTES, QUERY_KEYS } from "../../../utils/constants";
import {
  makeIncidentResponse,
  makeIncidentSummary,
} from "../../../mocks/incident/incidentFixtures";
import { useNavigate } from "react-router-dom";

// Mock dependencies
vi.mock("../../auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../../../utils/tokenManager/tokenManager", () => ({
  tokenManager: {
    getToken: vi.fn(),
  },
}));

// Mock STOMP Client
const mockActivate = vi.fn();
const mockDeactivate = vi.fn();
const mockSubscribe = vi.fn();
const mockDesktopNotification = vi.fn();
const mockRequestNotificationPermission = vi.fn();
const mockNavigate = vi.fn();
const mockCloseDesktopNotification = vi.fn();
let mockNotificationInstance: {
  onclick: (() => void) | null;
  close: typeof mockCloseDesktopNotification;
};

vi.mock("react-router-dom", () => ({
  useNavigate: vi.fn(),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) =>
      ({
        "notifications.desktop.title": "FinTrack - Nouvelle alerte",
        "notifications.desktop.defaultBody":
          "Vous avez une nouvelle notification.",
      })[key] || key,
    i18n: { language: "fr" },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Type le client STOMP simule par les tests WebSocket.
interface StompClientMockInstance {
  activate: () => void;
  deactivate: () => void;
  subscribe: (
    destination: string,
    callback: (message: { body: string }) => void,
  ) => void;
  options: {
    onConnect?: () => void;
  };
}

vi.mock("@stomp/stompjs", () => {
  return {
    Client: vi.fn().mockImplementation(function (
      this: StompClientMockInstance,
      options: { onConnect?: () => void },
    ) {
      this.activate = mockActivate;
      this.deactivate = mockDeactivate;
      this.subscribe = mockSubscribe;
      this.options = options;

      // Simulate connection
      setTimeout(() => {
        if (options.onConnect) {
          options.onConnect();
        }
      }, 0);
    }),
  };
});

vi.mock("sockjs-client", () => {
  return {
    default: vi.fn().mockImplementation(() => ({})),
  };
});

describe("useNotificationWebSocket", () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    vi.clearAllMocks();
    mockNotificationInstance = {
      onclick: null,
      close: mockCloseDesktopNotification,
    };
    mockDesktopNotification.mockImplementation(function () {
      return mockNotificationInstance;
    });
    vi.mocked(useNavigate).mockReturnValue(mockNavigate);
    Object.defineProperty(document, "hidden", {
      configurable: true,
      value: false,
    });
    Object.defineProperty(mockDesktopNotification, "permission", {
      configurable: true,
      value: "granted",
    });
    Object.defineProperty(mockDesktopNotification, "requestPermission", {
      configurable: true,
      value: mockRequestNotificationPermission,
    });
    mockRequestNotificationPermission.mockResolvedValue("granted");
    vi.stubGlobal("Notification", mockDesktopNotification);
    vi.stubGlobal(
      "Audio",
      vi.fn().mockImplementation(function () {
        return {
          play: vi.fn().mockResolvedValue(undefined),
        };
      }),
    );
  });

  const createWrapper = () => {
    return ({ children }: { children: React.ReactNode }) =>
      React.createElement(
        QueryClientProvider,
        { client: queryClient },
        children,
      );
  };

  const setupAuthMock = (user: AuthUser | null, isAuthenticated = false) => {
    vi.mocked(useAuth).mockReturnValue({
      user,
      isAuthenticated,
      hasRole: vi.fn().mockReturnValue(false),
      hasPermission: vi.fn().mockReturnValue(false),
    });
  };

  const mockUser: AuthUser = {
    id: "1",
    username: "testuser",
    roles: [],
    permissions: [],
  };

  it("should not connect if user is not authenticated", () => {
    setupAuthMock(null, false);

    renderHook(() => useNotificationWebSocket(), { wrapper: createWrapper() });

    expect(Client).not.toHaveBeenCalled();
  });

  it("should not connect if there is no token", () => {
    setupAuthMock(mockUser, true);
    vi.mocked(tokenManager.getToken).mockReturnValue(null);

    renderHook(() => useNotificationWebSocket(), { wrapper: createWrapper() });

    expect(Client).not.toHaveBeenCalled();
  });

  it("should connect, subscribe, and deactivate on unmount", async () => {
    setupAuthMock(mockUser, true);
    vi.mocked(tokenManager.getToken).mockReturnValue("mock-jwt-token");

    const { unmount } = renderHook(() => useNotificationWebSocket(), {
      wrapper: createWrapper(),
    });

    expect(Client).toHaveBeenCalled();
    expect(mockActivate).toHaveBeenCalled();

    // Battements et rafraichissement du token indispensables aux reconnexions.
    expect(Client).toHaveBeenCalledWith(
      expect.objectContaining({
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        beforeConnect: expect.any(Function),
      }),
    );

    // Attend le declenchement simule de onConnect.
    await new Promise((resolve) => setTimeout(resolve, 10));

    expect(mockSubscribe).toHaveBeenCalledWith(
      "/topic/notifications/testuser",
      expect.any(Function),
    );

    unmount();
    expect(mockDeactivate).toHaveBeenCalled();
  });

  it("should update the presence cache from WebSocket events", async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: mockUser,
      isAuthenticated: true,
      hasRole: vi.fn().mockReturnValue(false),
      hasPermission: vi.fn().mockReturnValue(true),
    });
    vi.mocked(tokenManager.getToken).mockReturnValue("mock-jwt-token");
    const invalidateQueriesSpy = vi.spyOn(queryClient, "invalidateQueries");

    renderHook(() => useNotificationWebSocket(), {
      wrapper: createWrapper(),
    });

    await new Promise((resolve) => setTimeout(resolve, 10));

    const presenceSubscription = mockSubscribe.mock.calls.find(
      ([destination]) => destination === "/topic/presence",
    );
    expect(presenceSubscription).toBeDefined();

    presenceSubscription?.[1]({
      body: JSON.stringify({
        online: ["alice", "bob"],
        states: {
          alice: {
            online: true,
            connectedAt: "2026-07-29T08:27:00",
          },
        },
      }),
    });

    expect(queryClient.getQueryData(QUERY_KEYS.PRESENCE.ONLINE)).toEqual({
      online: ["alice", "bob"],
      states: {
        alice: {
          online: true,
          connectedAt: "2026-07-29T08:27:00",
        },
      },
    });
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({
      queryKey: ["dashboard", "user-stats"],
    });
  });

  it("should refresh and update incident caches when an incident notification is received", async () => {
    setupAuthMock(mockUser, true);
    vi.mocked(tokenManager.getToken).mockReturnValue("mock-jwt-token");
    const invalidateQueriesSpy = vi.spyOn(queryClient, "invalidateQueries");
    const listQueryKey = [...QUERY_KEYS.INCIDENTS.ALL, { page: 0 }] as const;

    queryClient.setQueryData(
      QUERY_KEYS.INCIDENTS.DETAIL("incident-1"),
      makeIncidentResponse({ id: "incident-1", status: "OPEN" }),
    );
    queryClient.setQueryData(listQueryKey, {
      content: [
        makeIncidentSummary({ id: "incident-1", status: "OPEN" }),
        makeIncidentSummary({ id: "incident-2", status: "OPEN" }),
      ],
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 10,
      first: true,
      last: true,
      empty: false,
    });

    renderHook(() => useNotificationWebSocket(), {
      wrapper: createWrapper(),
    });

    await new Promise((resolve) => setTimeout(resolve, 10));

    const callback = mockSubscribe.mock.calls[0]?.[1] as
      | ((message: { body: string }) => void)
      | undefined;
    expect(callback).toBeDefined();

    callback?.({
      body: JSON.stringify({
        subject: "Incident blocked",
        incidentId: { id: "incident-1", status: "Bloqué" },
        templateParams: {
          incident_status: "Bloqué",
          incident_status_code: "BLOCKED",
        },
      }),
    });

    expect(
      queryClient.getQueryData<ReturnType<typeof makeIncidentResponse>>(
        QUERY_KEYS.INCIDENTS.DETAIL("incident-1"),
      )?.status,
    ).toBe("BLOCKED");
    expect(
      queryClient.getQueryData<{
        content: ReturnType<typeof makeIncidentSummary>[];
      }>(listQueryKey)?.content[0].status,
    ).toBe("BLOCKED");
    expect(
      queryClient.getQueryData<{
        content: ReturnType<typeof makeIncidentSummary>[];
      }>(listQueryKey)?.content[1].status,
    ).toBe("OPEN");

    expect(invalidateQueriesSpy).toHaveBeenCalledWith({
      queryKey: QUERY_KEYS.INCIDENTS.ALL,
    });
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({
      queryKey: QUERY_KEYS.INCIDENTS.DETAIL("incident-1"),
    });
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({
      queryKey: QUERY_KEYS.INCIDENTS.HISTORY("incident-1"),
    });
    expect(invalidateQueriesSpy).toHaveBeenCalledWith({
      queryKey: ["dashboard"],
    });
  });

  it("should show a native notification when the document is hidden", async () => {
    setupAuthMock(mockUser, true);
    vi.mocked(tokenManager.getToken).mockReturnValue("mock-jwt-token");
    Object.defineProperty(document, "hidden", {
      configurable: true,
      value: true,
    });

    const { unmount } = renderHook(() => useNotificationWebSocket(), {
      wrapper: createWrapper(),
    });

    await new Promise((resolve) => setTimeout(resolve, 10));

    const callback = mockSubscribe.mock.calls[0]?.[1] as
      | ((message: { body: string }) => void)
      | undefined;
    expect(callback).toBeDefined();

    callback?.({
      body: JSON.stringify({
        subject: "Incident requires validation",
        incidentId: { id: "incident-1", status: "PENDING_VALIDATION" },
      }),
    });

    expect(mockDesktopNotification).toHaveBeenCalledWith(
      "FinTrack - Nouvelle alerte",
      {
        body: "Incident requires validation",
        icon: "/Img/FinstarLogo.png",
        requireInteraction: true,
      },
    );

    unmount();
  });

  it("should request permission before showing a native notification when permission is default", async () => {
    setupAuthMock(mockUser, true);
    vi.mocked(tokenManager.getToken).mockReturnValue("mock-jwt-token");
    Object.defineProperty(document, "hidden", {
      configurable: true,
      value: true,
    });
    Object.defineProperty(mockDesktopNotification, "permission", {
      configurable: true,
      value: "default",
    });

    const { unmount } = renderHook(() => useNotificationWebSocket(), {
      wrapper: createWrapper(),
    });

    await new Promise((resolve) => setTimeout(resolve, 10));

    const callback = mockSubscribe.mock.calls[0]?.[1] as
      | ((message: { body: string }) => void)
      | undefined;
    expect(callback).toBeDefined();

    callback?.({
      body: JSON.stringify({
        subject: "Incident pending validation",
        incidentId: { id: "incident-1", status: "PENDING_VALIDATION" },
      }),
    });

    await vi.waitFor(() => {
      expect(mockRequestNotificationPermission).toHaveBeenCalled();
      expect(mockDesktopNotification).toHaveBeenCalledWith(
        "FinTrack - Nouvelle alerte",
        {
          body: "Incident pending validation",
          icon: "/Img/FinstarLogo.png",
          requireInteraction: true,
        },
      );
    });

    unmount();
  });

  it("should navigate to incident details when the native notification is clicked", async () => {
    setupAuthMock(mockUser, true);
    vi.mocked(tokenManager.getToken).mockReturnValue("mock-jwt-token");
    Object.defineProperty(document, "hidden", {
      configurable: true,
      value: true,
    });
    const focusSpy = vi.spyOn(window, "focus").mockImplementation(() => {});

    const { unmount } = renderHook(() => useNotificationWebSocket(), {
      wrapper: createWrapper(),
    });

    await new Promise((resolve) => setTimeout(resolve, 10));

    const callback = mockSubscribe.mock.calls[0]?.[1] as
      | ((message: { body: string }) => void)
      | undefined;
    expect(callback).toBeDefined();

    callback?.({
      body: JSON.stringify({
        subject: "Incident requires validation",
        incidentId: { id: "incident-1", status: "PENDING_VALIDATION" },
      }),
    });

    await vi.waitFor(() =>
      expect(mockNotificationInstance.onclick).toBeTypeOf("function"),
    );
    mockNotificationInstance.onclick?.();

    expect(focusSpy).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.INCIDENTS_DETAIL("incident-1"),
    );
    expect(mockCloseDesktopNotification).toHaveBeenCalled();

    focusSpy.mockRestore();
    unmount();
  });
});
