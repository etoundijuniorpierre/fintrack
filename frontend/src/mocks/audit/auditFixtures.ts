// Jeux de donnees simules pour audit.

import type {
  AuditEnumResponse,
  AuditLogResponse,
  PagedAuditResponse,
  UserSummaryResponse,
} from "../../api/audit/types";
import type { AuditAction, AuditStatus } from "../../api/audit/types";

// Fabrique une fixture de test pour audit fixtures.
const defaultAuditUser = (): UserSummaryResponse => ({
  id: "user-1",
  username: "jdoe",
  firstName: "John",
  lastName: "Doe",
  email: "john.doe@finstar.com",
});

// Fabrique une fixture de test pour audit fixtures.
export const makeAuditLog = (
  overrides: Partial<AuditLogResponse> = {},
): AuditLogResponse => ({
  id: "audit-1",
  timestamp: "2024-01-01T08:00:00Z",
  user: defaultAuditUser(),
  username: "jdoe",
  roles: ["Administrateur"],
  action: "LOGIN_SUCCESS" as AuditAction,
  resourceType: "AUTH",
  status: "SUCCESS" as AuditStatus,
  ...overrides,
});

// Fabrique une fixture de test pour audit fixtures.
export const makePagedAuditResponse = <T>(
  items: T[],
  overrides: Partial<PagedAuditResponse<T>> = {},
): PagedAuditResponse<T> => ({
  content: items,
  totalElements: items.length,
  totalPages: 1,
  size: items.length || 10,
  number: 0,
  ...overrides,
});
// Fabrique une fixture de test pour audit fixtures.
export const makeAuditEnumResponse = (
  overrides: Partial<AuditEnumResponse> = {},
): AuditEnumResponse => ({
  code: "LOGIN_SUCCESS",
  name: "Login Success",
  description: "User logged in successfully",
  ...overrides,
});
