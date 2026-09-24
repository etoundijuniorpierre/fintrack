// Tests frontend : verifie le comportement de sidebar.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import Sidebar from "./Sidebar";
import { MemoryRouter } from "react-router-dom";
import { useUIStore } from "../../store/uiStore/uiStore";
import { useAuth } from "../../hooks/auth/useAuth";

// Prepare l'affichage lisible de sidebar.test.
const renderSidebar = (initialPath = "/dashboard") => {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Sidebar />
    </MemoryRouter>,
  );
};

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => (key === "common.app_name" ? "FinTrack" : key),
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../store/uiStore/uiStore", () => ({
  useUIStore: vi.fn(),
}));

vi.mock("../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

// Configure les mocks de navigation de la barre laterale.
const setupSidebarMock = (overrides = {}) => {
  vi.mocked(useUIStore).mockReturnValue({
    sidebarCollapsed: false,
    ...overrides,
  });
};

// Configure les droits utilisateur exposes au test.
const setupAuthMock = (permissions: string[] = []) => {
  vi.mocked(useAuth).mockReturnValue({
    hasPermission: (p: string) => permissions.includes(p),
    user: null,
    isAuthenticated: false,
    hasRole: () => false,
  });
};

// Configure un utilisateur de test avec toutes les permissions.
const setupAuthMockAllPermissions = () => {
  vi.mocked(useAuth).mockReturnValue({
    hasPermission: () => true,
    user: null,
    isAuthenticated: false,
    hasRole: () => false,
  });
};

describe("Sidebar tests", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("displays the expanded logo when sidebar is not collapsed", () => {
    setupSidebarMock({ sidebarCollapsed: false });
    setupAuthMockAllPermissions();
    renderSidebar();
    expect(screen.getByAltText("FinTrack")).toHaveAttribute(
      "src",
      "/Img/logo-dark.svg",
    );
  });

  it("displays the icon-only mark when sidebar is collapsed", () => {
    setupSidebarMock({ sidebarCollapsed: true });
    setupAuthMockAllPermissions();
    renderSidebar();
    expect(screen.getByAltText("FinTrack")).toHaveAttribute(
      "src",
      "/Img/logo-mark-dark.svg",
    );
  });

  it("displays the full logo when sidebar is expanded", () => {
    setupSidebarMock({ sidebarCollapsed: false });
    setupAuthMockAllPermissions();
    renderSidebar();
    expect(screen.getByAltText("FinTrack")).toHaveAttribute(
      "src",
      "/Img/logo-dark.svg",
    );
  });

  it("displays main navigation links with their translation keys", () => {
    setupSidebarMock();
    setupAuthMockAllPermissions();
    renderSidebar();

    const expectedKeys = [
      "layout.sidebar.dashboard",
      "layout.sidebar.users",
      "layout.sidebar.incidents",
      "layout.sidebar.reports",
      "layout.sidebar.audit",
      "layout.sidebar.settings",
    ];

    expectedKeys.forEach((key) => {
      expect(screen.getByText(key)).toBeInTheDocument();
    });
  });

  it("should reflect collapsed state in Sider classes", () => {
    setupSidebarMock({ sidebarCollapsed: true });
    setupAuthMockAllPermissions();
    renderSidebar();

    const sider = screen.getByRole("complementary");
    expect(sider).toHaveClass("ant-layout-sider-collapsed");
  });

  it("shows only Dashboard when user has only DASHBOARD_CONFIGURE", () => {
    setupSidebarMock();
    setupAuthMock(["DASHBOARD_CONFIGURE"]);
    renderSidebar();

    expect(screen.getByText("layout.sidebar.dashboard")).toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.incidents"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("layout.sidebar.users")).not.toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.reports"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("layout.sidebar.audit")).not.toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.settings"),
    ).not.toBeInTheDocument();
  });

  it("shows Incidents and Users when user has INCIDENT_VIEW_ALL and USER_VIEW_ALL", () => {
    setupSidebarMock();
    setupAuthMock(["INCIDENT_VIEW_ALL", "USER_VIEW_ALL"]);
    renderSidebar();

    expect(screen.getByText("layout.sidebar.incidents")).toBeInTheDocument();
    expect(screen.getByText("layout.sidebar.users")).toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.dashboard"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.reports"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("layout.sidebar.audit")).not.toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.settings"),
    ).not.toBeInTheDocument();
  });

  it("shows Users when user has only USER_VIEW_SERVICE (service manager)", () => {
    setupSidebarMock();
    setupAuthMock(["USER_VIEW_SERVICE"]);
    renderSidebar();

    expect(screen.getByText("layout.sidebar.users")).toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.dashboard"),
    ).not.toBeInTheDocument();
  });

  it("shows Users when user has only USER_VIEW_AGENCY (agency manager)", () => {
    setupSidebarMock();
    setupAuthMock(["USER_VIEW_AGENCY"]);
    renderSidebar();

    expect(screen.getByText("layout.sidebar.users")).toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.dashboard"),
    ).not.toBeInTheDocument();
  });

  it("shows no menu items when user has no permissions", () => {
    setupSidebarMock();
    setupAuthMock([]);
    renderSidebar();

    expect(
      screen.queryByText("layout.sidebar.dashboard"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.incidents"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("layout.sidebar.users")).not.toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.reports"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("layout.sidebar.audit")).not.toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.settings"),
    ).not.toBeInTheDocument();
  });

  it("shows Incidents when user has INCIDENT_VIEW_OWN (any incident view permission suffices)", () => {
    setupSidebarMock();
    setupAuthMock(["INCIDENT_VIEW_OWN"]);
    renderSidebar();

    expect(screen.getByText("layout.sidebar.incidents")).toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.dashboard"),
    ).not.toBeInTheDocument();
  });

  it("shows Settings when user has any settings permission", () => {
    setupSidebarMock();
    setupAuthMock(["SETTINGS_INCIDENT_TYPES"]);
    renderSidebar();

    expect(screen.getByText("layout.sidebar.settings")).toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.dashboard"),
    ).not.toBeInTheDocument();
  });

  it("shows Audit only when user has AUDIT_VIEW", () => {
    setupSidebarMock();
    setupAuthMock(["AUDIT_VIEW"]);
    renderSidebar();

    expect(screen.getByText("layout.sidebar.audit")).toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.dashboard"),
    ).not.toBeInTheDocument();
  });

  it("shows Reports when user has REPORT_VIEW_AGENCY", () => {
    setupSidebarMock();
    setupAuthMock(["REPORT_VIEW_AGENCY"]);
    renderSidebar();

    expect(screen.getByText("layout.sidebar.reports")).toBeInTheDocument();
    expect(
      screen.queryByText("layout.sidebar.dashboard"),
    ).not.toBeInTheDocument();
  });

  it("marks only Dashboard as active when navigating to /dashboard", () => {
    setupSidebarMock();
    setupAuthMockAllPermissions();
    renderSidebar("/dashboard");

    const activeItems = document.querySelectorAll(".ant-menu-item-selected");
    expect(activeItems).toHaveLength(1);
    expect(activeItems[0]).toHaveTextContent("layout.sidebar.dashboard");
  });

  it("marks only Incidents as active when navigating to /dashboard/incidents", () => {
    setupSidebarMock();
    setupAuthMockAllPermissions();
    renderSidebar("/dashboard/incidents");

    const activeItems = document.querySelectorAll(".ant-menu-item-selected");
    expect(activeItems).toHaveLength(1);
    expect(activeItems[0]).toHaveTextContent("layout.sidebar.incidents");
  });

  it("marks only Users as active when navigating to /dashboard/users", () => {
    setupSidebarMock();
    setupAuthMockAllPermissions();
    renderSidebar("/dashboard/users");

    const activeItems = document.querySelectorAll(".ant-menu-item-selected");
    expect(activeItems).toHaveLength(1);
    expect(activeItems[0]).toHaveTextContent("layout.sidebar.users");
  });

  it("no two items are active simultaneously regardless of route", () => {
    setupSidebarMock();
    setupAuthMockAllPermissions();

    const routes = [
      "/dashboard",
      "/dashboard/incidents",
      "/dashboard/users",
      "/dashboard/reports",
      "/dashboard/audit",
      "/dashboard/settings",
    ];

    routes.forEach((route) => {
      const { unmount } = renderSidebar(route);
      const activeItems = document.querySelectorAll(".ant-menu-item-selected");
      expect(activeItems.length).toBeLessThanOrEqual(1);
      unmount();
    });
  });
});
