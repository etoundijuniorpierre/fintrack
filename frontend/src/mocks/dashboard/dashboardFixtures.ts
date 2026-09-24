// Jeux de donnees simules pour dashboard.
import type {
  IncidentStatsResponse,
  UserStatsResponse,
  DashboardConfig,
  TypeDistributionItem,
} from "../../types/dashboard";

// Fabrique une fixture de test pour tableau de bord fixtures.
export const makeTypeDistributionItem = (
  overrides: Partial<TypeDistributionItem> = {},
): TypeDistributionItem => ({
  typeId: "type-1",
  typeName: "TECHNICAL",
  count: 10,
  ...overrides,
});

// Fabrique une fixture de test pour tableau de bord fixtures.
export const makeIncidentStats = (
  overrides: Partial<IncidentStatsResponse> = {},
): IncidentStatsResponse => ({
  openCount: 15,
  pendingCount: 5,
  resolvedCount: 25,
  totalCount: 45,
  typeDistribution: [makeTypeDistributionItem()],
  openCountPreviousPeriod: 10,
  pendingCountPreviousPeriod: 8,
  resolvedCountPreviousPeriod: 20,
  period: "LAST_30_DAYS",
  ...overrides,
});

// Fabrique une fixture de test pour tableau de bord fixtures.
export const makeUserStats = (
  overrides: Partial<UserStatsResponse> = {},
): UserStatsResponse => ({
  totalCount: 100,
  activeCount: 85,
  inactiveCount: 15,
  connectedCount: 12,
  neverConnectedCount: 5,
  lockedCount: 2,
  firstLoginPendingCount: 8,
  recentlyActiveCount: 40,
  newUsersInPeriod: 5,
  ...overrides,
});

// Fabrique une fixture de test pour tableau de bord fixtures.
export const makeDashboardConfig = (
  overrides: Partial<DashboardConfig> = {},
): DashboardConfig => ({
  userId: "user-1",
  visibleWidgets: ["INCIDENT_TOTAL", "INCIDENT_ACTIVE", "INCIDENT_CLOSED"],
  widgetOrder: ["INCIDENT_TOTAL", "INCIDENT_ACTIVE", "INCIDENT_CLOSED"],
  lastUpdated: "2024-01-01T12:00:00Z",
  ...overrides,
});
