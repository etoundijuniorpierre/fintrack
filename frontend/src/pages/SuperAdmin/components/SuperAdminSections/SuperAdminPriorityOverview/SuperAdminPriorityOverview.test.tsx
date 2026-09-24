// Tests frontend : verifie le comportement de super-administration priorite overview.test.

import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import { SuperAdminPriorityOverview } from "./SuperAdminPriorityOverview";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: string) => fallback ?? key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

describe("SuperAdminPriorityOverview", () => {
  it("renders governance and service health information", () => {
    renderWithProviders(
      <SuperAdminPriorityOverview
        loading={false}
        navigate={vi.fn()}
        governance={{
          agencies: 2,
          services: 3,
          activeUsers: 8,
          byStatus: { IN_PROGRESS: 2 },
          byCriticality: { CRITICAL: 1 },
          byAgency: { Finstar: 2 },
          byService: { IT: 2 },
          superAdminKpis: { criticalAnomalies: 1, failedEmails: 0 },
        }}
        health={[
          {
            key: "user",
            label: "User service",
            baseUrl: "http://user-service",
            endpoint: "http://user-service/actuator/health",
            status: "UP",
            recentErrors: 0,
          },
        ]}
      />,
    );

    expect(screen.getByText("User service")).toBeInTheDocument();
    expect(
      screen.getByText("superAdmin.sections.governance"),
    ).toBeInTheDocument();
  });
});
