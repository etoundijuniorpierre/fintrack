// Tests frontend : verifie le comportement de super-administration operations.test.

import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import { SuperAdminOperations } from "./SuperAdminOperations";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: string | Record<string, unknown>) =>
      typeof opts === "string" ? opts : key,
    i18n: { language: "fr" },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

describe("SuperAdminOperations", () => {
  it("renders failed notification diagnostics", () => {
    renderWithProviders(
      <SuperAdminOperations
        loading={false}
        section={{
          notifications: {
            byChannel: {
              EMAIL: { total: 1, sent: 0, failed: 1, pending: 0 },
              INTERNAL: { total: 0, sent: 0, failed: 0, pending: 0 },
            },
            failedNotifications: [
              {
                id: "notif-1",
                type: "EMAIL",
                recipient: "user@example.com",
                subject: "Report",
                diagnosis: "DELIVERY_FAILED",
                recommendedAction: "CHECK_PROVIDER_OR_TEMPLATE",
                retryCount: 1,
                maxRetry: 5,
              },
            ],
            invalidRecipients: [],
          },
        }}
      />,
    );

    expect(screen.getByText("user@example.com")).toBeInTheDocument();
    expect(screen.getByText("DELIVERY_FAILED")).toBeInTheDocument();
  });
});
