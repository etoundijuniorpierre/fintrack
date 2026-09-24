// Tests frontend : verifie le comportement de permissions section.test.

import { describe, it, expect, vi, beforeEach } from "vitest";
import { screen } from "@testing-library/react";
import { renderWithProviders } from "../../../../test-utils/renderWithProviders";
import PermissionsSection from "./PermissionsSection";
import type { Permission, Role } from "../../../../api/user/types";

vi.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string, opts?: { defaultValue?: string }) =>
      opts?.defaultValue ?? key,
  }),
}));

// Fabrique une fixture de test pour permissions section.test.
const makePermission = (name: string, id: string): Permission => ({
  id,
  name,
  description: `${name} description`,
  createdAt: "",
  updatedAt: "",
});

// Fabrique une fixture de test pour permissions section.test.
const makeRole = (permissions: Permission[], id: string = "role-1"): Role => ({
  id,
  name: "AGENT",
  description: "",
  permissions,
  createdAt: "",
  updatedAt: "",
});

describe("PermissionsSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should show empty state when roles and permissions are both empty", () => {
    renderWithProviders(<PermissionsSection roles={[]} permissions={[]} />);
    expect(
      screen.getByText("myProfile.messages.no_permissions"),
    ).toBeInTheDocument();
  });

  it("should show empty state when role has no permissions and direct list is empty", () => {
    renderWithProviders(
      <PermissionsSection roles={[makeRole([], "role-1")]} permissions={[]} />,
    );
    expect(
      screen.getByText("myProfile.messages.no_permissions"),
    ).toBeInTheDocument();
  });

  it("should render permissions from roles", () => {
    const perm = makePermission("INCIDENT_CREATE", "permission-1");
    renderWithProviders(
      <PermissionsSection
        roles={[makeRole([perm], "role-1")]}
        permissions={[]}
      />,
    );
    expect(screen.getByText("INCIDENT_CREATE")).toBeInTheDocument();
  });

  it("should render direct permissions", () => {
    const perm = makePermission("USER_VIEW_ALL", "permission-1");
    renderWithProviders(<PermissionsSection roles={[]} permissions={[perm]} />);
    expect(screen.getByText("USER_VIEW_ALL")).toBeInTheDocument();
  });

  it("should deduplicate permissions that appear in both roles and direct list", () => {
    const perm = makePermission("USER_UPDATE", "permission-1");
    const role = makeRole([perm], "role-1");
    renderWithProviders(
      <PermissionsSection roles={[role]} permissions={[perm]} />,
    );
    expect(screen.getAllByText("USER_UPDATE")).toHaveLength(1);
  });

  it("should group permissions by prefix", () => {
    const perms = [
      makePermission("INCIDENT_CREATE", "permission-1"),
      makePermission("USER_VIEW_ALL", "permission-2"),
    ];
    renderWithProviders(<PermissionsSection roles={[]} permissions={perms} />);
    expect(screen.getByText("INCIDENT")).toBeInTheDocument();
    expect(screen.getByText("USER")).toBeInTheDocument();
  });

  it("should show both permissions when role and direct list have no overlap", () => {
    const rolePerm = makePermission("ROLE_PERM_A", "permission-1");
    const directPerm = makePermission("DIR_PERM_B", "permission-2");
    renderWithProviders(
      <PermissionsSection
        roles={[makeRole([rolePerm], "role-1")]}
        permissions={[directPerm]}
      />,
    );
    expect(screen.getByText("ROLE_PERM_A")).toBeInTheDocument();
    expect(screen.getByText("DIR_PERM_B")).toBeInTheDocument();
  });

  it("should show one tag when role and direct list share the same permission", () => {
    const perm = makePermission("SHARED_PERM", "permission-1");
    renderWithProviders(
      <PermissionsSection
        roles={[makeRole([perm], "role-1")]}
        permissions={[perm]}
      />,
    );
    expect(screen.getAllByText("SHARED_PERM")).toHaveLength(1);
  });

  it("should render permission tags with correct ARIA roles", () => {
    const perm = makePermission("INCIDENT_CREATE", "permission-1");
    renderWithProviders(<PermissionsSection roles={[]} permissions={[perm]} />);
    expect(screen.getByRole("list", { name: "INCIDENT" })).toBeInTheDocument();
    expect(screen.getByRole("listitem")).toBeInTheDocument();
  });

  it("should render permissions from multiple roles without duplicates", () => {
    const perm1 = makePermission("INCIDENT_CREATE", "permission-1");
    const perm2 = makePermission("USER_VIEW_ALL", "permission-2");
    const role1 = makeRole([perm1], "role-1");
    const role2 = makeRole([perm2], "role-2");
    renderWithProviders(
      <PermissionsSection roles={[role1, role2]} permissions={[]} />,
    );
    expect(screen.getByText("INCIDENT_CREATE")).toBeInTheDocument();
    expect(screen.getByText("USER_VIEW_ALL")).toBeInTheDocument();
  });
});
