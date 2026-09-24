// Jeux de donnees simules pour auth.

import type { AuthResponse } from "../../api/user/types";

// Fabrique une fixture de test pour authentification fixtures.
export const makeAuthResponse = (
  overrides: Partial<AuthResponse> = {},
): AuthResponse => ({
  token: "mock-jwt-token",
  id: "user-1",
  username: "jdoe",
  roles: ["Administrateur"],
  permissions: ["READ_USERS", "WRITE_USERS"],
  isFirstLogin: false,
  isActive: true,
  ...overrides,
});
