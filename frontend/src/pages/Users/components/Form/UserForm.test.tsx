// Tests frontend : verifie le comportement de utilisateur form.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen, fireEvent, waitFor } from "@testing-library/react";
import UserForm from "./UserForm";
import * as userHooks from "../../../../hooks/user/useUsers";
import * as authHooks from "../../../../hooks/auth/useAuth";
import * as roleHooks from "../../../../hooks/role/useRoles";
import * as permissionHooks from "../../../../hooks/permission/usePermissions";
import type {
  User,
  Role,
  Agency,
  Permission,
} from "../../../../api/user/types";
import { PERMISSIONS } from "../../../../utils/permissions/permissions";
import { ROLE_NAMES } from "../../../../utils/roles/roles";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";

const mockCreateUser = vi.fn();
const mockUpdateUser = vi.fn();

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
  initReactI18next: { type: "3rdParty", init: vi.fn() },
}));

vi.mock("../../../../hooks/user/useUsers", () => ({
  useCreateUser: vi.fn(() => ({ mutate: mockCreateUser, isPending: false })),
  useUpdateUser: vi.fn(() => ({ mutate: mockUpdateUser, isPending: false })),
  useRegeneratePassword: vi
    .fn()
    .mockReturnValue({ mutate: vi.fn(), isPending: false }),
}));

vi.mock("../../../../hooks/role/useRoles", () => ({
  useRoles: vi.fn().mockReturnValue({ data: [] }),
}));

vi.mock("../../../../hooks/agency/useAgencies", () => ({
  useAgencies: vi.fn().mockReturnValue({ data: [] }),
}));

vi.mock("../../../../hooks/service/useServices", () => ({
  useServices: vi.fn().mockReturnValue({ data: [] }),
}));

vi.mock("../../../../hooks/permission/usePermissions", () => ({
  usePermissions: vi.fn().mockReturnValue({ data: [] }),
}));

vi.mock("../../../../hooks/auth/useAuth", () => ({
  useLogin: vi.fn().mockReturnValue({ mutate: vi.fn(), isPending: false }),
  useReauth: vi.fn().mockReturnValue({ mutate: vi.fn(), isPending: false }),
  useAuth: vi.fn().mockReturnValue({
    user: { username: "admin" },
    isAuthenticated: true,
    hasPermission: vi.fn().mockReturnValue(true),
    hasRole: vi.fn().mockReturnValue(true),
  }),
}));

// Prepare l'affichage lisible de utilisateur form.test.
const renderUserForm = (props = {}) => {
  const defaultProps = {
    onCancel: vi.fn(),
    onSuccess: vi.fn(),
  };
  return renderWithProviders(<UserForm {...defaultProps} {...props} />);
};

// Fabrique la liste de roles exposee par le hook simule.
const mockRoles = (roles: Role[]) => {
  vi.mocked(roleHooks.useRoles).mockReturnValue({
    data: roles,
    isLoading: false,
  } as Partial<ReturnType<typeof roleHooks.useRoles>> as ReturnType<
    typeof roleHooks.useRoles
  >);
};

describe("UserForm", () => {
  const makeRole: Role = {
    id: "role-1",
    name: "AGENT",
    createdAt: "2024-01-01",
    updatedAt: "2024-01-01",
    permissions: [],
  };

  const makeChefAgenceRole: Role = {
    id: "role-2",
    name: "CHEF_AGENCE",
    createdAt: "2024-01-01",
    updatedAt: "2024-01-01",
    permissions: [],
  };

  const makeChefServiceRole: Role = {
    id: "role-3",
    name: "CHEF_SERVICE",
    createdAt: "2024-01-01",
    updatedAt: "2024-01-01",
    permissions: [],
  };

  const manageProfilePermission: Permission = {
    id: "permission-1",
    name: PERMISSIONS.USER.MANAGE_PROFILE,
    description: "Manage own profile",
    createdAt: "2024-01-01",
    updatedAt: "2024-01-01",
  };

  const mockCreateUserValues: Partial<User> = {
    username: "newuser",
    firstName: "New",
    lastName: "User",
    email: "new@finstar-cm.com",
    agency: {
      id: "agency-1",
      name: "Agency 1",
      createdAt: "2024-01-01",
      updatedAt: "2024-01-01",
    } as Agency,
    roles: [makeRole],
  };

  const mockExistingUser: Partial<User> = {
    id: "user-1",
    username: "existinguser",
    firstName: "Existing",
    lastName: "User",
    email: "existing@finstar-cm.com",
    phoneNumber: 771234567,
    isActive: true,
    agency: {
      id: "agency-1",
      name: "Agency 1",
      createdAt: "2024-01-01",
      updatedAt: "2024-01-01",
    } as Agency,
    roles: [makeRole],
    permissions: [],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authHooks.useAuth).mockReturnValue({
      user: {
        id: "admin-1",
        username: "admin",
        roles: ["ADMIN"],
        permissions: [],
      },
      isAuthenticated: true,
      hasPermission: vi.fn().mockReturnValue(true),
      hasRole: vi.fn().mockReturnValue(true),
    } as Partial<ReturnType<typeof authHooks.useAuth>> as ReturnType<
      typeof authHooks.useAuth
    >);
    vi.mocked(userHooks.useCreateUser).mockImplementation(
      () =>
        ({ mutate: mockCreateUser, isPending: false }) as Partial<
          ReturnType<typeof userHooks.useCreateUser>
        > as ReturnType<typeof userHooks.useCreateUser>,
    );
    vi.mocked(userHooks.useUpdateUser).mockImplementation(
      () =>
        ({ mutate: mockUpdateUser, isPending: false }) as Partial<
          ReturnType<typeof userHooks.useUpdateUser>
        > as ReturnType<typeof userHooks.useUpdateUser>,
    );
  });

  it("should render all form fields including phone and service", () => {
    renderUserForm({ initialValues: mockExistingUser });

    const expectedLabels = [
      "users.form.labels.firstname",
      "users.form.labels.lastname",
      "users.form.labels.email",
      "users.form.labels.username",
      "users.form.labels.roles",
      "users.form.labels.agency",
      "users.form.labels.service",
    ];

    expectedLabels.forEach((label) => {
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    });

    expect(
      screen.getByRole("spinbutton", { name: "users.form.labels.phone" }),
    ).toBeInTheDocument();
  });

  it("should prevent submission and display validation errors on empty form", async () => {
    renderUserForm();

    const submitBtn = screen.getByRole("button", {
      name: "users.form.buttons.submit_create",
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText("users.form.validation.roles_required"),
      ).toBeInTheDocument();
    });

    expect(mockCreateUser).not.toHaveBeenCalled();
  });

  it("should call createUser mutation with correct field mappings", async () => {
    const onSuccessMock = vi.fn();
    renderUserForm({
      initialValues: mockCreateUserValues,
      onSuccess: onSuccessMock,
    });

    const submitBtn = screen.getByRole("button", {
      name: "users.form.buttons.submit_create",
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockCreateUser).toHaveBeenCalled();
    });

    const createArgs = mockCreateUser.mock.calls[0];
    expect(createArgs[0]).toMatchObject({
      firstName: "New",
      lastName: "User",
      roleIds: ["role-1"],
    });

    createArgs[1].onSuccess({ id: "user-1", username: "newuser" });
    expect(onSuccessMock).toHaveBeenCalled();
  });

  it("should validate roles as required when form is submitted empty", async () => {
    renderUserForm();

    const submitBtn = screen.getByRole("button", {
      name: "users.form.buttons.submit_create",
    });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(
        screen.getByText("users.form.validation.roles_required"),
      ).toBeInTheDocument();
    });
  });

  it("should call updateUser mutation when initialValues with id is provided", async () => {
    renderUserForm({
      initialValues: mockExistingUser,
      submitText: "Submit Update",
    });

    fireEvent.change(screen.getByLabelText("users.form.labels.firstname"), {
      target: { value: "UpdatedName" },
    });

    const submitBtn = screen.getByRole("button", { name: "Submit Update" });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockUpdateUser).toHaveBeenCalled();
    });

    const updateArgs = mockUpdateUser.mock.calls[0];
    expect(updateArgs[0]).toMatchObject({
      id: "user-1",
      data: expect.objectContaining({
        firstName: "UpdatedName",
        username: "existinguser",
        roleIds: ["role-1"],
      }),
    });

    updateArgs[1].onSuccess({ id: "user-1", username: "existinguser" });
  });

  it("should disable all fields when disabled prop is true", () => {
    renderUserForm({ disabled: true, initialValues: mockExistingUser });

    expect(screen.getByLabelText("users.form.labels.firstname")).toBeDisabled();
    expect(screen.getByLabelText("users.form.labels.lastname")).toBeDisabled();
    expect(screen.getByLabelText("users.form.labels.email")).toBeDisabled();
    expect(
      screen.getByRole("spinbutton", { name: "users.form.labels.phone" }),
    ).toBeDisabled();
    expect(screen.getByLabelText("users.form.labels.username")).toBeDisabled();
    expect(
      screen.queryByRole("button", { name: "Submit Update" }),
    ).not.toBeInTheDocument();
  });

  it("should not load role and permission catalogs for a scoped read-only user", () => {
    vi.mocked(authHooks.useAuth).mockReturnValue({
      user: { id: "viewer-1", username: "viewer", roles: [], permissions: [] },
      isAuthenticated: true,
      hasPermission: vi.fn().mockReturnValue(false),
      hasRole: vi.fn().mockReturnValue(false),
    } as Partial<ReturnType<typeof authHooks.useAuth>> as ReturnType<
      typeof authHooks.useAuth
    >);

    renderUserForm({
      disabled: true,
      initialValues: {
        id: "user-2",
        username: "scopeduser",
        isActive: true,
      },
    });

    expect(vi.mocked(roleHooks.useRoles)).toHaveBeenCalledWith(false);
    expect(vi.mocked(permissionHooks.usePermissions)).toHaveBeenCalledWith(
      false,
    );
  });

  it("should display loading state during submission", () => {
    vi.mocked(userHooks.useCreateUser).mockImplementation(
      () =>
        ({ mutate: mockCreateUser, isPending: true }) as Partial<
          ReturnType<typeof userHooks.useCreateUser>
        > as ReturnType<typeof userHooks.useCreateUser>,
    );
    vi.mocked(userHooks.useUpdateUser).mockImplementation(
      () =>
        ({ mutate: mockUpdateUser, isPending: true }) as Partial<
          ReturnType<typeof userHooks.useUpdateUser>
        > as ReturnType<typeof userHooks.useUpdateUser>,
    );

    renderUserForm();

    const submitBtn = screen.getByRole("button", {
      name: /users\.form\.buttons\.submit_create/,
    });
    expect(submitBtn).toBeDisabled();
  });

  it("should display managed_agency field when user has CHEF_AGENCE role", () => {
    mockRoles([makeChefAgenceRole]);
    renderUserForm({
      initialValues: { ...mockExistingUser, roles: [makeChefAgenceRole] },
    });
    expect(
      screen.getByLabelText("users.form.labels.managed_agency"),
    ).toBeInTheDocument();
  });

  it("should display managed_services field when user has CHEF_SERVICE role", () => {
    mockRoles([makeChefServiceRole]);
    renderUserForm({
      initialValues: { ...mockExistingUser, roles: [makeChefServiceRole] },
    });
    expect(
      screen.getByLabelText("users.form.labels.managed_services"),
    ).toBeInTheDocument();
  });

  it("should allow an agency manager with USER_CREATE_AGENT_AGENCY to select a subordinate role", () => {
    mockRoles([makeRole, makeChefAgenceRole, makeChefServiceRole]);
    vi.mocked(authHooks.useAuth).mockReturnValue({
      user: {
        id: "manager-1",
        username: "manager",
        roles: [ROLE_NAMES.AGENCY_MANAGER],
        permissions: [
          PERMISSIONS.USER.CREATE_AGENT_AGENCY,
          PERMISSIONS.USER.VIEW_AGENCY,
        ],
        agencyId: "agency-1",
      },
      isAuthenticated: true,
      hasPermission: (permission: string) =>
        permission === PERMISSIONS.USER.CREATE_AGENT_AGENCY ||
        permission === PERMISSIONS.USER.VIEW_AGENCY,
      hasRole: (role: string) => role === ROLE_NAMES.AGENCY_MANAGER,
    } as ReturnType<typeof authHooks.useAuth>);

    renderUserForm();

    expect(screen.getByLabelText("users.form.labels.roles")).not.toBeDisabled();
    expect(screen.getByLabelText("users.form.labels.agency")).toBeDisabled();
    expect(screen.getByLabelText("users.form.labels.service")).toBeDisabled();
  });

  it("should lock agency and service when a service manager creates a user", () => {
    mockRoles([makeRole]);
    vi.mocked(authHooks.useAuth).mockReturnValue({
      user: {
        id: "manager-2",
        username: "manager",
        roles: [ROLE_NAMES.SERVICE_MANAGER],
        permissions: [
          PERMISSIONS.USER.CREATE_AGENT_SERVICE,
          PERMISSIONS.USER.VIEW_SERVICE,
        ],
        agencyId: "agency-1",
        serviceId: "service-1",
      },
      isAuthenticated: true,
      hasPermission: (permission: string) =>
        permission === PERMISSIONS.USER.CREATE_AGENT_SERVICE ||
        permission === PERMISSIONS.USER.VIEW_SERVICE,
      hasRole: (role: string) => role === ROLE_NAMES.SERVICE_MANAGER,
    } as ReturnType<typeof authHooks.useAuth>);

    renderUserForm();

    expect(screen.getByLabelText("users.form.labels.agency")).toBeDisabled();
    expect(screen.getByLabelText("users.form.labels.service")).toBeDisabled();
  });

  it("should omit direct permissions when the creator lacks ROLE_ASSIGN", async () => {
    mockRoles([makeRole]);
    vi.mocked(authHooks.useAuth).mockReturnValue({
      user: {
        id: "manager-1",
        username: "manager",
        roles: [ROLE_NAMES.AGENCY_MANAGER],
        permissions: [PERMISSIONS.USER.CREATE_AGENT_AGENCY],
        agencyId: "agency-1",
      },
      isAuthenticated: true,
      hasPermission: (permission: string) =>
        permission === PERMISSIONS.USER.CREATE_AGENT_AGENCY,
      hasRole: (role: string) => role === ROLE_NAMES.AGENCY_MANAGER,
    } as ReturnType<typeof authHooks.useAuth>);

    renderUserForm({
      initialValues: {
        ...mockCreateUserValues,
        permissions: [manageProfilePermission],
      },
    });

    fireEvent.click(
      screen.getByRole("button", { name: "users.form.buttons.submit_create" }),
    );

    await waitFor(() => expect(mockCreateUser).toHaveBeenCalled());
    expect(mockCreateUser.mock.calls[0][0]).not.toHaveProperty("permissionIds");
  });

  it("should persist direct permissions without submitting role permissions", async () => {
    const inheritedPermission: Permission = {
      id: "permission-inherited",
      name: "INCIDENT_CREATE",
      description: "Create incidents",
      createdAt: "2024-01-01",
      updatedAt: "2024-01-01",
    };
    const directPermission: Permission = {
      id: "permission-direct",
      name: "REPORT_EXPORT",
      description: "Export reports",
      createdAt: "2024-01-01",
      updatedAt: "2024-01-01",
    };
    const roleWithPermission: Role = {
      ...makeRole,
      permissions: [inheritedPermission],
    };
    mockRoles([roleWithPermission]);
    vi.mocked(permissionHooks.usePermissions).mockReturnValue({
      data: [inheritedPermission, directPermission],
      isLoading: false,
    } as Partial<
      ReturnType<typeof permissionHooks.usePermissions>
    > as ReturnType<typeof permissionHooks.usePermissions>);

    renderUserForm({
      initialValues: {
        ...mockExistingUser,
        roles: [roleWithPermission],
        permissions: [inheritedPermission, directPermission],
      },
      submitText: "Submit Update",
    });

    fireEvent.change(screen.getByLabelText("users.form.labels.firstname"), {
      target: { value: "UpdatedName" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Submit Update" }));

    await waitFor(() => expect(mockUpdateUser).toHaveBeenCalled());
    expect(mockUpdateUser.mock.calls[0][0].data.permissionIds).toEqual([
      directPermission.id,
    ]);
  });
});
