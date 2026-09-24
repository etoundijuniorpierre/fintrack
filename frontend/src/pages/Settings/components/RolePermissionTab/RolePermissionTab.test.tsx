// Tests frontend : verifie le comportement de role permission tab.test.

import { screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import RolePermissionTab from "./RolePermissionTab";
import { useAuth } from "../../../../hooks/auth/useAuth";
import type {
  RoleResponse,
  PermissionResponse,
} from "../../../../api/settings/types";
import { makeSettingsRole, makeSettingsPermission } from "../../../../mocks";
import { APP_ROUTES } from "../../../../utils/constants";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, options?: { defaultValue?: string }) => {
      if (key === "users.roles.ADMIN") return "Administrator";
      if (key === "users.role_descriptions.ADMIN")
        return "Manages the platform";
      return options?.defaultValue ?? key;
    },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({ hasPermission: () => true })),
}));

const { mockDeleteRole } = vi.hoisted(() => ({
  mockDeleteRole: vi.fn(),
}));

vi.mock("../../../../hooks/settings", () => ({
  useRoles: () => ({
    data: ROLES,
    isLoading: false,
  }),
  useDeleteRole: () => ({ mutate: mockDeleteRole, isPending: false }),
}));

const PERMISSIONS: PermissionResponse[] = [
  makeSettingsPermission({
    id: "perm-1",
    name: "ROLE_CREATE",
    description: "Create roles",
  }),
  makeSettingsPermission({
    id: "perm-2",
    name: "ROLE_UPDATE",
    description: "Update roles",
  }),
  makeSettingsPermission({
    id: "perm-3",
    name: "ROLE_DELETE",
    description: "Delete roles",
  }),
];

const ROLES: RoleResponse[] = [
  // ROLE_ADMIN has a raw translation key stored in description (simulates backend behaviour for system roles)
  makeSettingsRole({
    id: "role-1",
    name: "ROLE_ADMIN",
    description: "users.role_descriptions.ADMIN",
    isSystem: false,
    permissions: [PERMISSIONS[0]],
  }),
  makeSettingsRole({
    id: "role-2",
    name: "ROLE_SYSTEM",
    description: "System role",
    isSystem: true,
    permissions: [PERMISSIONS[0], PERMISSIONS[1]],
  }),
];

// Fabrique une fixture de test pour role permission tab.test.
const makeHasPermission =
  (...allowedPermissions: string[]) =>
  (permission: string) =>
    allowedPermissions.includes(permission);

// Prepare l'affichage lisible de role permission tab.test.
const renderTab = () => renderWithProviders(<RolePermissionTab />);

describe("RolePermissionTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: () => true,
      user: null,
      isAuthenticated: true,
      hasRole: () => false,
    } as ReturnType<typeof useAuth>);
  });

  it("should render the table and display role data", () => {
    renderTab();
    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.getByText("Administrator")).toBeInTheDocument();
    expect(screen.getByText("ROLE_SYSTEM")).toBeInTheDocument();
  });

  it("should show permissions count instead of listing all permissions", () => {
    renderTab();

    expect(screen.getAllByText("1").length).toBeGreaterThan(0);
    expect(screen.getAllByText("2").length).toBeGreaterThan(0);
    expect(screen.queryByText("ROLE_CREATE")).not.toBeInTheDocument();
  });

  it("should navigate to create page when add button is clicked", () => {
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: makeHasPermission("ROLE_CREATE"),
      user: null,
      isAuthenticated: true,
      hasRole: () => false,
    } as ReturnType<typeof useAuth>);
    renderTab();

    const addButton = screen.getByText("settings.roles.addButton");
    fireEvent.click(addButton);

    expect(mockNavigate).toHaveBeenCalledWith(APP_ROUTES.SETTINGS_ROLES_CREATE);
  });

  it("should navigate to details page when a row is clicked", () => {
    renderTab();

    const row = screen.getByText("Administrator").closest("tr");
    fireEvent.click(row!);

    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_ROLES_DETAILS("role-1"),
    );
  });

  it("should show confirmation dialog and call deleteRole", async () => {
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: makeHasPermission("ROLE_DELETE"),
      user: null,
      isAuthenticated: true,
      hasRole: () => false,
    } as ReturnType<typeof useAuth>);
    renderTab();

    const deleteButtons = screen.getAllByLabelText("settings.buttons.delete");
    fireEvent.click(deleteButtons[0]);

    expect(
      screen.getByText("settings.roles.messages.delete_confirm"),
    ).toBeInTheDocument();

    const confirmBtn = screen.getByText("settings.buttons.confirm");
    fireEvent.click(confirmBtn);

    expect(mockDeleteRole).toHaveBeenCalledWith("role-1");
  });

  it("should display translated description instead of the raw translation key stored in DB", () => {
    // ROLE_ADMIN has description='users.role_descriptions.ADMIN' (raw key) in the fixture.
    // The column must resolve it via t() 'Manages the platform', not show the raw key.
    renderTab();
    expect(screen.getByText("Manages the platform")).toBeInTheDocument();
    expect(
      screen.queryByText("users.role_descriptions.ADMIN"),
    ).not.toBeInTheDocument();
  });

  it("should display defaultValue description for custom roles without a translation key", () => {
    // ROLE_SYSTEM garde une description brute 'System role' sans surcharge t() associee.
    // Verifie que la colonne affiche le libelle de secours defaultValue.
    renderTab();
    expect(screen.getByText("System role")).toBeInTheDocument();
  });

  it("should disable delete button for system roles", () => {
    vi.mocked(useAuth).mockReturnValue({
      hasPermission: makeHasPermission("ROLE_DELETE"),
      user: null,
      isAuthenticated: true,
      hasRole: () => false,
    } as ReturnType<typeof useAuth>);
    renderTab();

    const deleteButtons = screen.getAllByLabelText("settings.buttons.delete");
    expect(deleteButtons[1]).toBeDisabled();
  });
});
