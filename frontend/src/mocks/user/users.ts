// Definition des chemins et utilitaires de navigation pour les utilisateurs.

import type { User, UserSummary } from "../../api/user/types";
import { makeRole } from "./roles";
import { makeAgency } from "./agencies";
import { makeService } from "./services";

// Fabrique une fixture de test pour utilisateurs.
export const makeUser = (overrides: Partial<User> = {}): User => ({
  id: "user-1",
  username: "jdoe",
  email: "john.doe@finstar.com",
  firstName: "John",
  lastName: "Doe",
  phoneNumber: 12345678,
  isActive: true,
  isFirstLogin: false,
  lastLogin: "2024-01-01T00:00:00Z",
  roles: [makeRole()],
  permissions: [],
  agency: makeAgency(),
  service: makeService(),
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-02T00:00:00Z",
  modifiedBy: "admin-1",
  ...overrides,
});

// Fabrique une fixture de test pour utilisateurs.
export const makeUsers = (count: number = 3): User[] => {
  return Array.from({ length: count }, (_, index) =>
    makeUser({
      id: `user-${index + 1}`,
      username: `user${index + 1}`,
      email: `user${index + 1}@finstar.com`,
      firstName: `User${index + 1}`,
      lastName: `Test${index + 1}`,
    }),
  );
};
// Fabrique une fixture de test pour utilisateurs.
export const makeUserSummary = (
  overrides: Partial<UserSummary> = {},
): UserSummary => ({
  id: "user-1",
  username: "jdoe",
  firstName: "John",
  lastName: "Doe",
  ...overrides,
});
