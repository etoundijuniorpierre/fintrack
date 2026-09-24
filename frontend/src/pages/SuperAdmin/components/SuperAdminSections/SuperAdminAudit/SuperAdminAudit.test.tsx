// Tests frontend : verifie le comportement de super-administration audit.test.

import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import { SuperAdminAudit } from "./SuperAdminAudit";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: string) => fallback ?? key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

describe("SuperAdminAudit", () => {
  it("renders the CSV export controls", () => {
    renderWithProviders(
      <SuperAdminAudit
        loading={false}
        section={{ byAction: { USER_CREATE: 1 }, byStatus: { SUCCESS: 1 } }}
      />,
    );

    expect(
      screen.getByText("superAdmin.audit.exportSectionTitle"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("superAdmin.audit.exportButton"),
    ).toBeInTheDocument();
  });

  it("renders security insights (sensitive actions + permission changes)", () => {
    renderWithProviders(
      <SuperAdminAudit
        loading={false}
        section={{
          byAction: { ROLE_UPDATE: 1 },
          sensitiveCount: 3,
          recentSensitive: [
            {
              id: "a1",
              timestamp: "2026-05-28T10:00:00Z",
              username: "root",
              userId: "user-root",
              action: "ROLE_UPDATE",
              resourceId: "role-admin",
              ipAddress: "127.0.0.1",
              status: "SUCCESS",
            },
          ],
          permissionChangeHistory: [
            {
              id: "p1",
              timestamp: "2026-05-28T09:00:00Z",
              username: "admin",
              userId: "user-admin",
              action: "ROLE_DELETE",
              resourceId: "role-agent",
              ipAddress: "10.0.0.2",
              status: "SUCCESS",
            },
          ],
        }}
      />,
    );

    expect(
      screen.getByText("superAdmin.audit.sensitiveTitle"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("superAdmin.audit.permissionChangesTitle"),
    ).toBeInTheDocument();
    expect(screen.getByText("root")).toBeInTheDocument();
    expect(screen.getByText("admin")).toBeInTheDocument();
    expect(screen.getByText("user-root")).toBeInTheDocument();
    expect(screen.getByText("role-admin")).toBeInTheDocument();
    expect(screen.getByText("127.0.0.1")).toBeInTheDocument();
    expect(screen.getByText("user-admin")).toBeInTheDocument();
    expect(screen.getByText("role-agent")).toBeInTheDocument();
    expect(screen.getByText("10.0.0.2")).toBeInTheDocument();
  });
});
