import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, act } from "@testing-library/react";
import AppHeader from "./AppHeader";
import { useAuth, useLogout } from "../../hooks/auth/useAuth";
import { useMyProfile } from "../../hooks/user/useMyProfile";
import { useUIStore } from "../../store/uiStore/uiStore";
import {
  useNotificationsByStatus,
  useNotificationsByRecipient,
} from "../../hooks/notification";
import { APP_ROUTES } from "../../utils/constants";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: "fr", changeLanguage: vi.fn() },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

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

vi.mock("react-router-dom", () => ({
  useNavigate: vi.fn(),
}));

vi.mock("../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
  useLogout: vi.fn(),
}));

vi.mock("../../hooks/user/useMyProfile", () => ({
  useMyProfile: vi.fn(),
}));

vi.mock("../../store/uiStore/uiStore", () => ({
  useUIStore: vi.fn(),
}));

vi.mock("../../hooks/notification", () => ({
  useNotificationsByStatus: vi.fn(),
  useNotificationsByRecipient: vi.fn(),
  useMarkNotificationRead: () => ({ mutate: vi.fn(), isPending: false }),
  useNotificationWebSocket: vi.fn(),
}));

// La presence est couverte par le test dedie du composant : on neutralise le
// composant et son hook, qui interroge le serveur via TanStack Query.
vi.mock("./presence/PresenceIndicator", () => ({
  default: () => null,
}));

vi.mock("../../hooks/user/useOnlinePresence/useOnlinePresence", () => ({
  PRESENCE_VIEW_PERMISSIONS: [],
  useOnlinePresence: () => ({
    online: [],
    isOnline: () => false,
    canViewPresence: false,
    isLoading: false,
  }),
}));

import { useNavigate } from "react-router-dom";

// Configure les mocks de l'entete pour un scenario de test.
const setupHeaderMocks = (overrides: Record<string, unknown> = {}) => {
  const defaultUser = { username: "john_doe", roles: ["ADMIN"] };
  const mockLogoutMutate = vi.fn();
  const mockNavigate = vi.fn();

  vi.mocked(useAuth).mockReturnValue({
    user: Object.hasOwn(overrides, "user") ? overrides.user : defaultUser,
    hasPermission:
      (overrides.hasPermission as (p: string) => boolean) ||
      vi.fn().mockReturnValue(false),
  } as Partial<ReturnType<typeof useAuth>> as ReturnType<typeof useAuth>);

  vi.mocked(useLogout).mockReturnValue({
    mutate: (overrides.logoutMutate as () => void) || mockLogoutMutate,
  } as Partial<ReturnType<typeof useLogout>> as ReturnType<typeof useLogout>);

  vi.mocked(useUIStore).mockReturnValue({
    sidebarCollapsed: (overrides.sidebarCollapsed as boolean) || false,
    toggleSidebar: (overrides.toggleSidebar as () => void) || vi.fn(),
  } as Partial<ReturnType<typeof useUIStore>> as ReturnType<typeof useUIStore>);

  vi.mocked(useNavigate).mockReturnValue(
    (overrides.navigate as () => void) || mockNavigate,
  );

  vi.mocked(useMyProfile).mockReturnValue({
    data: overrides.profile ?? {},
  } as Partial<ReturnType<typeof useMyProfile>> as ReturnType<
    typeof useMyProfile
  >);

  vi.mocked(useNotificationsByStatus).mockReturnValue(
    (Object.hasOwn(overrides, "pendingNotifications")
      ? overrides.pendingNotifications
      : { data: { content: [] } }) as Partial<
      ReturnType<typeof useNotificationsByStatus>
    > as ReturnType<typeof useNotificationsByStatus>,
  );

  vi.mocked(useNotificationsByRecipient).mockReturnValue(
    (Object.hasOwn(overrides, "pendingNotifications")
      ? overrides.pendingNotifications
      : { data: [] }) as Partial<
      ReturnType<typeof useNotificationsByRecipient>
    > as ReturnType<typeof useNotificationsByRecipient>,
  );

  return { mockLogoutMutate, mockNavigate };
};

describe("AppHeader", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();
    window.HTMLMediaElement.prototype.play = vi
      .fn()
      .mockResolvedValue(undefined);
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  const renderAndWait = () => {
    const utils = render(<AppHeader />);
    act(() => {
      vi.advanceTimersByTime(3000);
    });
    return utils;
  };

  it("displays username and role", () => {
    const customUser = { username: "john_doe", roles: ["ADMIN"] };
    setupHeaderMocks({ user: customUser });

    renderAndWait();

    expect(screen.getByText(customUser.username)).toBeInTheDocument();
    expect(screen.getByText("ADMIN")).toBeInTheDocument();
  });

  it("should fallback to default role translation key when user has no roles", () => {
    setupHeaderMocks({ user: { username: "no_role_user", roles: [] } });

    renderAndWait();

    expect(screen.getByText("layout.header.default_role")).toBeInTheDocument();
  });

  it("triggers sidebar toggle via accessible button", () => {
    const toggleSidebar = vi.fn();
    setupHeaderMocks({ toggleSidebar });

    renderAndWait();

    const toggleBtn = screen.getByRole("button", {
      name: "layout.sidebar.collapse",
    });
    fireEvent.click(toggleBtn);

    expect(toggleSidebar).toHaveBeenCalledTimes(1);
  });

  it("displays tooltip translation for notification button", () => {
    setupHeaderMocks({
      hasPermission: (permission: string) =>
        permission === "NOTIFICATION_VIEW_OWN",
    });
    renderAndWait();

    const notifyBtn = screen.getByRole("button", {
      name: "layout.header.notifications",
    });
    expect(notifyBtn).toBeInTheDocument();
  });

  it("initiates logout via user menu", async () => {
    const logoutMutate = vi.fn();
    setupHeaderMocks({ logoutMutate });

    renderAndWait();

    fireEvent.click(
      screen.getByRole("button", { name: "layout.header.user_menu" }),
    );

    const logoutItem = screen.getByText("layout.header.logout");
    fireEvent.click(logoutItem);

    expect(logoutMutate).toHaveBeenCalledTimes(1);
  });

  it('navigates to notifications route when "view all" is clicked in dropdown', () => {
    const { mockNavigate } = setupHeaderMocks({
      hasPermission: (permission: string) =>
        permission === "NOTIFICATION_VIEW_OWN",
    });

    renderAndWait();

    const bellBtn = screen.getByTestId("notification-bell");
    act(() => {
      fireEvent.click(bellBtn);
      vi.runAllTimers();
    });

    const viewAllBtn = screen.getByText("notifications.viewAll");
    act(() => {
      fireEvent.click(viewAllBtn);
      vi.runAllTimers();
    });

    expect(mockNavigate).toHaveBeenCalledWith(APP_ROUTES.NOTIFICATIONS);
  });

  it("shows unread badge count from useNotificationsByStatus", () => {
    setupHeaderMocks({
      hasPermission: (permission: string) =>
        permission === "NOTIFICATION_VIEW_OWN",
      pendingNotifications: {
        data: {
          content: [
            { id: 1, status: "PENDING" },
            { id: 2, status: "PENDING" },
          ],
        },
      },
    });

    renderAndWait();

    expect(screen.getByText("2")).toBeInTheDocument();
  });
});
