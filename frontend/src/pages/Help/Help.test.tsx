// Tests frontend : verifie le comportement de help.test.

import { screen, fireEvent } from "@testing-library/react";
import { afterEach, describe, it, expect, vi } from "vitest";
import Help from "./Help";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import * as authHooks from "../../hooks/auth/useAuth";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: { defaultValue?: string }) =>
      opts?.defaultValue ?? key,
  }),
  Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
  initReactI18next: { type: "3rdParty", init: vi.fn() },
  I18nextProvider: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
}));

vi.mock("../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => {
    const perms = ["INCIDENT_CREATE", "INCIDENT_UPDATE", "FOO_UNKNOWN"];
    return {
      user: {
        id: "u1",
        username: "jdoe",
        roles: ["AGENT"],
        permissions: perms,
      },
      hasPermission: (perm: string) => perms.includes(perm),
    };
  }),
}));

vi.mock("../../../../api/incident/incidentApi/incidentApi", () => ({
  incidentApi: {
    getIncidentTypes: vi.fn().mockResolvedValue([]),
  },
}));

describe("Help", () => {
  afterEach(() => {
    vi.mocked(authHooks.useAuth).mockImplementation(() => {
      const perms = ["INCIDENT_CREATE", "INCIDENT_UPDATE", "FOO_UNKNOWN"];
      return {
        user: {
          id: "u1",
          username: "jdoe",
          roles: ["AGENT"],
          permissions: perms,
        },
        isAuthenticated: true,
        hasRole: () => false,
        hasPermission: (perm: string) => perms.includes(perm),
      };
    });
  });

  it("renders the page header and the tabs that the user has permission for", () => {
    renderWithProviders(<Help />);
    expect(screen.getByText("help.title")).toBeInTheDocument();

    const accessibleTabs = ["workflow", "fields", "statuses", "causes", "faq"];
    accessibleTabs.forEach((key) => {
      expect(
        screen.getByText(`help.sections.${key}.title`),
      ).toBeInTheDocument();
    });

    expect(
      screen.queryByText("help.sections.settings.title"),
    ).not.toBeInTheDocument();
  });

  it("lists incident form fields when field guide tab is active", () => {
    renderWithProviders(<Help />);
    fireEvent.click(screen.getByText("help.sections.fields.title"));
    expect(screen.getByText("help.fields.title.label")).toBeInTheDocument();
  });

  it("displays statuses when statuses tab is clicked", () => {
    renderWithProviders(<Help />);
    fireEvent.click(screen.getByText("help.sections.statuses.title"));
    expect(screen.getByText("incidents.status.OPEN")).toBeInTheDocument();
  });

  it("dynamically renders paragraphs based on permissions in workflow", () => {
    renderWithProviders(<Help />);
    expect(
      screen.getByText("help.sections.workflow.items.creation.title"),
    ).toBeInTheDocument();
  });

  it("hides privileged help tabs when the permissions are absent", () => {
    const permissions = ["INCIDENT_VIEW_OWN"];
    vi.mocked(authHooks.useAuth).mockReturnValue({
      user: {
        id: "u2",
        username: "agent",
        roles: ["CUSTOM_ROLE"],
        permissions,
      },
      hasPermission: (permission: string) => permissions.includes(permission),
    } as ReturnType<typeof authHooks.useAuth>);

    renderWithProviders(<Help />);

    expect(screen.getByText("help.sections.managing.title")).toBeInTheDocument();
    expect(screen.getByText("help.sections.dashboard.title")).toBeInTheDocument();
    expect(screen.queryByText("help.sections.fields.title")).not.toBeInTheDocument();
    expect(screen.queryByText("help.sections.types.title")).not.toBeInTheDocument();
    expect(screen.queryByText("help.sections.users.title")).not.toBeInTheDocument();
    expect(screen.queryByText("help.sections.audit.title")).not.toBeInTheDocument();
  });
});
