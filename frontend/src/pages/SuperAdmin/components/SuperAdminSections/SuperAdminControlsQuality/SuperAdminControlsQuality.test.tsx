// Tests frontend : verifie le comportement de super-administration controles quality.test.

import { describe, expect, it, vi } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import { SuperAdminControlsQuality } from "./SuperAdminControlsQuality";
import { APP_ROUTES } from "../../../../../utils/constants";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: string | Record<string, unknown>) =>
      typeof opts === "string" ? opts : key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

describe("SuperAdminControlsQuality", () => {
  it("renders inline detail items for quality issues", () => {
    renderWithProviders(
      <SuperAdminControlsQuality
        loading={false}
        navigate={vi.fn()}
        section={{
          permissions: {},
          dataQuality: {
            issues: [
              { key: "reports-without-file", count: 1, severity: "medium" },
              {
                key: "invalid-recipient-notifications",
                count: 1,
                severity: "medium",
              },
            ],
            reportsWithoutFile: [
              { id: "report-1", name: "Missing file report" },
            ],
            invalidRecipients: [{ id: "notif-1", name: "bad-recipient" }],
          },
        }}
      />,
    );

    expect(screen.getByText("Missing file report")).toBeInTheDocument();
    expect(screen.getByText("bad-recipient")).toBeInTheDocument();
  });

  it("navigates to settings with hash #agencies for agencies-without-head", () => {
    const mockNavigate = vi.fn();
    renderWithProviders(
      <SuperAdminControlsQuality
        loading={false}
        navigate={mockNavigate}
        section={{
          permissions: {},
          dataQuality: {
            issues: [
              { key: "agencies-without-head", count: 2, severity: "low" },
            ],
            agenciesWithoutHead: [
              { id: "a1", name: "Agence A" },
              { id: "a2", name: "Agence B" },
            ],
          },
        }}
      />,
    );

    const viewButtons = screen.getAllByText(
      "superAdmin.quality.viewItemsButton",
    );
    fireEvent.click(viewButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith(
      `${APP_ROUTES.SETTINGS}?missingHead=true#agencies`,
    );
  });

  it("navigates to settings with hash #services for services-without-head", () => {
    const mockNavigate = vi.fn();
    renderWithProviders(
      <SuperAdminControlsQuality
        loading={false}
        navigate={mockNavigate}
        section={{
          permissions: {},
          dataQuality: {
            issues: [
              { key: "services-without-head", count: 1, severity: "low" },
            ],
            servicesWithoutHead: [{ id: "s1", name: "Service X" }],
          },
        }}
      />,
    );

    const viewButtons = screen.getAllByText(
      "superAdmin.quality.viewItemsButton",
    );
    fireEvent.click(viewButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith(
      `${APP_ROUTES.SETTINGS}?missingHead=true#services`,
    );
  });

  it("navigates to settings with hash #incidentTypes for unused-incident-types", () => {
    const mockNavigate = vi.fn();
    renderWithProviders(
      <SuperAdminControlsQuality
        loading={false}
        navigate={mockNavigate}
        section={{
          permissions: {},
          dataQuality: {
            issues: [
              { key: "unused-incident-types", count: 1, severity: "low" },
            ],
            unusedIncidentTypes: [{ id: "it1", name: "OLD_TYPE" }],
          },
        }}
      />,
    );

    const viewButtons = screen.getAllByText(
      "superAdmin.quality.viewItemsButton",
    );
    fireEvent.click(viewButtons[0]);
    expect(mockNavigate).toHaveBeenCalledWith(
      `${APP_ROUTES.SETTINGS}?unused=true#incidentTypes`,
    );
  });
});
