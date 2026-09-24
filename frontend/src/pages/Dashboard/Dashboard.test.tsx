// Tests frontend : verifie le comportement de dashboard.test.

import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, screen } from "@testing-library/react";
import Dashboard from "./Dashboard";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import { useDashboardMetrics } from "../../hooks/dashboard/useDashboardMetrics/useDashboardMetrics";
import { PeriodType } from "../../types/dashboard";
import { STORAGE_KEYS } from "../../utils/constants";
vi.mock("react-i18next", async (importOriginal) => {
  const actual = await importOriginal<typeof import("react-i18next")>();
  return {
    ...actual,
    useTranslation: () => ({
      t: (key: string) => key,
      i18n: { changeLanguage: vi.fn() },
    }),
  };
});

const adminPermissions = [
  "INCIDENT_VIEW_ALL",
  "INCIDENT_VALIDATE",
  "INCIDENT_RESOLVE",
  "USER_VIEW_ALL",
  "DASHBOARD_CONFIGURE",
];

vi.mock("../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({
    user: {
      id: "user-1",
      username: "admin",
      roles: ["ADMIN"],
      permissions: adminPermissions,
    },
    isAuthenticated: true,
    hasPermission: (permission: string) =>
      adminPermissions.includes(permission),
    hasRole: () => true,
  })),
}));

vi.mock(
  "../../hooks/dashboard/useDashboardMetrics/useDashboardMetrics",
  () => ({
    useDashboardMetrics: vi.fn(() => ({
      data: {
        activeIncidents: 10,
        closedIncidents: 50,
        rejectedIncidents: 3,
        blockedIncidents: 0,
        totalIncidents: 63,
        avgClosureHours: 12.5,
        medianClosureHours: 9.0,
        p90ClosureHours: 30.0,
        closureSampleSize: 40,
        avgResolutionHours: 10.5,
        medianResolutionHours: 8.0,
        p90ResolutionHours: 26.0,
        resolutionSampleSize: 35,
        slaComplianceRate: 80,
        slaCompliantCount: 16,
        slaDenominator: 20,
        inflow: 63,
        outflow: 57,
        exitsClosed: 50,
        exitsRejected: 3,
        exitsCancelled: 4,
        backlogReEntries: 2,
        netBacklog: 8,
        distributionByType: {},
        distributionByCriticality: {},
        recentActivities: [],
        assignedToMe: 2,
        topServices: [],
      },
      isLoading: false,
      isError: false,
    })),
  }),
);

const mockUseDashboardMetrics = vi.mocked(useDashboardMetrics);

vi.mock("../../utils/dashboard/config/dashboardConfig", () => ({
  loadDashboardConfig: vi.fn(() => null),
  saveDashboardConfig: vi.fn(),
  resetDashboardConfig: vi.fn(),
}));

// Prepare l'affichage lisible de dashboard.test.
const renderDashboard = () => renderWithProviders(<Dashboard />);

describe("Dashboard", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it("renders title and permissioned KPI cards", () => {
    renderDashboard();

    expect(screen.getByText("dashboard.title")).toBeInTheDocument();
    expect(
      screen.getByText("dashboard.kpi.active_incidents"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("dashboard.kpi.closed_incidents"),
    ).toBeInTheDocument();
    expect(screen.getByText("dashboard.kpi.closure_hours")).toBeInTheDocument();
    expect(
      screen.getByText("dashboard.kpi.resolution_hours"),
    ).toBeInTheDocument();
    // La mediane reste la valeur principale, dite en toutes lettres ; moyenne, p90
    // et duree hors attente ne s'affichent plus a cote d'elle.
    expect(screen.getAllByText("common.duration.hours").length).toBe(2);
    expect(screen.getAllByText("dashboard.kpi.measured_on").length).toBe(2);
  });

  it("shows a deadline compliance count instead of a percentage", () => {
    renderDashboard();

    expect(
      screen.getByText("dashboard.kpi.sla_compliance"),
    ).toBeInTheDocument();
    // « 16 » sur une base annoncee, plutot qu'un « 80 % » a reconvertir.
    expect(screen.getByText("16")).toBeInTheDocument();
    expect(
      screen.getByText("dashboard.kpi.sla_compliance_detail"),
    ).toBeInTheDocument();
  });

  it("breaks the flow card down into each movement", () => {
    renderDashboard();

    // Le solde annonce en tete est exactement la somme des mouvements detailles.
    expect(screen.getByText("+8")).toBeInTheDocument();
    expect(screen.getByText("dashboard.kpi.flow_up")).toBeInTheDocument();
    expect(
      screen.getByText(
        [
          "dashboard.kpi.flow_declared",
          "dashboard.kpi.flow_reopened",
          "dashboard.kpi.flow_closed",
          "dashboard.kpi.flow_rejected",
          "dashboard.kpi.flow_cancelled",
        ].join(" · "),
      ),
    ).toBeInTheDocument();
  });

  it("loads dashboard metrics with the last 30 days by default", () => {
    renderDashboard();

    expect(mockUseDashboardMetrics).toHaveBeenCalledWith(
      "all",
      expect.objectContaining({ period: PeriodType.LAST_30_DAYS }),
    );
  });

  it("restores the period saved for the authenticated user", () => {
    localStorage.setItem(
      `${STORAGE_KEYS.DASHBOARD_PERIOD}:user-1`,
      JSON.stringify({ period: PeriodType.THIS_MONTH }),
    );

    renderDashboard();

    expect(mockUseDashboardMetrics).toHaveBeenCalledWith(
      "all",
      expect.objectContaining({ period: PeriodType.THIS_MONTH }),
    );
  });

  it("renders the new director KPI cards for an admin (all view)", () => {
    renderDashboard();

    expect(
      screen.getByText("dashboard.kpi.sla_breach_now"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("dashboard.kpi.inflow_outflow"),
    ).toBeInTheDocument();
    // L'indice d'efficacite n'a de sens qu'en perimetre agence/service : masque en vue globale brute.
    expect(
      screen.queryByText("dashboard.kpi.scorecard"),
    ).not.toBeInTheDocument();
  });

  it("renders recent activity only when the widget is enabled", () => {
    renderDashboard();

    expect(
      screen.getByText("dashboard.cards.recent_activity"),
    ).toBeInTheDocument();
  });

  it("shows the customize action when DASHBOARD_CONFIGURE is present", () => {
    renderDashboard();

    expect(
      screen.getByLabelText("dashboard.widgets.customize"),
    ).toBeInTheDocument();
  });

  it("applies dashboard customization immediately without a page refresh", () => {
    renderDashboard();

    expect(
      screen.getByText("dashboard.kpi.closed_incidents"),
    ).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText("dashboard.widgets.customize"));
    const closedCheckbox = screen.getByRole("checkbox", {
      name: "dashboard.widgets.INCIDENT_CLOSED",
    });
    fireEvent.click(closedCheckbox);

    fireEvent.click(screen.getByText("dashboard.widgets.apply"));

    expect(
      screen.getByText("dashboard.kpi.active_incidents"),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("dashboard.kpi.closed_incidents"),
    ).not.toBeInTheDocument();
  });
});
