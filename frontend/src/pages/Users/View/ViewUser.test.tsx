// Tests frontend : verifie le comportement de view user.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent } from "@testing-library/react";
import { renderWithProviders } from "../../../test-utils/renderWithProviders";
import ViewUser from "./ViewUser";
import { makeUser } from "../../../mocks/user/users";
import {
  useUser,
  useDeleteUser,
  useToggleUserStatus,
  useRegeneratePassword,
} from "../../../hooks/user/useUsers";
import type { User } from "../../../api/user/types";
import type { UseQueryResult, UseMutationResult } from "@tanstack/react-query";

vi.mock("../../../hooks/user/useUsers", () => ({
  useUser: vi.fn(),
  useDeleteUser: vi.fn(),
  useToggleUserStatus: vi.fn(),
  useRegeneratePassword: vi
    .fn()
    .mockReturnValue({ mutate: vi.fn(), isPending: false }),
  useUpdateUser: vi.fn().mockReturnValue({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("../../../hooks/user/useOnlinePresence/useOnlinePresence", () => ({
  useOnlinePresence: () => ({
    online: [],
    isOnline: () => false,
    canViewPresence: true,
    isLoading: false,
  }),
}));

vi.mock("../../../hooks/role/useRoles", () => ({
  useRoles: vi.fn().mockReturnValue({ data: [] }),
}));

vi.mock("../../../hooks/agency/useAgencies", () => ({
  useAgencies: vi.fn().mockReturnValue({ data: [] }),
}));

vi.mock("../components", () => ({
  UserForm: ({
    initialValues,
    disabled,
    showButtons,
    onCancel,
    submitText,
  }: {
    initialValues?: Partial<User>;
    disabled?: boolean;
    showButtons?: boolean;
    onCancel?: () => void;
    submitText?: string;
  }) => (
    <div role="form" aria-label="user-form">
      <label htmlFor="username">Username</label>
      <input
        id="username"
        defaultValue={initialValues?.username}
        disabled={disabled}
      />
      {showButtons && (
        <>
          <button type="submit">{submitText}</button>
          <button type="button" onClick={onCancel}>
            common.cancel
          </button>
        </>
      )}
    </div>
  ),
  UserTable: () => <div />,
  PasswordManagement: () => <div data-testid="password-management" />,
}));

vi.mock("../../../hooks/permission/usePermissions", () => ({
  usePermissions: vi.fn(() => ({ data: [], isLoading: false })),
}));

vi.mock("../../../hooks/auth/useAuth", () => ({
  useLogin: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useAuth: vi.fn(() => ({
    user: {
      username: "admin",
      id: "admin-1",
      roles: ["ADMIN"],
      permissions: [],
    },
    isAuthenticated: true,
    hasRole: () => true,
    hasPermission: () => true,
  })),
  useReauth: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
}));

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
  initReactI18next: {
    type: "3rdParty",
    init: () => {},
  },
}));

const { mockNavigate } = vi.hoisted(() => ({ mockNavigate: vi.fn() }));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useParams: () => ({ id: "user-1" }),
    useNavigate: () => mockNavigate,
  };
});

// Prepare l'affichage lisible de view user.test.
const renderViewUser = () => renderWithProviders(<ViewUser />);

describe("ViewUser", () => {
  const testUser = makeUser({ id: "user-1" });
  const mockDelete = vi.fn();
  const mockToggle = vi.fn();
  const mockReset = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useUser).mockReturnValue({
      data: testUser,
      isLoading: false,
    } as Partial<UseQueryResult<User>> as UseQueryResult<User>);
    vi.mocked(useDeleteUser).mockReturnValue({
      mutate: mockDelete,
    } as Partial<
      UseMutationResult<void, Error, string, unknown>
    > as UseMutationResult<void, Error, string, unknown>);
    vi.mocked(useToggleUserStatus).mockReturnValue({
      mutate: mockToggle,
    } as Partial<
      UseMutationResult<User, Error, string, unknown>
    > as UseMutationResult<User, Error, string, unknown>);
    vi.mocked(useRegeneratePassword).mockReturnValue({
      mutate: mockReset,
      isPending: false,
    } as Partial<
      UseMutationResult<User, Error, string, unknown>
    > as UseMutationResult<User, Error, string, unknown>);
  });

  it("should show loading skeleton when isLoading is true", () => {
    vi.mocked(useUser).mockReturnValue({
      data: undefined,
      isLoading: true,
    } as Partial<UseQueryResult<User>> as UseQueryResult<User>);
    renderViewUser();

    // Ant Design's Skeleton adds the class ant-skeleton-content
    expect(document.querySelector(".ant-skeleton")).toBeInTheDocument();

    // PageHeader is visible even while loading
    expect(screen.getByText("users.form.titles.view")).toBeInTheDocument();
  });

  it("should show error alert when user is not found after loading", () => {
    vi.mocked(useUser).mockReturnValue({
      data: undefined,
      isLoading: false,
    } as Partial<UseQueryResult<User>> as UseQueryResult<User>);
    renderViewUser();
    expect(screen.getByText("common.unknown_error")).toBeInTheDocument();
  });

  it("should navigate to incidents created by the user from the incidents card", () => {
    renderViewUser();

    fireEvent.click(screen.getByText("users.incidents.created"));

    expect(mockNavigate).toHaveBeenCalledWith(
      "/dashboard/incidents?createdBy=user-1",
      expect.objectContaining({
        state: expect.objectContaining({
          incidentUserLabel: expect.any(String),
        }),
      }),
    );
  });

  it("should display user information in read-only mode when loaded", () => {
    renderViewUser();

    expect(
      screen.getByRole("heading", { name: testUser.username }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Username")).toHaveValue(testUser.username);
    expect(screen.getByLabelText("Username")).toBeDisabled();

    expect(
      screen.getByRole("button", { name: "common.edit" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "common.delete" }),
    ).toBeInTheDocument();
  });

  it("should switch to edit mode when edit button is clicked", () => {
    renderViewUser();

    fireEvent.click(screen.getByRole("button", { name: "common.edit" }));

    expect(
      screen.getAllByText("users.form.titles.edit").length,
    ).toBeGreaterThan(0);
    expect(screen.getByLabelText("Username")).not.toBeDisabled();
    expect(
      screen.getByRole("button", { name: "users.form.buttons.submit_edit" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "common.cancel" }),
    ).toBeInTheDocument();
  });

  it("should return to view mode when cancel is clicked in edit mode", () => {
    renderViewUser();

    fireEvent.click(screen.getByRole("button", { name: "common.edit" }));
    fireEvent.click(screen.getByRole("button", { name: "common.cancel" }));

    expect(
      screen.getByRole("heading", { name: testUser.username }),
    ).toBeInTheDocument();
  });

  it("should call delete mutation after confirmation", () => {
    renderViewUser();

    fireEvent.click(screen.getByRole("button", { name: "common.delete" }));
    fireEvent.click(screen.getByText("common.yes"));

    expect(mockDelete).toHaveBeenCalledWith(testUser.id, expect.anything());
  });

  it("should call toggle status mutation when deactivate button is clicked", () => {
    renderViewUser();

    fireEvent.click(
      screen.getByRole("button", { name: "users.form.actions.deactivate" }),
    );

    expect(mockToggle).toHaveBeenCalledWith(testUser.id);
  });

  it("should show activate button when user is inactive", () => {
    vi.mocked(useUser).mockReturnValue({
      data: makeUser({ id: "user-1", isActive: false }),
      isLoading: false,
    } as Partial<UseQueryResult<User>> as UseQueryResult<User>);
    renderViewUser();

    expect(
      screen.getByRole("button", { name: "users.form.actions.activate" }),
    ).toBeInTheDocument();
  });

  it("should navigate back when back button is clicked", () => {
    renderViewUser();

    fireEvent.click(screen.getByRole("button", { name: "common.back" }));

    expect(mockNavigate).toHaveBeenCalled();
  });

  describe("Self-Destructive Actions Protection", () => {
    it("should disable delete and deactivate buttons when viewing own account", () => {
      const ownUser = makeUser({ id: "admin-1" });
      vi.mocked(useUser).mockReturnValue({
        data: ownUser,
        isLoading: false,
      } as Partial<UseQueryResult<User>> as UseQueryResult<User>);

      renderViewUser();

      const deleteButton = screen.getByRole("button", {
        name: "common.delete",
      });
      const deactivateButton = screen.getByRole("button", {
        name: "users.form.actions.deactivate",
      });

      expect(deleteButton).toBeDisabled();
      expect(deactivateButton).toBeDisabled();
    });

    it("should enable delete and deactivate buttons when viewing another user account", () => {
      const otherUser = makeUser({ id: "user-2" });
      vi.mocked(useUser).mockReturnValue({
        data: otherUser,
        isLoading: false,
      } as Partial<UseQueryResult<User>> as UseQueryResult<User>);

      renderViewUser();

      const deleteButton = screen.getByRole("button", {
        name: "common.delete",
      });
      const deactivateButton = screen.getByRole("button", {
        name: "users.form.actions.deactivate",
      });

      expect(deleteButton).not.toBeDisabled();
      expect(deactivateButton).not.toBeDisabled();
    });

    it("should not call delete mutation when delete button is disabled", () => {
      const ownUser = makeUser({ id: "admin-1" });
      vi.mocked(useUser).mockReturnValue({
        data: ownUser,
        isLoading: false,
      } as Partial<UseQueryResult<User>> as UseQueryResult<User>);

      renderViewUser();

      const deleteButton = screen.getByRole("button", {
        name: "common.delete",
      });

      expect(deleteButton).toBeDisabled();
      fireEvent.click(deleteButton);

      expect(mockDelete).not.toHaveBeenCalled();
    });

    it("should not call toggle status mutation when deactivate button is disabled", () => {
      const ownUser = makeUser({ id: "admin-1" });
      vi.mocked(useUser).mockReturnValue({
        data: ownUser,
        isLoading: false,
      } as Partial<UseQueryResult<User>> as UseQueryResult<User>);

      renderViewUser();

      const deactivateButton = screen.getByRole("button", {
        name: "users.form.actions.deactivate",
      });

      expect(deactivateButton).toBeDisabled();
      fireEvent.click(deactivateButton);

      expect(mockToggle).not.toHaveBeenCalled();
    });
  });
});
