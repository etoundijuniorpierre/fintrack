// Tests frontend : verifie le comportement de notification bell.test.

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, act } from "@testing-library/react";
import NotificationBell from "./NotificationBell";
import { useAuth } from "../../../hooks/auth/useAuth";
import {
  useNotificationsByRecipient,
  useMarkNotificationRead,
} from "../../../hooks/notification";
import { useNavigate } from "react-router-dom";
import { APP_ROUTES } from "../../../utils/constants";
import type { NotificationResponse } from "../../../api/notification";

// Mock audio
const mockPlay = vi.fn().mockResolvedValue(undefined);
vi.stubGlobal(
  "Audio",
  class {
    play = mockPlay;
  },
);

// Mock i18next
vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: "fr" },
  }),
}));

// Mock router
vi.mock("react-router-dom", () => ({
  useNavigate: vi.fn(),
}));

// Mock auth
vi.mock("../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

// Mock antd App to avoid context missing issues
const mockNotificationInfo = vi.fn();
vi.mock("antd", async (importOriginal) => {
  const actual = await importOriginal<typeof import("antd")>();
  return {
    ...actual,
    App: {
      useApp: () => ({
        notification: { info: mockNotificationInfo },
      }),
    },
  };
});

// Mock notification hooks
vi.mock("../../../hooks/notification", () => ({
  useNotificationsByRecipient: vi.fn(),
  useMarkNotificationRead: vi.fn(),
  useNotificationWebSocket: vi.fn(),
}));

describe("NotificationBell", () => {
  const mockNavigate = vi.fn();
  const mockMarkAsRead = vi.fn();

  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();

    vi.mocked(useNavigate).mockReturnValue(mockNavigate);
    vi.mocked(useMarkNotificationRead).mockReturnValue({
      mutate: mockMarkAsRead,
    } as unknown as ReturnType<typeof useMarkNotificationRead>);
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  const setupMock = (
    hasPerm = true,
    pendingNotifs: Partial<NotificationResponse>[] = [],
  ) => {
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: () => hasPerm,
      user: { username: "me" },
    } as unknown as ReturnType<typeof useAuth>);

    // La cloche ne garde que les notifications PENDING du destinataire courant.
    const withStatus = pendingNotifs.map((n) => ({ status: "PENDING", ...n }));
    vi.mocked(useNotificationsByRecipient).mockReturnValue({
      data: withStatus.length > 0 ? withStatus : undefined,
    } as unknown as ReturnType<typeof useNotificationsByRecipient>);
  };

  const renderAndWait = () => {
    return render(<NotificationBell />);
  };

  it("renders null if user lacks permission", () => {
    setupMock(false);
    const { container } = renderAndWait();
    expect(container.firstChild).toBeNull();
  });

  it("renders bell icon and badge with count", () => {
    setupMock(true, [
      {
        id: "1",
        subject: "Notif 1",
        content: "Cont",
        createdAt: "2024-01-01T00:00:00Z",
      },
      {
        id: "2",
        subject: "Notif 2",
        content: "Cont",
        createdAt: "2024-01-01T00:00:00Z",
      },
    ]);
    renderAndWait();

    // Check if the bell button is visible
    const bellBtn = screen.getByTestId("notification-bell");
    expect(bellBtn).toBeInTheDocument();

    // Check if badge shows 2
    expect(screen.getByText("2")).toBeInTheDocument();
  });

  it("opens dropdown and displays list of notifications", () => {
    setupMock(true, [
      {
        id: "1",
        subject: "Alert 1",
        content: "Cont 1",
        createdAt: "2024-01-01T00:00:00Z",
      },
    ]);
    renderAndWait();

    const bellBtn = screen.getByTestId("notification-bell");
    act(() => {
      fireEvent.click(bellBtn);
      vi.runAllTimers();
    });

    const title = screen.getByText("notifications.pageTitle");
    expect(title).toBeInTheDocument();

    // Check if notification is rendered
    expect(screen.getByText("Alert 1")).toBeInTheDocument();
    expect(screen.getByText("Cont 1")).toBeInTheDocument();
  });

  it("shows empty message when no notifications", () => {
    setupMock(true, []);
    renderAndWait();

    const bellBtn = screen.getByTestId("notification-bell");
    act(() => {
      fireEvent.click(bellBtn);
      vi.runAllTimers();
    });

    const emptyMsg = screen.getByText("notifications.empty");
    expect(emptyMsg).toBeInTheDocument();
  });

  it("navigates to notifications list when view all is clicked", () => {
    setupMock(true, []);
    renderAndWait();

    act(() => {
      fireEvent.click(screen.getByTestId("notification-bell"));
      vi.runAllTimers();
    });

    const viewAllBtn = screen.getByText("notifications.viewAll");
    act(() => {
      fireEvent.click(viewAllBtn);
      vi.runAllTimers();
    });

    expect(mockNavigate).toHaveBeenCalledWith(APP_ROUTES.NOTIFICATIONS);
  });

  it("marks as read and navigates when a notification is clicked", () => {
    setupMock(true, [
      {
        id: "notif-1",
        subject: "Alert 1",
        content: "Cont 1",
        createdAt: "2024-01-01T00:00:00Z",
        incidentId: undefined,
      },
    ]);
    renderAndWait();

    act(() => {
      fireEvent.click(screen.getByTestId("notification-bell"));
      vi.runAllTimers();
    });

    const alertEl = screen.getByText("Alert 1");
    act(() => {
      fireEvent.click(alertEl);
      vi.runAllTimers();
    });

    expect(mockMarkAsRead).toHaveBeenCalledWith("notif-1");
    expect(mockNavigate).toHaveBeenCalledWith(APP_ROUTES.NOTIFICATIONS);
  });

  it("navigates to incident details when an incident notification is clicked", () => {
    setupMock(true, [
      {
        id: "notif-2",
        subject: "Alert 2",
        content: "Cont 2",
        createdAt: "2024-01-01T00:00:00Z",
        incidentId: {
          id: "inc-2",
          title: "Test incident",
        } as unknown as NotificationResponse["incidentId"],
      },
    ]);
    renderAndWait();

    act(() => {
      fireEvent.click(screen.getByTestId("notification-bell"));
      vi.runAllTimers();
    });

    const alertEl = screen.getByText("Alert 2");
    act(() => {
      fireEvent.click(alertEl);
      vi.runAllTimers();
    });

    expect(mockMarkAsRead).toHaveBeenCalledWith("notif-2");
    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.INCIDENTS_DETAIL("inc-2"),
    );
  });
});
