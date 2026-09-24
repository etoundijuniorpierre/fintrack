// Jeux de donnees resumes simules.

import type {
  UserSummaryResponse,
  AgencySummaryResponse,
  ServiceSummaryResponse,
  EnumResponse,
} from "../../api/incident/types";

// Fabrique une fixture de test pour summary fixtures.
export const makeUserSummaryResponse = (
  overrides: Partial<UserSummaryResponse> = {},
): UserSummaryResponse => ({
  id: "user-1",
  username: "jdoe",
  firstName: "John",
  lastName: "Doe",
  email: "john.doe@finstar.com",
  ...overrides,
});

// Fabrique une fixture de test pour summary fixtures.
export const makeAgencySummaryResponse = (
  overrides: Partial<AgencySummaryResponse> = {},
): AgencySummaryResponse => ({
  id: "agency-1",
  name: "Main Agency",
  code: "MA",
  ...overrides,
});

// Fabrique une fixture de test pour summary fixtures.
export const makeServiceSummaryResponse = (
  overrides: Partial<ServiceSummaryResponse> = {},
): ServiceSummaryResponse => ({
  id: "service-1",
  name: "IT Support",
  description: "Technical support team",
  ...overrides,
});

// Fabrique une fixture de test pour summary fixtures.
export const makeEnumResponse = (
  overrides: Partial<EnumResponse> = {},
): EnumResponse => ({
  code: "OPEN",
  name: "Open",
  description: "Item is open and awaiting action",
  ...overrides,
});
