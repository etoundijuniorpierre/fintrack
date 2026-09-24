// Declarations de types TypeScript du domaine.

import type { PagedResponse } from "../user/types";

// Centralise la logique d'interface liee a rapport type.
export type ReportType = "DAILY" | "WEEKLY" | "MONTHLY" | "CUSTOM";
// Distingue le contenu du rapport de sa periodicite.
export type ReportContentType =
  | "OPERATIONAL"
  | "INCIDENT_TYPE_ANALYSIS"
  | "INCIDENT_STATUS_OVERVIEW";
// Centralise la logique d'interface liee a rapport generation type.
export type ReportGenerationType = "MANUAL" | "AUTOMATIC";
// Centralise la logique d'interface liee a rapport format.
export type ReportFormat = "PDF" | "EXCEL" | "JSON";
// Centralise la logique d'interface liee a rapport statut.
export type ReportStatus =
  | "PENDING"
  | "GENERATING"
  | "AVAILABLE"
  | "FAILED"
  | string;

// Definit les constantes report status.
export const REPORT_STATUS = {
  PENDING: "PENDING",
  GENERATING: "GENERATING",
  AVAILABLE: "AVAILABLE",
  FAILED: "FAILED",
} as const;

// Centralise la logique d'interface liee a rapport filters.
export interface ReportFilters {
  view?: string;
  incidentTypes?: string[];
  criticalities?: string[];
  statuses?: string[];
  services?: string[];
  assignedUsers?: string[];
  subjectUserId?: string;
  agencyId?: string;
  serviceId?: string;
  subjectUserIds?: string[];
  agencyIds?: string[];
  serviceIds?: string[];
}

// Centralise la logique d'interface liee a rapport generate request.
export interface ReportGenerateRequest {
  type: ReportType;
  contentType?: ReportContentType;
  startDate: string;
  endDate: string;
  format: ReportFormat;
  filters?: ReportFilters;
  metrics?: string[];
  sendEmail?: boolean;
  recipients?: string[];
}

// Centralise la logique d'interface liee a rapport indicateurs.
export interface ReportMetrics {
  totalIncidents?: number;
  activeIncidents?: number;
  closedIncidents?: number;
  rejectedIncidents?: number;
  cancelledIncidents?: number;
  avgClosureHours?: number;
  medianClosureHours?: number;
  p90ClosureHours?: number;
  avgNetClosureHours?: number;
  medianNetClosureHours?: number;
  closureSampleSize?: number;
  cohortCompletion?: Record<string, unknown>;
  resolutionSampleSize?: number;
  avgResolutionHours?: number;
  medianResolutionHours?: number;
  p90ResolutionHours?: number;
  distributionByType?: Record<string, number>;
  distributionByCriticality?: Record<string, number>;
  monthlyResolutions?:
    | Record<string, number>
    | Array<{ month: number | string; value: number }>;
  monthlyAvgClosureHours?:
    | Record<string, number>
    | Array<{ month: number | string; value: number }>;
  assignedToMe?: number;
  transferredByMe?: number;
  closedByMe?: number;
  topServices?: Array<{ serviceName?: string; name?: string; count: number }>;
  topAgencies?: Array<{ name: string; count: number }>;
  topResolvers?: Array<{ name: string; count: number }>;
  incidentsList?: ReportIncidentSummary[];
}

// Centralise la logique d'interface liee a rapport person resume.
export interface ReportPersonSummary {
  id?: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
}

// Centralise la logique d'interface liee a rapport named resume.
export interface ReportNamedSummary {
  id?: string;
  name?: string;
  displayName?: string;
}

// Centralise la logique d'interface liee a rapport incident resume.
export interface ReportIncidentSummary {
  id?: string;
  title?: string;
  description?: string;
  status?: string;
  criticality?: string;
  createdAt?: string;
  incidentDate?: string;
  dueDate?: string;
  resolvedAt?: string;
  closedAt?: string;
  createdBy?: ReportPersonSummary | string;
  assignedTo?: ReportPersonSummary | string;
  agency?: ReportNamedSummary | string;
  type?: ReportNamedSummary | string;
  transferredToService?: ReportNamedSummary | string;
  serviceName?: string;
  cause?: string;
  causeDetail?: string;
  resolutionDescription?: string;
}

// Centralise la logique d'interface liee a rapport response.
export interface ReportResponse {
  id: string;
  name: string;
  type: ReportType;
  contentType?: ReportContentType;
  generationType?: ReportGenerationType;
  period: string;
  generatedAt: string;
  format: ReportFormat;
  status: ReportStatus;
  fileSize?: number;
  downloadUrl?: string;
  createdBy?: string;
  agencyId?: string;
  serviceId?: string;
  scope?: string;
  createdByLabel?: string;
  filters?: Record<string, unknown>;
  requestedMetrics?: string[];
  metrics?: ReportMetrics;
  recipients?: string[];
}

// Centralise la logique d'interface liee a rapport page response.
export type ReportPageResponse = PagedResponse<ReportResponse>;
