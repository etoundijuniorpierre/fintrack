// Tests frontend : verifie le comportement de service tab.test.

import { screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import ServiceTab from "./ServiceTab";
import { useAuth } from "../../../../hooks/auth/useAuth";
import type { ServiceResponse } from "../../../../api/settings/types";
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
  useAuth: vi.fn(),
}));

const { mockDeleteDepartment } = vi.hoisted(() => ({
  mockDeleteDepartment: vi.fn(),
}));

vi.mock("../../../../hooks/settings", () => ({
  useDepartments: () => ({
    data: SERVICES,
    isLoading: false,
  }),
  useDeleteDepartment: () => ({
    mutate: mockDeleteDepartment,
    isPending: false,
  }),
}));

// Fabrique une fixture de test pour service tab.test.
const makeService = (
  overrides: Partial<ServiceResponse> = {},
): ServiceResponse => ({
  id: "service-1",
  name: "Service Informatique",
  description: "Gestion des systèmes informatiques",
  isActive: true,
  headOfService: {
    id: "user-1",
    username: "jdupont",
    firstName: "Jean",
    lastName: "Dupont",
  },
  members: [],
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

const SERVICES: ServiceResponse[] = [
  makeService({
    id: "service-1",
    name: "Service Informatique",
    isActive: true,
  }),
  makeService({
    id: "service-2",
    name: "Service RH",
    description: undefined,
    isActive: false,
    headOfService: undefined,
  }),
];

// Fabrique une fixture de test pour service tab.test.
const makeAuthWithPermission = (
  allowed: boolean,
): ReturnType<typeof useAuth> => ({
  user: null,
  isAuthenticated: false,
  hasRole: vi.fn().mockReturnValue(false),
  hasPermission: vi.fn().mockReturnValue(allowed),
});

// Prepare l'affichage lisible de service tab.test.
const renderTab = () => renderWithProviders(<ServiceTab />);

describe("ServiceTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSearchParams = new URLSearchParams();
    vi.mocked(useAuth).mockReturnValue(makeAuthWithPermission(true));
  });

  it("should render the table with correct columns when component mounts", () => {
    renderTab();

    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(
      screen.getAllByText("settings.services.table.name").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.services.table.description").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.services.table.status").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.services.table.headOfService").length,
    ).toBeGreaterThan(0);
    expect(
      screen.getAllByText("settings.services.table.actions").length,
    ).toBeGreaterThan(0);
  });

  it("should display service data in the table rows when services are loaded", () => {
    renderTab();

    expect(screen.getByText("Service Informatique")).toBeInTheDocument();
    expect(
      screen.getByText("Gestion des systèmes informatiques"),
    ).toBeInTheDocument();
    expect(screen.getByText("Service RH")).toBeInTheDocument();
    expect(screen.getByText("Jean Dupont")).toBeInTheDocument();
  });

  it("should render a dash when description or headOfService is absent", () => {
    renderTab();

    const dashes = screen.getAllByText("-");
    expect(dashes.length).toBeGreaterThan(0);
  });

  it("should not render the add button when user lacks SETTINGS_SYSTEM permission", () => {
    vi.mocked(useAuth).mockReturnValue(makeAuthWithPermission(false));
    renderTab();

    expect(
      screen.queryByText("settings.services.addButton"),
    ).not.toBeInTheDocument();
  });

  it("should render the add button when user has SETTINGS_SYSTEM permission", () => {
    renderTab();

    expect(screen.getByText("settings.services.addButton")).toBeInTheDocument();
  });

  it("should not render action buttons when user lacks SETTINGS_SYSTEM permission", () => {
    vi.mocked(useAuth).mockReturnValue(makeAuthWithPermission(false));
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

    fireEvent.click(screen.getByText("settings.services.addButton"));

    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_SERVICES_CREATE,
    );
  });

  it("should navigate to edit page when edit icon is clicked", () => {
    renderTab();

    const editButtons = screen.getAllByLabelText("settings.buttons.edit");
    fireEvent.click(editButtons[0]);

    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_SERVICES_EDIT("service-1"),
    );
  });

  it("should show a confirmation dialog before deleting a service", async () => {
    renderTab();

    const deleteButtons = screen.getAllByLabelText("settings.buttons.delete");
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(
        screen.getByText("settings.services.messages.delete_confirm"),
      ).toBeInTheDocument();
    });

    expect(mockDeleteDepartment).not.toHaveBeenCalled();
  });

  it("should call deleteDepartment after confirming the delete dialog", async () => {
    renderTab();

    const deleteButtons = screen.getAllByLabelText("settings.buttons.delete");
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(
        screen.getByText("settings.services.messages.delete_confirm"),
      ).toBeInTheDocument();
    });

    const confirmButton = screen.getByText("settings.buttons.confirm");
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(mockDeleteDepartment).toHaveBeenCalledWith("service-1");
    });
  });

  it("filters to services without head when missingHead param is set", () => {
    mockSearchParams = new URLSearchParams("missingHead=true");
    renderTab();
    // Service RH has no headOfService, so it should be visible
    expect(screen.getByText("Service RH")).toBeInTheDocument();
    // Service Informatique has a headOfService, so it should NOT be visible
    expect(screen.queryByText("Jean Dupont")).not.toBeInTheDocument();
  });

  it("shows all services when missingHead param is absent", () => {
    renderTab();
    expect(screen.getByText("Service Informatique")).toBeInTheDocument();
    expect(screen.getByText("Service RH")).toBeInTheDocument();
  });
});
