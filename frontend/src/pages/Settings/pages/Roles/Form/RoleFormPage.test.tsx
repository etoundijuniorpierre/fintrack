// Tests frontend : verifie le comportement de role formulaire page.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../../../test-utils/renderWithProviders";
import RoleFormPage from "./RoleFormPage";
import { makeSettingsPermission, makeSettingsRole } from "../../../../../mocks";
import { useRole } from "../../../../../hooks/settings";

const mockNavigate = vi.fn();
const mockUseParams = vi.fn<() => { id?: string }>();

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, options?: { defaultValue?: string }) => {
      if (key === "users.role_descriptions.ADMIN")
        return "Manages the entire platform";
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
    useParams: () => mockUseParams(),
  };
});

const mockUpdateRole = vi.fn();

vi.mock("../../../../../hooks/settings", () => ({
  useCreateRole: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useUpdateRole: vi.fn(() => ({ mutate: mockUpdateRole, isPending: false })),
  usePermissions: vi.fn(() => ({
    data: [
      makeSettingsPermission({
        id: "perm-1",
        name: "ROLE_CREATE",
        description: "Create roles",
      }),
    ],
    isLoading: false,
  })),
  useRoles: vi.fn(() => ({
    data: [
      makeSettingsRole({
        id: "agent-role",
        name: "AGENT",
        permissions: [
          makeSettingsPermission({
            id: "perm-1",
            name: "ROLE_CREATE",
            description: "Create roles",
          }),
        ],
      }),
    ],
    isLoading: false,
  })),
  useRole: vi.fn(() => ({ data: undefined, isLoading: false })),
}));

describe("RoleFormPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseParams.mockReturnValue({});
  });

  it("should render create title when route has no id", () => {
    renderWithProviders(<RoleFormPage />);
    expect(
      screen.getByText("settings.roles.page.createTitle"),
    ).toBeInTheDocument();
  });

  it("should preselect the system AGENT permissions when creating a custom role", () => {
    renderWithProviders(<RoleFormPage />);

    expect(screen.getByRole("checkbox")).toBeChecked();
  });

  it("should render edit title and system notice when route has id", async () => {
    mockUseParams.mockReturnValue({ id: "role-1" });
    vi.mocked(useRole).mockReturnValue({
      data: makeSettingsRole({ id: "role-1", isSystem: true }),
      isLoading: false,
    } as ReturnType<typeof useRole>);

    renderWithProviders(<RoleFormPage />);
    expect(
      screen.getByText("settings.roles.page.editTitle"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("settings.roles.messages.system_role_edit_notice"),
    ).toBeInTheDocument();
  });

  it("should navigate back when cancel button is clicked", () => {
    renderWithProviders(<RoleFormPage />);
    const cancel = screen
      .getByText("settings.buttons.cancel")
      .closest("button");
    expect(cancel).not.toBeNull();
    fireEvent.click(cancel!);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  it("should pre-fill description with translated text, not the raw DB key, for system roles", async () => {
    mockUseParams.mockReturnValue({ id: "role-1" });
    vi.mocked(useRole).mockReturnValue({
      // Backend stores a raw translation key as description for system roles
      data: makeSettingsRole({
        id: "role-1",
        name: "ROLE_ADMIN",
        description: "users.role_descriptions.ADMIN",
        isSystem: true,
      }),
      isLoading: false,
    } as ReturnType<typeof useRole>);

    renderWithProviders(<RoleFormPage />);

    const textarea = await screen.findByPlaceholderText(
      "settings.roles.form.placeholders.description",
    );
    // Verifie que le textarea affiche le texte traduit plutot que la cle brute.
    expect(textarea).toHaveValue("Manages the entire platform");
    expect(textarea).not.toHaveValue("users.role_descriptions.ADMIN");
  });

  it("should lock the name field for system roles in edit mode", async () => {
    mockUseParams.mockReturnValue({ id: "role-1" });
    vi.mocked(useRole).mockReturnValue({
      data: makeSettingsRole({
        id: "role-1",
        name: "ROLE_ADMIN",
        isSystem: true,
      }),
      isLoading: false,
    } as ReturnType<typeof useRole>);

    renderWithProviders(<RoleFormPage />);

    const nameInput = await screen.findByPlaceholderText(
      "settings.roles.form.placeholders.name",
    );
    expect(nameInput).toBeDisabled();
  });
});
