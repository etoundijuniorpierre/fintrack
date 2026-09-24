// Navigation utilitaire vers les differents indicateurs du tableau de bord.
import type { IncidentHistoryResponse } from "../api/incident/types";

// Rend le composant PeriodType.
export const PeriodType = {
  TODAY: "TODAY",
  LAST_7_DAYS: "LAST_7_DAYS",
  LAST_30_DAYS: "LAST_30_DAYS",
  THIS_MONTH: "THIS_MONTH",
  LAST_365_DAYS: "LAST_365_DAYS",
  LAST_MONTH: "LAST_MONTH",
  LAST_2_MONTHS: "LAST_2_MONTHS",
  LAST_6_MONTHS: "LAST_6_MONTHS",
  CUSTOM: "CUSTOM",
  ALL: "ALL",
} as const;

// Centralise la logique d'interface liee a period type.
export type PeriodType = (typeof PeriodType)[keyof typeof PeriodType];

// Rend le composant WidgetType.
export const WidgetType = {
  INCIDENT_ACTIVE: "INCIDENT_ACTIVE",
  INCIDENT_CLOSED: "INCIDENT_CLOSED",
  INCIDENT_REJECTED: "INCIDENT_REJECTED",
  INCIDENT_BLOCKED: "INCIDENT_BLOCKED",
  INCIDENT_TOTAL: "INCIDENT_TOTAL",
  INCIDENT_AVG_CLOSURE: "INCIDENT_AVG_CLOSURE",
  INCIDENT_AVG_RESOLUTION: "INCIDENT_AVG_RESOLUTION",
  INCIDENT_ASSIGNED_TO_ME: "INCIDENT_ASSIGNED_TO_ME",
  INCIDENT_TRANSFERRED_BY_ME: "INCIDENT_TRANSFERRED_BY_ME",
  INCIDENT_CLOSED_BY_ME: "INCIDENT_CLOSED_BY_ME",
  INCIDENT_TYPE_DISTRIBUTION: "INCIDENT_TYPE_DISTRIBUTION",
  INCIDENT_CRITICALITY_DISTRIBUTION: "INCIDENT_CRITICALITY_DISTRIBUTION",
  INCIDENT_STATUS_DISTRIBUTION: "INCIDENT_STATUS_DISTRIBUTION",
  INCIDENT_RECENT_ACTIVITY: "INCIDENT_RECENT_ACTIVITY",
  INCIDENT_TOP_SERVICES: "INCIDENT_TOP_SERVICES",
  INCIDENT_TOP_AGENCIES: "INCIDENT_TOP_AGENCIES",
  INCIDENT_TOP_RESOLVERS: "INCIDENT_TOP_RESOLVERS",
  INCIDENT_MONTHLY_CLOSURES: "INCIDENT_MONTHLY_CLOSURES",
  INCIDENT_MONTHLY_AVG_CLOSURE: "INCIDENT_MONTHLY_AVG_CLOSURE",
  INCIDENT_CANCELLED: "INCIDENT_CANCELLED",
  INCIDENT_COHORT_COMPLETION: "INCIDENT_COHORT_COMPLETION",
  INCIDENT_CLOSURE_BY_TYPE: "INCIDENT_CLOSURE_BY_TYPE",
  INCIDENT_CLOSURE_BY_CRITICALITY: "INCIDENT_CLOSURE_BY_CRITICALITY",
  INCIDENT_TRANSFER_RATE: "INCIDENT_TRANSFER_RATE",
  INCIDENT_SLA_COMPLIANCE: "INCIDENT_SLA_COMPLIANCE",
  INCIDENT_REOPEN_RATE: "INCIDENT_REOPEN_RATE",
  INCIDENT_AVG_FIRST_RESPONSE: "INCIDENT_AVG_FIRST_RESPONSE",
  INCIDENT_CREATED_BY_ME: "INCIDENT_CREATED_BY_ME",
  INCIDENT_RESOLVED_BY_ME: "INCIDENT_RESOLVED_BY_ME",
  INCIDENT_INFLOW_OUTFLOW: "INCIDENT_INFLOW_OUTFLOW",
  INCIDENT_AGING: "INCIDENT_AGING",
  INCIDENT_COHORT: "INCIDENT_COHORT",
  INCIDENT_SLA_BREACH_NOW: "INCIDENT_SLA_BREACH_NOW",
  INCIDENT_WORKLOAD: "INCIDENT_WORKLOAD",
  INCIDENT_SCORECARD: "INCIDENT_SCORECARD",
} as const;

// Centralise la logique d'interface liee a widget type.
export type WidgetType = (typeof WidgetType)[keyof typeof WidgetType];

// Centralise la logique d'interface liee a period filter.
export interface PeriodFilter {
  period: PeriodType;
  dateFrom?: string;
  dateTo?: string;
  view?: string;
  agencyId?: string;
  serviceId?: string;
}

// Regroupe les filtres transmis aux endpoints du tableau de bord.
export interface DashboardMetricsFilters {
  agencyId?: string;
  serviceId?: string;
  targetUserId?: string;
  year?: number;
  period?: PeriodType;
  dateFrom?: string;
  dateTo?: string;
}

// Centralise la logique d'interface liee a type distribution item.
export interface TypeDistributionItem {
  typeId: string;
  typeName: string;
  count: number;
}

// Modele les statistiques affichees dans le tableau de bord.
export interface UserStatsResponse {
  totalCount: number;
  activeCount: number;
  inactiveCount: number;
  connectedCount: number;
  neverConnectedCount: number;
  lockedCount: number;
  firstLoginPendingCount: number;
  recentlyActiveCount: number;
  newUsersInPeriod: number;
}

// Centralise la logique d'interface liee a service incident count.
export interface ServiceIncidentCount {
  serviceName: string;
  count: number;
}

// Centralise la logique d'interface liee a named incident count.
export interface NamedIncidentCount {
  name: string;
  count: number;
}

// Centralise la logique d'interface liee a monthly indicateur.
export interface MonthlyMetric {
  month: number;
  value: number;
}

// Centralise la logique d'interface liee a cohort completion.
export interface CohortCompletion {
  size: number;
  closedCount: number;
  p50Hours: number;
  p50Reached: boolean;
  p90Hours: number;
  p90Reached: boolean;
  openMedianAgeHours: number;
  maxElapsedHours: number;
}

// Centralise la logique d'interface liee a named duration metric.
export interface NamedDurationMetric {
  name: string;
  avgHours: number;
  sampleSize: number;
}

// Centralise la logique d'interface liee a efficiency scorecard.
export interface EfficiencyScorecard {
  delayScore: number;
  qualityScore: number;
  throughputScore: number;
  compositeScore: number;
}

// Centralise la logique d'interface liee a dashboard indicateurs response.
export interface DashboardMetricsResponse {
  activeIncidents: number;
  closedIncidents: number;
  resolvedIncidents: number;
  rejectedIncidents: number;
  cancelledIncidents?: number;
  blockedIncidents: number;
  totalIncidents: number;
  openCount: number;
  pendingCount: number;
  openCountPreviousPeriod: number;
  pendingCountPreviousPeriod: number;
  resolvedCountPreviousPeriod: number;
  period: PeriodType;
  effectiveDateFrom?: string;
  effectiveDateTo?: string;
  generatedAt?: string;
  transferRate: number;
  transferCount: number;
  transferDenominator: number;
  slaComplianceRate: number;
  slaCompliantCount: number;
  slaDenominator: number;
  resolutionReopenRate: number;
  reopenedCount: number;
  reopenDenominator: number;
  avgTimeToFirstResponse: number;
  inflow: number;
  outflow: number;
  exitsClosed: number;
  exitsRejected: number;
  exitsCancelled: number;
  backlogReEntries: number;
  netBacklog: number;
  ageDistribution?: Record<string, number>;
  slaBreachNow: number;
  cohortOutcome?: Record<string, number>;
  workload?: NamedIncidentCount[];
  efficiencyScorecard?: EfficiencyScorecard;
  cohortCompletion?: CohortCompletion;
  avgClosureHours: number;
  medianClosureHours?: number;
  p90ClosureHours?: number;
  avgNetClosureHours?: number;
  medianNetClosureHours?: number;
  closureSampleSize?: number;
  resolutionSampleSize?: number;
  avgResolutionHours: number;
  medianResolutionHours?: number;
  p90ResolutionHours?: number;
  transferredByMe: number;
  closedByMe: number;
  createdByMe: number;
  resolvedByMe: number;
  distributionByType: Record<string, number>;
  distributionByCriticality: Record<string, number>;
  distributionByStatus: Record<string, number>;
  recentActivities: IncidentHistoryResponse[];
  assignedToMe: number;
  topServices?: ServiceIncidentCount[];
  topAgencies?: NamedIncidentCount[];
  topResolvers?: NamedIncidentCount[];
  monthlyClosures?: MonthlyMetric[];
  monthlyAvgClosureHours?: MonthlyMetric[];
  closureHoursByType?: NamedDurationMetric[];
  closureHoursByCriticality?: NamedDurationMetric[];
}

// Centralise la logique d'interface liee a comparison entry.
export interface ComparisonEntry {
  id: string;
  name: string;
  metrics: DashboardMetricsResponse;
}

// Centralise la logique d'interface liee a comparison response.
export interface ComparisonResponse {
  entityType: string;
  entries: ComparisonEntry[];
}

// Centralise la logique d'interface liee a dashboard configuration.
export interface DashboardConfig {
  userId: string;
  visibleWidgets: WidgetType[];
  widgetOrder?: WidgetType[];
  lastUpdated: string;
}

// Centralise la logique d'interface liee a incident statistiques response.
export interface IncidentStatsResponse {
  openCount: number;
  pendingCount: number;
  resolvedCount: number;
  totalCount: number;
  typeDistribution: TypeDistributionItem[];
  openCountPreviousPeriod: number;
  pendingCountPreviousPeriod: number;
  resolvedCountPreviousPeriod: number;
  period: string;
}
