// Jeux de donnees simules pour incident.

import type {
  IncidentResponse,
  IncidentSummaryResponse,
  IncidentTypeConfigResponse,
  IncidentCommentResponse,
  IncidentHistoryResponse,
} from "../../api/incident/types";
import { makeAgencySummaryResponse } from "../shared/summaryFixtures";

// Fabrique une fixture de test pour incident fixtures.
const defaultUser = () => ({
  id: "user-1",
  username: "jdoe",
  firstName: "John",
  lastName: "Doe",
  email: "john.doe@finstar.com",
});

// Fabrique une fixture de test pour incident fixtures.
export const makeIncidentTypeConfig = (
  overrides: Partial<IncidentTypeConfigResponse> = {},
): IncidentTypeConfigResponse => ({
  id: "type-1",
  name: "TECHNICAL",
  displayName: "Technical Issue",
  description: "A technical incident",
  isActive: true,
  slaHours: 24,
  requiresValidation: true,
  requiresCauseAnalysis: false,
  emailNotificationsEnabled: false,
  treaterRoles: ["ASSIGNEE"],
  resolverRoles: ["SOURCE_AGENCY_MANAGER"],
  closerRoles: ["ASSIGNEE"],
  reopenerRoles: ["SOURCE_AGENCY_MANAGER"],
  validatorScope: "AGENCY_MANAGER",
  ...overrides,
});

// Fabrique une fixture de test pour incident fixtures.
export const makeIncidentSummary = (
  overrides: Partial<IncidentSummaryResponse> = {},
): IncidentSummaryResponse => ({
  id: "incident-1",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-02T00:00:00Z",
  title: "Sample Incident",
  type: makeIncidentTypeConfig(),
  criticality: "MEDIUM",
  status: "OPEN",
  createdBy: defaultUser(),
  agency: makeAgencySummaryResponse(),
  ...overrides,
});

// Fabrique une fixture de test pour incident fixtures.
export const makeIncidentComment = (
  overrides: Partial<IncidentCommentResponse> = {},
): IncidentCommentResponse => ({
  id: "comment-1",
  createdAt: "2024-01-01T10:00:00Z",
  updatedAt: "2024-01-01T10:00:00Z",
  author: defaultUser(),
  content: "This is a comment.",
  isInternal: false,
  ...overrides,
});

// Fabrique une fixture de test pour incident fixtures.
export const makeIncidentHistory = (
  overrides: Partial<IncidentHistoryResponse> = {},
): IncidentHistoryResponse => ({
  id: "history-1",
  createdAt: "2024-01-01T08:00:00Z",
  updatedAt: "2024-01-01T08:00:00Z",
  user: defaultUser(),
  action: "CREATION",
  ...overrides,
});

// Fabrique une fixture de test pour incident fixtures.
export const makeIncidentResponse = (
  overrides: Partial<IncidentResponse> = {},
): IncidentResponse => ({
  ...makeIncidentSummary(),
  description: "Detailed description of the incident.",
  history: [makeIncidentHistory()],
  comments: [makeIncidentComment()],
  ...overrides,
  isReopenExpired: overrides.isReopenExpired ?? false,
  isMaxReopenReached: overrides.isMaxReopenReached ?? false,
});

