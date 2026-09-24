// Tests frontend : verifie le comportement de role details page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import RoleDetailsPage from "./RoleDetailsPage";
import { makeSettingsRole } from "../../../../../mocks";
import type { User } from "../../../../../api/user/types";
import { APP_ROUTES } from "../../../../../utils/constants";

const mockNavigate = vi.fn();
const mockDeleteRole = vi.fn();

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, options?: { defaultValue?: string } | string) => {
      if (key === "users.roles.ADMIN") return "Administrator";
      if (key === "users.role_descriptions.ADMIN")
        return "Administrator with full system access";
      if (typeof options === "string") return options;
      return options?.defaultValue ?? key;
    },
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: "role-1" }),
  };
});

vi.mock("../../../../../hooks/auth/useAuth", () => ({
  useAuth: vi.fn(() => ({
    hasPermission: () => true,
    user: null,
    isAuthenticated: true,
    hasRole: () => false,
  })),
}));

vi.mock("../../../../../hooks/settings", () => ({
  useRole: vi.fn(() => ({
    data: makeSettingsRole({
      id: "role-1",
      name: "ROLE_ADMIN",
      isSystem: false,
      permissions: [
        {
          id: "perm-1",
          name: "ROLE_CREATE",
          description: "Create",
          createdAt: "",
          updatedAt: "",
        },
        {
          id: "perm-2",
          name: "ROLE_UPDATE",
          description: "Update",
          createdAt: "",
          updatedAt: "",
        },
      ],
    }),
    isLoading: false,
  })),
  useDeleteRole: vi.fn(() => ({ mutate: mockDeleteRole })),
}));

vi.mock("../../../../../hooks/user", () => ({
  useUsers: vi.fn(() => ({
    data: [
      {
        id: "user-1",
        username: "jdoe",
        email: "jdoe@test.com",
        firstName: "John",
        lastName: "Doe",
        isActive: true,
        createdAt: "",
        updatedAt: "",
        roles: [
          { id: "role-1", name: "ROLE_ADMIN", createdAt: "", updatedAt: "" },
        ],
        permissions: [],
      } as User,
    ],
    isLoading: false,
  })),
}));

describe("RoleDetailsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should render role details with permissions and assigned users counts", () => {
    renderWithProviders(<RoleDetailsPage />);
    expect(screen.getAllByText("Administrator").length).toBeGreaterThan(0);
    expect(
      screen.getByText("Administrator with full system access"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("settings.roles.assigned_users"),
    ).toBeInTheDocument();
    expect(screen.getAllByText("2").length).toBeGreaterThan(0);
    expect(screen.getAllByText("1").length).toBeGreaterThan(0);
  });

  it("should navigate to edit page when edit button is clicked", () => {
    renderWithProviders(<RoleDetailsPage />);
    const edit = screen.getByText("settings.buttons.edit").closest("button");
    expect(edit).not.toBeNull();
    fireEvent.click(edit!);
    expect(mockNavigate).toHaveBeenCalledWith(
      APP_ROUTES.SETTINGS_ROLES_EDIT("role-1"),
    );
  });

  it("should call delete mutation after confirmation", () => {
    renderWithProviders(<RoleDetailsPage />);
    const deleteBtn = screen
      .getByText("settings.buttons.delete")
      .closest("button");
    expect(deleteBtn).not.toBeNull();
    fireEvent.click(deleteBtn!);
    fireEvent.click(screen.getByText("Confirm"));
    expect(mockDeleteRole).toHaveBeenCalledWith("role-1", expect.any(Object));
  });
});
