// Helpers de validation des profils et roles.

import type { Role, Permission } from "../../api/user/types";

// Fabrique une fixture de test pour roles.
export const makePermission = (
  overrides: Partial<Permission> = {},
): Permission => ({
  id: "perm-1",
  name: "READ_USERS",
  description: "Can read users",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour roles.
export const makeRole = (overrides: Partial<Role> = {}): Role => ({
  id: "role-1",
  name: "Administrateur",
  description: "Admin role",
  isSystem: true,
  permissions: [],
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour roles.
export const makeRoles = (count: number = 2): Role[] => {
  return Array.from({ length: count }, (_, index) =>
    makeRole({
      id: `role-${index + 1}`,
      name: index === 0 ? "Administrateur" : "Agent",
      description: index === 0 ? "Admin role" : "Agent role",
      isSystem: index === 0,
    }),
  );
};
