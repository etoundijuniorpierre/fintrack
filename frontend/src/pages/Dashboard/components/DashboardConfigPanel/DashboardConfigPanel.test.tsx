// Tests frontend : verifie le comportement de tableau de bord configuration panel.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import DashboardConfigPanel from "./DashboardConfigPanel";
import { WidgetType, type DashboardConfig } from "../../../../types/dashboard";
import * as dashboardConfig from "../../../../utils/dashboard/config/dashboardConfig";
import {
  getVisibleWidgets,
  isWidgetAllowedInView,
} from "../../../../utils/dashboard/widgetPermissions/widgetPermissions";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../utils/dashboard/config/dashboardConfig", () => ({
  saveDashboardConfig: vi.fn(),
  resetDashboardConfig: vi.fn(),
}));

const ADMIN_PERMISSIONS = [
  "INCIDENT_VIEW_ALL",
  "INCIDENT_VALIDATE",
  "INCIDENT_RESOLVE",
  "USER_VIEW_ALL",
  "DASHBOARD_CONFIGURE",
];
// Definit les donnees de test agent permissions.
const AGENT_PERMISSIONS = ["INCIDENT_VIEW_OWN", "DASHBOARD_CONFIGURE"];

// Fabrique une fixture de test pour tableau de bord configuration panel.test.
const makeConfig = (visibleWidgets: WidgetType[]): DashboardConfig => ({
  userId: "user-1",
  visibleWidgets,
  lastUpdated: "2024-01-01T00:00:00.000Z",
});

const renderPanel = (
  overrides: Partial<{
    userId: string;
    userPermissions: string[];
    currentConfig: DashboardConfig | null;
    view: "own" | "agency" | "service" | "all";
    onClose: () => void;
    onSave: (config: DashboardConfig) => void;
  }> = {},
) => {
  const props = {
    userId: "user-1",
    userPermissions: ADMIN_PERMISSIONS,
    currentConfig: null,
    view: "all" as const,
    onClose: vi.fn(),
    onSave: vi.fn(),
    ...overrides,
  };
  return renderWithProviders(<DashboardConfigPanel {...props} />);
};

describe("DashboardConfigPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe("rendering", () => {
    it("should render the panel title", () => {
      renderPanel();
      expect(
        screen.getByText("dashboard.widgets.customize"),
      ).toBeInTheDocument();
    });

    it("should render Apply, Reset and Cancel buttons", () => {
      renderPanel();
      expect(screen.getByText("dashboard.widgets.apply")).toBeInTheDocument();
      expect(screen.getByText("dashboard.widgets.reset")).toBeInTheDocument();
      expect(screen.getByText("dashboard.widgets.cancel")).toBeInTheDocument();
    });

    it("should render checkboxes for all permitted widgets when user has admin permissions", () => {
      renderPanel({ userPermissions: ADMIN_PERMISSIONS });

      const checkboxes = screen.getAllByRole("checkbox");
      const expected = getVisibleWidgets(ADMIN_PERMISSIONS).filter((widget) =>
        isWidgetAllowedInView(widget, "all"),
      );
      expect(checkboxes).toHaveLength(expected.length);
    });

    it("should expose contextual help for every displayed metric", () => {
      renderPanel({ userPermissions: ADMIN_PERMISSIONS });
      const expected = getVisibleWidgets(ADMIN_PERMISSIONS).filter((widget) =>
        isWidgetAllowedInView(widget, "all"),
      );

      expected.forEach((widget) => {
        expect(
          screen.getByLabelText(`dashboard.kpi.help.${widget}`),
        ).toBeInTheDocument();
      });
    });

    it("should render checkboxes only for permitted incident widgets when user has agent permissions", () => {
      renderPanel({ userPermissions: AGENT_PERMISSIONS });

      const checkboxes = screen.getAllByRole("checkbox");
      const expected = getVisibleWidgets(AGENT_PERMISSIONS).filter((widget) =>
        isWidgetAllowedInView(widget, "all"),
      );
      expect(checkboxes).toHaveLength(expected.length);
    });
  });

  describe("widget visibility based on permissions", () => {
    it("should not show top services and top agencies checkboxes when user lacks INCIDENT_VIEW_ALL", () => {
      renderPanel({ userPermissions: AGENT_PERMISSIONS });

      expect(
        screen.queryByText(
          `dashboard.widgets.${WidgetType.INCIDENT_TOP_SERVICES}`,
        ),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByText(
          `dashboard.widgets.${WidgetType.INCIDENT_TOP_AGENCIES}`,
        ),
      ).not.toBeInTheDocument();
    });

    it("should show top services and top agencies checkboxes when user has INCIDENT_VIEW_ALL", () => {
      renderPanel({ userPermissions: ADMIN_PERMISSIONS });

      expect(
        screen.getByText(
          `dashboard.widgets.${WidgetType.INCIDENT_TOP_SERVICES}`,
        ),
      ).toBeInTheDocument();
      expect(
        screen.getByText(
          `dashboard.widgets.${WidgetType.INCIDENT_TOP_AGENCIES}`,
        ),
      ).toBeInTheDocument();
    });

    it("should show basic incident widget checkboxes for users with INCIDENT_VIEW_OWN", () => {
      renderPanel({ userPermissions: AGENT_PERMISSIONS });

      expect(
        screen.getByText(`dashboard.widgets.${WidgetType.INCIDENT_ACTIVE}`),
      ).toBeInTheDocument();
      expect(
        screen.getByText(`dashboard.widgets.${WidgetType.INCIDENT_CLOSED}`),
      ).toBeInTheDocument();
      expect(
        screen.getByText(`dashboard.widgets.${WidgetType.INCIDENT_TOTAL}`),
      ).toBeInTheDocument();
      expect(
        screen.queryByText(
          `dashboard.widgets.${WidgetType.INCIDENT_TOP_SERVICES}`,
        ),
      ).not.toBeInTheDocument();
    });

    it("should hide the efficiency scorecard outside agency and service views", () => {
      renderPanel({
        userPermissions: ["INCIDENT_VIEW_ALL"],
        view: "all",
      });

      expect(
        screen.queryByText(
          `dashboard.widgets.${WidgetType.INCIDENT_SCORECARD}`,
        ),
      ).not.toBeInTheDocument();
    });
  });

  describe("initial state from currentConfig", () => {
    it("should check all permitted widgets when currentConfig is null", () => {
      renderPanel({ userPermissions: AGENT_PERMISSIONS, currentConfig: null });

      const checkboxes = screen.getAllByRole("checkbox") as HTMLInputElement[];
      checkboxes.forEach((cb) => {
        expect(cb.checked).toBe(true);
      });
    });

    it("should pre-check only widgets from currentConfig that are permitted", () => {
      const config = makeConfig([
        WidgetType.INCIDENT_ACTIVE,
        WidgetType.INCIDENT_TOTAL,
      ]);
      renderPanel({
        userPermissions: AGENT_PERMISSIONS,
        currentConfig: config,
      });

      const checkboxes = screen.getAllByRole("checkbox") as HTMLInputElement[];
      const checkedBoxes = checkboxes.filter((cb) => cb.checked);
      expect(checkedBoxes.length).toBe(2);
    });

    it("should filter out top services widget from config when user lacks INCIDENT_VIEW_ALL", () => {
      const config = makeConfig([
        WidgetType.INCIDENT_ACTIVE,
        WidgetType.INCIDENT_TOP_SERVICES,
      ]);
      renderPanel({
        userPermissions: AGENT_PERMISSIONS,
        currentConfig: config,
      });

      expect(
        screen.queryByText(
          `dashboard.widgets.${WidgetType.INCIDENT_TOP_SERVICES}`,
        ),
      ).not.toBeInTheDocument();

      const checkboxes = screen.getAllByRole("checkbox") as HTMLInputElement[];
      const checkedBoxes = checkboxes.filter((cb) => cb.checked);
      expect(checkedBoxes.length).toBe(1);
    });
  });

  describe("Apply button", () => {
    it("should call saveDashboardConfig when Apply is clicked", async () => {
      const onSave = vi.fn();
      const onClose = vi.fn();
      renderPanel({ onSave, onClose });

      fireEvent.click(screen.getByText("dashboard.widgets.apply"));

      await waitFor(() => {
        expect(dashboardConfig.saveDashboardConfig).toHaveBeenCalled();
      });
    });

    it("should call onSave with the new config when Apply is clicked", async () => {
      const onSave = vi.fn();
      renderPanel({ onSave });

      fireEvent.click(screen.getByText("dashboard.widgets.apply"));

      await waitFor(() => {
        expect(onSave).toHaveBeenCalledWith(
          expect.objectContaining({
            userId: "user-1",
            visibleWidgets: expect.any(Array),
            lastUpdated: expect.any(String),
          }),
        );
      });
    });

    it("should call onClose when Apply is clicked", async () => {
      const onClose = vi.fn();
      renderPanel({ onClose });

      fireEvent.click(screen.getByText("dashboard.widgets.apply"));

      await waitFor(() => {
        expect(onClose).toHaveBeenCalled();
      });
    });

    it("should save only the selected widgets when some are unchecked", async () => {
      const onSave = vi.fn();
      renderPanel({ userPermissions: AGENT_PERMISSIONS, onSave });

      const checkboxes = screen.getAllByRole("checkbox");
      fireEvent.click(checkboxes[0]);

      fireEvent.click(screen.getByText("dashboard.widgets.apply"));

      await waitFor(() => {
        expect(onSave).toHaveBeenCalledWith(
          expect.objectContaining({
            visibleWidgets: expect.not.arrayContaining([
              WidgetType.INCIDENT_ACTIVE,
            ]),
          }),
        );
      });
    });
  });

  describe("Reset button", () => {
    it("should call resetDashboardConfig when Reset is clicked", async () => {
      renderPanel();

      fireEvent.click(screen.getByText("dashboard.widgets.reset"));

      await waitFor(() => {
        expect(dashboardConfig.resetDashboardConfig).toHaveBeenCalledWith(
          "user-1",
        );
      });
    });

    it("should call onSave with all permitted widgets after Reset is clicked", async () => {
      const onSave = vi.fn();
      renderPanel({ userPermissions: AGENT_PERMISSIONS, onSave });

      const checkboxes = screen.getAllByRole("checkbox");
      fireEvent.click(checkboxes[0]);

      fireEvent.click(screen.getByText("dashboard.widgets.reset"));

      await waitFor(() => {
        expect(onSave).toHaveBeenCalledWith(
          expect.objectContaining({
            visibleWidgets: expect.arrayContaining([
              WidgetType.INCIDENT_ACTIVE,
              WidgetType.INCIDENT_CLOSED,
              WidgetType.INCIDENT_REJECTED,
              WidgetType.INCIDENT_TOTAL,
              WidgetType.INCIDENT_AVG_RESOLUTION,
              WidgetType.INCIDENT_TYPE_DISTRIBUTION,
              WidgetType.INCIDENT_CRITICALITY_DISTRIBUTION,
              WidgetType.INCIDENT_RECENT_ACTIVITY,
            ]),
          }),
        );
      });
    });

    it("should call onClose when Reset is clicked", async () => {
      const onClose = vi.fn();
      renderPanel({ onClose });

      fireEvent.click(screen.getByText("dashboard.widgets.reset"));

      await waitFor(() => {
        expect(onClose).toHaveBeenCalled();
      });
    });
  });

  describe("Cancel button", () => {
    it("should call onClose when Cancel is clicked", () => {
      const onClose = vi.fn();
      renderPanel({ onClose });

      fireEvent.click(screen.getByText("dashboard.widgets.cancel"));

      expect(onClose).toHaveBeenCalled();
    });

    it("should not call saveDashboardConfig when Cancel is clicked", () => {
      renderPanel();

      fireEvent.click(screen.getByText("dashboard.widgets.cancel"));

      expect(dashboardConfig.saveDashboardConfig).not.toHaveBeenCalled();
    });

    it("should not call onSave when Cancel is clicked", () => {
      const onSave = vi.fn();
      renderPanel({ onSave });

      fireEvent.click(screen.getByText("dashboard.widgets.cancel"));

      expect(onSave).not.toHaveBeenCalled();
    });
  });

  describe("checkbox toggle", () => {
    it("should toggle a widget off when its checkbox is unchecked", () => {
      renderPanel({ userPermissions: AGENT_PERMISSIONS });

      const checkboxes = screen.getAllByRole("checkbox") as HTMLInputElement[];
      expect(checkboxes[0].checked).toBe(true);

      fireEvent.click(checkboxes[0]);

      expect(checkboxes[0].checked).toBe(false);
    });

    it("should toggle a widget back on when its checkbox is re-checked", () => {
      renderPanel({ userPermissions: AGENT_PERMISSIONS });

      const checkboxes = screen.getAllByRole("checkbox") as HTMLInputElement[];
      fireEvent.click(checkboxes[0]);
      expect(checkboxes[0].checked).toBe(false);

      fireEvent.click(checkboxes[0]);
      expect(checkboxes[0].checked).toBe(true);
    });
  });
});
