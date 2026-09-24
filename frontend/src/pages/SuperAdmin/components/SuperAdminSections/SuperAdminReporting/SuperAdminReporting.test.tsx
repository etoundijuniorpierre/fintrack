// Tests frontend : verifie le comportement de super-administration reporting.test.

import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import { SuperAdminReporting } from "./SuperAdminReporting";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: string | Record<string, unknown>) =>
      typeof opts === "string" ? opts : key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

describe("SuperAdminReporting", () => {
  it("renders stuck (pending), failed and missing-file report lists", () => {
    renderWithProviders(
      <SuperAdminReporting
        loading={false}
        section={{
          total: 3,
          failed: 1,
          pending: 1,
          // createdAt ancien => considere bloque => affiche dans la table des bloques.
          pendingReports: [
            {
              id: "pending-1",
              name: "Pending report",
              status: "GENERATING",
              createdAt: "2020-01-01T00:00:00Z",
            },
          ],
          failedReports: [
            {
              id: "failed-1",
              name: "Failed report",
              status: "FAILED",
              errorMessage: "boom",
            },
          ],
          availableWithoutFileReports: [
            {
              id: "missing-1",
              name: "Missing file report",
              status: "AVAILABLE",
            },
          ],
        }}
      />,
    );

    expect(screen.getByText("Pending report")).toBeInTheDocument();
    expect(screen.getByText("Failed report")).toBeInTheDocument();
    expect(screen.getByText("Missing file report")).toBeInTheDocument();
  });
});
