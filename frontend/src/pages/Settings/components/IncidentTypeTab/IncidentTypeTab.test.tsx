// Tests frontend : verifie le comportement de incident type tab.test.

import { screen, fireEvent, waitFor, within } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import IncidentTypeTab from "./IncidentTypeTab";
import { useAuth } from "../../../../hooks/auth/useAuth";
import type { IncidentTypeConfigResponse } from "../../../../api/settings/types";
import { APP_ROUTES } from "../../../../utils/constants";

const mockNavigate = vi.fn();
let mockSearchParams = new URLSearchParams();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useSearchParams: () => [mockSearchParams],
  };
});

vi.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({ hasPermission: () => true })),
}));

const { mockDeleteIncidentType } = vi.hoisted(() => ({
  mockDeleteIncidentType: vi.fn(),
}));

vi.mock("../../../../hooks/settings", () => ({
  useSettingsIncidentTypes: () => ({
    data: INCIDENT_TYPES,
    isLoading: false,
  }),
  useDeleteIncidentType: () => ({
    mutate: mockDeleteIncidentType,
    isPending: false,
  }),
  useDepartments: () => ({ data: [], isLoading: false }),
}));

vi.mock("../../../../hooks/user", () => ({
  useUsers: () => ({ data: [], isLoading: false }),
}));

// Fabrique une fixture de test pour incident type tab.test.
const makeIncidentType = (
  overrides: Partial<IncidentTypeConfigResponse> = {},
): IncidentTypeConfigResponse => ({
  id: "type-1",
  name: "TECHNICAL",
  displayName: "Technical Issue",
  description: "A technical incident",
  isActive: true,
  slaHours: 8,
  requiresValidation: true,
  requiresCauseAnalysis: false,
  emailNotificationsEnabled: false,
  closerRoles: ["ASSIGNEE"],
  validatorScope: "AGENCY_MANAGER",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

const INCIDENT_TYPES: IncidentTypeConfigResponse[] = [
  makeIncidentType({
    id: "type-1",
    name: "TECHNICAL",
    displayName: "Technical Issue",
    isActive: true,
  }),
  makeIncidentType({
    id: "type-2",
    name: "NETWORK",
    displayName: "Network Outage",
    isActive: false,
    slaHours: undefined,
    defaultTargetService: { id: "svc-1", name: "IT Department" },
  }),
  makeIncidentType({
    id: "type-3",
    name: "PERSONAL",
    displayName: "Personal Issue",
    isActive: true,
    defaultTargetUser: {
      id: "usr-1",
      username: "jdoe",
      firstName: "John",
      lastName: "Doe",
    },
  }),
];

// Fabrique un mock d'authentification avec ou sans permission.
const mockAuthWithPermission = (hasPermission: boolean) => {
  vi.mocked(useAuth).mockReturnValue({
    user: { id: "user-1", username: "admin", roles: [], permissions: [] },
    isAuthenticated: true,
    hasRole: () => false,
    hasPermission: () => hasPermission,
  });
};

// Prepare l'affichage lisible de incident type tab.test.
const renderTab = () => renderWithProviders(<IncidentTypeTab />);

describe("IncidentTypeTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSearchParams = new URLSearchParams();
    mockAuthWithPermission(true);
  });

  it("should render the table with correct columns when component mounts", () => {
    renderTab();

    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(
      screen.getAllByText("settings.incidentTypes.table.displayName").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.incidentTypes.table.slaHours").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.incidentTypes.table.status").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.incidentTypes.table.defaultTargetService")
        .length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.incidentTypes.table.defaultTargetUser")
        .length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.incidentTypes.table.actions").length,
    ).toBeGreaterThan(0);
  });

  it("should display incident type data in the table rows when data is loaded", () => {
    renderTab();

    expect(screen.getByText("Technical Issue")).toBeInTheDocument();
    expect(screen.getByText("Network Outage")).toBeInTheDocument();
    expect(screen.getByText("IT Department")).toBeInTheDocument();
    expect(screen.getByText("John Doe")).toBeInTheDocument();
  });

  it("should render a dash when slaHours is absent", () => {
    renderTab();
    const dashes = screen.getAllByText("-");
    expect(dashes.length).toBeGreaterThan(0);
  });

  it("should not render the add button when user lacks SETTINGS_SYSTEM permission", () => {
    mockAuthWithPermission(false);
    renderTab();

    expect(
      screen.queryByText("settings.incidentTypes.addButton"),
    ).not.toBeInTheDocument();
  });

  it("should render the add button when user has SETTINGS_SYSTEM permission", () => {
    renderTab();

    expect(
      screen.getByText("settings.incidentTypes.addButton"),
    ).toBeInTheDocument();
  });

  it("should not render action buttons when user lacks SETTINGS_SYSTEM permission", () => {
    mockAuthWithPermission(false);
    renderTab();

    expect(
      screen.queryByLabelText("settings.buttons.edit"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByLabelText("settings.buttons.delete"),
    ).not.toBeInTheDocument();
  });

  it("should render edit and delete action buttons when user has SETTINGS_SYSTEM permission", () => {
    renderTab();

    expect(
      screen.getAllByLabelText("settings.buttons.edit").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByLabelText("settings.buttons.delete").length,
    ).toBeGreaterThan(0);
  });

  it("should navigate to create page when add button is clicked", () => {
    renderTab();

    fireEvent.click(screen.getByText("settings.incidentTypes.addButton"));

    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_INCIDENT_TYPES_CREATE,
    );
  });

  it("should navigate to edit page when edit icon is clicked", () => {
    renderTab();

    const row = screen.getByText("Technical Issue").closest("tr");
    expect(row).not.toBeNull();
    fireEvent.click(within(row!).getByLabelText("settings.buttons.edit"));

    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_INCIDENT_TYPES_EDIT("type-1"),
    );
  });

  it("should show a confirmation dialog before deleting an incident type", async () => {
    renderTab();

    const deleteButtons = screen.getAllByLabelText("settings.buttons.delete");
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(
        screen.getByText("settings.incidentTypes.messages.delete_confirm"),
      ).toBeInTheDocument();
    });

    expect(mockDeleteIncidentType).not.toHaveBeenCalled();
  });

  it("should call deleteIncidentType after confirming the deletion dialog", async () => {
    renderTab();

    const row = screen.getByText("Technical Issue").closest("tr");
    expect(row).not.toBeNull();
    fireEvent.click(within(row!).getByLabelText("settings.buttons.delete"));

    await waitFor(() => {
      expect(
        screen.getByText("settings.incidentTypes.messages.delete_confirm"),
      ).toBeInTheDocument();
    });

    const confirmButton = screen.getByText("settings.buttons.confirm");
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockDeleteIncidentType).toHaveBeenCalledWith("type-1");
    });
  });

  it("should display all incident types when unused param is absent", () => {
    renderTab();
    expect(screen.getByText("Technical Issue")).toBeInTheDocument();
    expect(screen.getByText("Network Outage")).toBeInTheDocument();
    expect(screen.getByText("Personal Issue")).toBeInTheDocument();
  });
});
