// Tests frontend : verifie le comportement de incident table.test.

import { screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import IncidentTable from "./IncidentTable";
import { useAuth } from "../../../../hooks/auth/useAuth";
import { makeIncidentSummary } from "../../../../mocks/incident/incidentFixtures";
import { incidentNavigation } from "../../../../utils/navigation/incidents/incidents";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

vi.mock("react-window", () => ({
  FixedSizeList: ({
    children,
    itemCount,
    itemSize,
  }: {
    children: (props: {
      index: number;
      style: React.CSSProperties;
    }) => React.ReactNode;
    itemCount: number;
    itemSize: number;
  }) => (
    <div data-testid="virtualized-list">
      {Array.from({ length: Math.min(itemCount, 5) }, (_, i) =>
        children({ index: i, style: { height: itemSize, top: i * itemSize } }),
      )}
    </div>
  ),
}));

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock("../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(),
}));

vi.mock("../../../../hooks/incident/useIncidents/useIncidents", () => ({
  useDeleteIncident: () => ({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("../../../../utils/navigation/incidents/incidents", () => ({
  incidentNavigation: { navigateToIncidentDetail: vi.fn() },
  incidentPathIdentifier: (incident: { id: string; reference?: string }) =>
    incident.reference ?? incident.id,
}));

vi.mock("../../../../utils/formatters/formatters", () => ({
  formatDate: () => "2024-01-01",
  formatDateTime: () => "2024-01-01",
  formatUserName: (u: { firstName?: string; lastName?: string } | null) =>
    u ? `${u.firstName ?? ""} ${u.lastName ?? ""}`.trim() : "—",
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  I18nextProvider: ({ children }: { children: React.ReactNode }) => (
    <>{children}</>
  ),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

// Definit les donnees de test test inincidents.
const TEST_INCIDENTS = [
  makeIncidentSummary({ id: "incident-1", title: "First Incident" }),
  makeIncidentSummary({ id: "incident-2", title: "Second Incident" }),
];

// Prepare l'affichage lisible de incident table.test.
const renderTable = (incidents = TEST_INCIDENTS) =>
  renderWithProviders(<IncidentTable incidents={incidents} loading={false} />);

// Prepare tag color pour incident table.test.
const tagColor = (text: string) => {
  const el = screen.getByText(text).closest(".ant-tag") as HTMLElement;
  return el.className + (el.getAttribute("style") ?? "");
};

// Fabrique un mock d'authentification avec ou sans permission.
const mockAuthWithPermission = (hasPermission: boolean) => {
  vi.mocked(useAuth).mockReturnValue({
    user: null,
    isAuthenticated: false,
    hasRole: vi.fn().mockReturnValue(false),
    hasPermission: vi.fn().mockReturnValue(hasPermission),
  });
};

describe("IncidentTable", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthWithPermission(true);
  });

  it("should render the table with correct structure when incidents are provided", () => {
    renderTable();
    expect(screen.getByRole("table")).toBeInTheDocument();
  });

  it("should render all expected column headers when table is displayed", () => {
    renderTable();
    expect(screen.getAllByText("incidents.table.title").length).toBeGreaterThan(
      0,
    );
    expect(screen.getAllByText("incidents.table.type").length).toBeGreaterThan(
      0,
    );
    expect(
      screen.getAllByText("incidents.table.criticality").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("incidents.table.status").length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText("incidents.table.sla").length).toBeGreaterThan(
      0,
    );
    expect(
      screen.getAllByText("incidents.table.created_by").length,
    ).toBeGreaterThan(0);
    expect(screen.getAllByText("incidents.table.age").length).toBeGreaterThan(
      0,
    );
    expect(
      screen.getAllByText("incidents.table.agency").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("incidents.table.reference").length,
    ).toBeGreaterThan(0);
  });

  it("should display incident data correctly when incident has all fields populated", () => {
    const incident = makeIncidentSummary({
      title: "Network Outage",
      type: {
        id: "type-1",
        name: "NETWORK",
        displayName: "Network Issue",
        isActive: true,
        description: "",
        slaHours: 8,
        requiresValidation: true,
        requiresCauseAnalysis: false,
        emailNotificationsEnabled: false,
        closerRoles: ["ASSIGNEE"],
        validatorScope: "AGENCY_MANAGER",
      },
      createdBy: {
        id: "user-1",
        username: "jsmith",
        firstName: "Jane",
        lastName: "Smith",
        email: "j@s.com",
      },
      agency: { id: "agency-1", name: "West Agency", code: "WA" },
    });
    renderWithProviders(
      <IncidentTable incidents={[incident]} loading={false} />,
    );

    expect(screen.getByText("Network Outage")).toBeInTheDocument();
    expect(screen.getByText("Network Issue")).toBeInTheDocument();
    expect(screen.getByText("Jane Smith")).toBeInTheDocument();
    expect(screen.getByText("West Agency")).toBeInTheDocument();
  });

  it("should render LOW criticality tag with green color when criticality is LOW", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ criticality: "LOW" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.criticality.LOW")).toContain("green");
  });

  it("should render MEDIUM criticality tag with orange color when criticality is MEDIUM", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ criticality: "MEDIUM" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.criticality.MEDIUM")).toContain("orange");
  });

  it("should render HIGH criticality tag with red color when criticality is HIGH", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ criticality: "HIGH" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.criticality.HIGH")).toContain("red");
  });

  it("should render CRITICAL criticality tag with red color when criticality is CRITICAL", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ criticality: "CRITICAL" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.criticality.CRITICAL")).toContain("red");
  });

  it("should render OPEN status tag with red color when status is OPEN", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ status: "OPEN" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.status.OPEN")).toContain("red");
  });

  it("should render PENDING_VALIDATION status tag with gold color when status is PENDING_VALIDATION", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ status: "PENDING_VALIDATION" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.status.PENDING_VALIDATION")).toContain("gold");
  });

  it("should render VALIDATED status tag with blue color when status is VALIDATED", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ status: "VALIDATED" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.status.VALIDATED")).toContain("blue");
  });

  it("should render TRANSFERRED status tag with blue color when status is TRANSFERRED", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ status: "TRANSFERRED" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.status.TRANSFERRED")).toContain("blue");
  });

  it("should render IN_PROGRESS status tag with gold color when status is IN_PROGRESS", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ status: "IN_PROGRESS" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.status.IN_PROGRESS")).toContain("gold");
  });

  it("should render RESOLVED status tag with green color when status is RESOLVED", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ status: "RESOLVED" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.status.RESOLVED")).toContain("green");
  });

  it("should render CLOSED status tag with green color when status is CLOSED", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ status: "CLOSED" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.status.CLOSED")).toContain("green");
  });

  it("should render REJECTED status tag with red color when status is REJECTED", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ status: "REJECTED" })]}
        loading={false}
      />,
    );
    expect(tagColor("incidents.status.REJECTED")).toContain("red");
  });

  it("should render the incident reference code", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ reference: "FT-I-2026-0007" })]}
        loading={false}
      />,
    );
    expect(screen.getByText("FT-I-2026-0007")).toBeInTheDocument();
  });

  it("should render a dash when the incident has no reference yet", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ reference: undefined })]}
        loading={false}
      />,
    );
    expect(screen.getAllByText("-").length).toBeGreaterThan(0);
  });

  it("should navigate to incident detail when row is clicked", () => {
    renderTable();
    fireEvent.click(screen.getByText("First Incident").closest("tr")!);
    expect(incidentNavigation.navigateToIncidentDetail).toHaveBeenCalledWith(
      mockNavigate,
      "incident-1",
    );
  });

  it("should render the formatted created_at date when createdAt is present", () => {
    renderWithProviders(
      <IncidentTable
        incidents={[makeIncidentSummary({ createdAt: "2025-06-01T00:00:00Z" })]}
        loading={false}
      />,
    );
    expect(screen.getByText("2024-01-01")).toBeInTheDocument();
  });

  it("should render an empty table when incident list is empty", () => {
    renderWithProviders(<IncidentTable incidents={[]} loading={false} />);
    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.queryByText("First Incident")).not.toBeInTheDocument();
    expect(screen.queryByText("Second Incident")).not.toBeInTheDocument();
  });

});
