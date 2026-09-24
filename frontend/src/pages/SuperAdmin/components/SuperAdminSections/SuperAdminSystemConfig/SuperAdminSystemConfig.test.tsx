// Tests frontend : verifie le comportement de super-administration systeme config.test.

import { describe, expect, it, vi } from "vitest";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { QueryClient } from "@tanstack/react-query";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import { SuperAdminSystemConfig } from "./SuperAdminSystemConfig";
import { superAdminApi } from "../../../../../api/superAdmin";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: string) => fallback ?? key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../../api/superAdmin", () => ({
  superAdminApi: {
    updateThresholds: vi.fn().mockResolvedValue({}),
    triggerBackup: vi.fn(),
  },
}));

describe("SuperAdminSystemConfig", () => {
  it("offers admin by default and saves the service manager choice", async () => {
    renderWithProviders(
      <SuperAdminSystemConfig loading={false}
        section={{ businessThresholds: { serviceManagerSelfValidationEnabled: 0 } }} />,
    );
    expect(screen.getByText("superAdmin.config.selfValidationOptions.admin")).toBeInTheDocument();
    fireEvent.mouseDown(screen.getByRole("combobox"));
    fireEvent.click(screen.getByText("superAdmin.config.selfValidationOptions.serviceManager"));
    fireEvent.click(screen.getByText("superAdmin.config.saveThresholds"));
    fireEvent.click(await screen.findByRole("button", { name: "OK" }));
    await waitFor(() => expect(superAdminApi.updateThresholds).toHaveBeenCalledWith({
      serviceManagerSelfValidationEnabled: 1,
    }));
  });

  it("renders useful threshold fields", () => {
    renderWithProviders(
      <SuperAdminSystemConfig
        loading={false}
        section={{
          businessThresholds: {
            defaultSlaHours: 24,
            criticalIncidentHours: 4,
            slaReminderIntervalHours: 24,
            maxTransfersBeforeAlert: 2,
          },
        }}
      />,
    );

    expect(
      screen.getByText("superAdmin.config.thresholdLabels.defaultSlaHours"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("superAdmin.config.saveThresholds"),
    ).toBeInTheDocument();
  });

  it("should invalidate incident caches after thresholds are saved so derived flags refresh", async () => {
    // Les drapeaux derives des incidents (isMaxReopenReached, isReopenExpired, SLA)
    // dependent de ces seuils : la sauvegarde doit invalider les caches d'incidents.
    const invalidateSpy = vi.spyOn(
      QueryClient.prototype,
      "invalidateQueries",
    );

    renderWithProviders(
      <SuperAdminSystemConfig
        loading={false}
        section={{ businessThresholds: { defaultSlaHours: 24 } }}
      />,
    );

    const input = screen.getByLabelText(
      "superAdmin.config.thresholdLabels.defaultSlaHours",
    );
    fireEvent.change(input, { target: { value: "48" } });
    fireEvent.blur(input);

    fireEvent.click(screen.getByText("superAdmin.config.saveThresholds"));

    // Confirmer la modale de diff.
    const okButton = await screen.findByRole("button", { name: "OK" });
    fireEvent.click(okButton);

    await waitFor(() =>
      expect(vi.mocked(superAdminApi.updateThresholds)).toHaveBeenCalledWith({
        defaultSlaHours: 48,
      }),
    );
    await waitFor(() =>
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["incidents"] }),
    );

    invalidateSpy.mockRestore();
  });
});
