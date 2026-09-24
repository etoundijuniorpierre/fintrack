// Jeux de donnees simules pour settings.

import type {
  AgencyResponse,
  ServiceResponse,
  RoleResponse,
  PermissionResponse,
  ReportScheduleResponse,
  IncidentTypeConfigResponse,
  ReportType,
  ReportFormat,
} from "../../api/settings/types";
import { makeUserSummaryResponse } from "../shared/summaryFixtures";

// Fabrique une fixture de test pour parametrage fixtures.
export const makeSettingsPermission = (
  overrides: Partial<PermissionResponse> = {},
): PermissionResponse => ({
  id: "perm-1",
  name: "READ_USERS",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour parametrage fixtures.
export const makeSettingsRole = (
  overrides: Partial<RoleResponse> = {},
): RoleResponse => ({
  id: "role-1",
  name: "Administrateur",
  isSystem: true,
  permissions: [],
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour parametrage fixtures.
export const makeSettingsAgency = (
  overrides: Partial<AgencyResponse> = {},
): AgencyResponse => ({
  id: "agency-1",
  name: "Agence Principale",
  code: "AP",
  isActive: true,
  members: [],
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour parametrage fixtures.
export const makeSettingsService = (
  overrides: Partial<ServiceResponse> = {},
): ServiceResponse => ({
  id: "service-1",
  name: "Informatique",
  isActive: true,
  members: [],
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour parametrage fixtures.
export const makeSettingsIncidentType = (
  overrides: Partial<IncidentTypeConfigResponse> = {},
): IncidentTypeConfigResponse => ({
  id: "type-1",
  name: "TECHNICAL",
  displayName: "Technical Issue",
  isActive: true,
  requiresValidation: true,
  requiresCauseAnalysis: false,
  emailNotificationsEnabled: false,
  treaterRoles: ["ASSIGNEE"],
  resolverRoles: ["SOURCE_AGENCY_MANAGER"],
  closerRoles: ["ASSIGNEE"],
  reopenerRoles: ["SOURCE_AGENCY_MANAGER"],
  validatorScope: "AGENCY_MANAGER",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});

// Fabrique une fixture de test pour parametrage fixtures.
export const makeReportSchedule = (
  overrides: Partial<ReportScheduleResponse> = {},
): ReportScheduleResponse => ({
  id: "schedule-1",
  name: "Daily Report",
  type: "DAILY" as ReportType,
  format: "PDF" as ReportFormat,
  recipientEmails: ["admin@finstar.com"],
  isActive: true,
  createdBy: makeUserSummaryResponse(),
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  ...overrides,
});
