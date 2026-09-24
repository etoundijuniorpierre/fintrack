// Tests frontend : verifie le comportement de settings.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import Settings from "./Settings";
import { useAuth } from "../../hooks/auth/useAuth";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("./components/IncidentTypeTab/IncidentTypeTab", () => ({
  default: () => <div data-testid="incident-type-tab" />,
}));
vi.mock("./components/AgencyTab/AgencyTab", () => ({
  default: () => <div data-testid="agency-tab" />,
}));
vi.mock("./components/ServiceTab/ServiceTab", () => ({
  default: () => <div data-testid="service-tab" />,
}));
vi.mock("./components/ReportScheduleTab/ReportScheduleTab", () => ({
  default: () => <div data-testid="report-schedule-tab" />,
}));
vi.mock("./components/RolePermissionTab/RolePermissionTab", () => ({
  default: () => <div data-testid="role-permission-tab" />,
}));

// Configure les droits utilisateur exposes au test.
const setupAuthMock = (permissions: string[]) => {
  vi.mocked(useAuth).mockReturnValue({
    hasPermission: (p: string) => permissions.includes(p),
    user: null,
    isAuthenticated: true,
    hasRole: () => false,
  } as ReturnType<typeof useAuth>);
};

// Prepare l'affichage lisible de settings.test.
const renderSettings = () => renderWithProviders(<Settings />);

describe("Settings", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should redirect to dashboard when user has no relevant permissions", () => {
    setupAuthMock([]);
    renderSettings();

    expect(
      screen.queryByText("settings.tabs.incidentTypes"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("settings.tabs.agencies"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("settings.tabs.services"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("settings.tabs.reportSchedules"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("settings.tabs.rolesPermissions"),
    ).not.toBeInTheDocument();
  });

  it("should show IncidentTypes, Agencies, Services and ReportSchedules tabs when user has SETTINGS_SYSTEM", () => {
    setupAuthMock(["SETTINGS_SYSTEM"]);
    renderSettings();

    expect(screen.getByText("settings.tabs.incidentTypes")).toBeInTheDocument();
    expect(screen.getByText("settings.tabs.agencies")).toBeInTheDocument();
    expect(screen.getByText("settings.tabs.services")).toBeInTheDocument();
    expect(
      screen.getByText("settings.tabs.reportSchedules"),
    ).toBeInTheDocument();
  });

  it("should not show RolesPermissions tab when user only has SETTINGS_SYSTEM", () => {
    setupAuthMock(["SETTINGS_SYSTEM"]);
    renderSettings();

    expect(
      screen.queryByText("settings.tabs.rolesPermissions"),
    ).not.toBeInTheDocument();
  });

  it("should show RolesPermissions tab when user has ROLE_CREATE", () => {
    setupAuthMock(["ROLE_CREATE"]);
    renderSettings();

    expect(
      screen.getByText("settings.tabs.rolesPermissions"),
    ).toBeInTheDocument();
  });

  it("should show RolesPermissions tab when user has ROLE_UPDATE", () => {
    setupAuthMock(["ROLE_UPDATE"]);
    renderSettings();

    expect(
      screen.getByText("settings.tabs.rolesPermissions"),
    ).toBeInTheDocument();
  });

  it("should show RolesPermissions tab when user has ROLE_DELETE", () => {
    setupAuthMock(["ROLE_DELETE"]);
    renderSettings();

    expect(
      screen.getByText("settings.tabs.rolesPermissions"),
    ).toBeInTheDocument();
  });

  it("should not show SETTINGS_SYSTEM tabs when user only has ROLE_CREATE", () => {
    setupAuthMock(["ROLE_CREATE"]);
    renderSettings();

    expect(
      screen.queryByText("settings.tabs.incidentTypes"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("settings.tabs.agencies"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("settings.tabs.services"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByText("settings.tabs.reportSchedules"),
    ).not.toBeInTheDocument();
  });

  it("should show all tabs when user has both SETTINGS_SYSTEM and ROLE permissions", () => {
    setupAuthMock([
      "SETTINGS_SYSTEM",
      "ROLE_CREATE",
      "ROLE_UPDATE",
      "ROLE_DELETE",
    ]);
    renderSettings();

    expect(screen.getByText("settings.tabs.incidentTypes")).toBeInTheDocument();
    expect(screen.getByText("settings.tabs.agencies")).toBeInTheDocument();
    expect(screen.getByText("settings.tabs.services")).toBeInTheDocument();
    expect(
      screen.getByText("settings.tabs.reportSchedules"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("settings.tabs.rolesPermissions"),
    ).toBeInTheDocument();
  });

  it("should render exactly 4 tabs when user only has SETTINGS_SYSTEM", () => {
    setupAuthMock(["SETTINGS_SYSTEM"]);
    renderSettings();

    const tabs = screen.getAllByRole("tab");
    expect(tabs).toHaveLength(4);
  });

  it("should render exactly 1 tab when user only has ROLE_CREATE", () => {
    setupAuthMock(["ROLE_CREATE"]);
    renderSettings();

    const tabs = screen.getAllByRole("tab");
    expect(tabs).toHaveLength(1);
  });

  it("should render exactly 5 tabs when user has SETTINGS_SYSTEM and ROLE_CREATE", () => {
    setupAuthMock(["SETTINGS_SYSTEM", "ROLE_CREATE"]);
    renderSettings();

    const tabs = screen.getAllByRole("tab");
    expect(tabs).toHaveLength(5);
  });
});
