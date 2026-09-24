// Client API frontend : regroupe les echanges HTTP de super-administration API.

import apiClient from "../client";
import { SERVICE_BASES } from "../base";
import type {
  AuditOverviewSection,
  ControlsQualitySection,
  GovernanceSection,
  OperationsSection,
  ReportingOverviewSection,
  ServiceHealthItem,
  ServiceHealthResponse,
  SuperAdminOverview,
  SuperAdminServiceKey,
  SystemHealthSection,
  SystemConfigSection,
} from "./types";

const SERVICE_LABELS: Record<SuperAdminServiceKey, string> = {
  user: "User service",
  incident: "Incident service",
  document: "Document service",
  notification: "Notification service",
  reporting: "Reporting service",
  audit: "Audit service",
};

const SERVICE_URLS: Record<SuperAdminServiceKey, string> = {
  user: SERVICE_BASES.USER,
  incident: SERVICE_BASES.INCIDENT,
  document: SERVICE_BASES.DOCUMENT,
  notification: SERVICE_BASES.NOTIFICATION,
  reporting: SERVICE_BASES.REPORTING,
  audit: SERVICE_BASES.AUDIT,
};

// Expose la constante SERVICE_KEYS utilisee par super-administration API.
const SERVICE_KEYS = Object.keys(SERVICE_URLS) as SuperAdminServiceKey[];
// Expose la constante SUPER_ADMIN_BASE utilisee par super-administration API.
const SUPER_ADMIN_BASE = `${SERVICE_BASES.REPORTING}/super-admin`;

// Normalise les valeurs de super-administration API avant utilisation.
const normalizeStatus = (status?: string): ServiceHealthItem["status"] => {
  if (status === "UP") return "UP";
  if (status === "DOWN") return "DOWN";
  return "UNKNOWN";
};

// Prepare super admin api pour super-administration API.
export const superAdminApi = {
  // ---------- 7 endpoints lazy par onglet ----------

  getGovernance: async (signal?: AbortSignal): Promise<GovernanceSection> => {
    const { data } = await apiClient.get<GovernanceSection>(
      `${SUPER_ADMIN_BASE}/governance`,
      { signal },
    );
    return data;
  },

  getSystemHealth: async (
    signal?: AbortSignal,
  ): Promise<SystemHealthSection> => {
    const { data } = await apiClient.get<SystemHealthSection>(
      `${SUPER_ADMIN_BASE}/health`,
      { signal },
    );
    return data;
  },

  getControlsQuality: async (
    signal?: AbortSignal,
  ): Promise<ControlsQualitySection> => {
    const { data } = await apiClient.get<ControlsQualitySection>(
      `${SUPER_ADMIN_BASE}/controls-quality`,
      { signal },
    );
    return data;
  },

  getOperations: async (signal?: AbortSignal): Promise<OperationsSection> => {
    const { data } = await apiClient.get<OperationsSection>(
      `${SUPER_ADMIN_BASE}/operations`,
      { signal },
    );
    return data;
  },

  getAuditOverview: async (
    signal?: AbortSignal,
  ): Promise<AuditOverviewSection> => {
    const { data } = await apiClient.get<AuditOverviewSection>(
      `${SUPER_ADMIN_BASE}/audit-overview`,
      { signal },
    );
    return data;
  },

  getSystemConfigSection: async (
    signal?: AbortSignal,
  ): Promise<SystemConfigSection> => {
    const { data } = await apiClient.get<SystemConfigSection>(
      `${SUPER_ADMIN_BASE}/system-config`,
      { signal },
    );
    return data;
  },

  getReportingOverview: async (
    signal?: AbortSignal,
  ): Promise<ReportingOverviewSection> => {
    const { data } = await apiClient.get<ReportingOverviewSection>(
      `${SUPER_ADMIN_BASE}/reporting-overview`,
      { signal },
    );
    return data;
  },

  getOverview: async (signal?: AbortSignal): Promise<SuperAdminOverview> => {
    const { data } = await apiClient.get<SuperAdminOverview>(
      `${SUPER_ADMIN_BASE}/overview`,
      { signal },
    );
    return data;
  },

  // ---------- Mutations ----------

  updateThresholds: async (
    thresholds: Record<string, number>,
  ): Promise<Record<string, unknown>> => {
    const { data } = await apiClient.patch(
      `${SUPER_ADMIN_BASE}/system-config/thresholds`,
      thresholds,
    );
    return data;
  },

  getEmailNotifications: async (): Promise<
    import("./types").EmailNotificationSettings
  > => {
    const { data } = await apiClient.get<
      import("./types").EmailNotificationSettings
    >(`${SUPER_ADMIN_BASE}/system-config/email-notifications`);
    return data;
  },

  updateEmailNotifications: async (
    settings: import("./types").EmailNotificationSettings,
  ): Promise<import("./types").EmailNotificationSettings> => {
    const { data } = await apiClient.patch<
      import("./types").EmailNotificationSettings
    >(`${SUPER_ADMIN_BASE}/system-config/email-notifications`, settings);
    return data;
  },

  retryReport: async (reportId: string): Promise<Record<string, unknown>> => {
    const { data } = await apiClient.post<Record<string, unknown>>(
      `${SERVICE_BASES.REPORTING}/reports/${reportId}/retry`,
    );
    return data;
  },

  exportAuditCsv: async (filters: {
    action?: string;
    status?: string;
    from?: string;
    to?: string;
    limit?: number;
  }): Promise<Blob> => {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") params.set(k, String(v));
    });
    const query = params.toString();
    const url = `${SUPER_ADMIN_BASE}/audit/export${query ? `?${query}` : ""}`;
    const { data } = await apiClient.get<Blob>(url, { responseType: "blob" });
    return data;
  },

  // ---------- Helpers (direct probe per service, optionnel) ----------

  getServiceHealth: async (
    key: SuperAdminServiceKey,
    signal?: AbortSignal,
  ): Promise<ServiceHealthItem> => {
    const baseUrl = SERVICE_URLS[key];
    const endpoint = `${baseUrl}/actuator/health`;
    const { data } = await apiClient.get<ServiceHealthResponse>(endpoint, {
      signal,
    });

    return {
      key,
      label: SERVICE_LABELS[key],
      baseUrl,
      endpoint,
      status: normalizeStatus(data.status),
    };
  },

  getAllServiceHealth: async (
    signal?: AbortSignal,
  ): Promise<ServiceHealthItem[]> => {
    const results = await Promise.allSettled(
      SERVICE_KEYS.map((key) => superAdminApi.getServiceHealth(key, signal)),
    );

    return results.map((result, index) => {
      const key = SERVICE_KEYS[index];
      if (result.status === "fulfilled") {
        return result.value;
      }

      return {
        key,
        label: SERVICE_LABELS[key],
        baseUrl: SERVICE_URLS[key],
        endpoint: `${SERVICE_URLS[key]}/actuator/health`,
        status: "DOWN",
      };
    });
  },

  getJobs: async (
    service: "user" | "reporting",
  ): Promise<Record<string, unknown>[]> => {
    const baseUrl =
      service === "user" ? SERVICE_URLS.user : SERVICE_URLS.reporting;
    const { data } = await apiClient.get<Record<string, unknown>[]>(
      `${baseUrl}/internal/jobs`,
    );
    return data;
  },

  triggerJob: async (
    service: "user" | "reporting",
    jobName: string,
  ): Promise<{ jobName: string; affected: number }> => {
    const baseUrl =
      service === "user" ? SERVICE_URLS.user : SERVICE_URLS.reporting;
    const { data } = await apiClient.post<{
      jobName: string;
      affected: number;
    }>(`${baseUrl}/internal/jobs/${jobName}/trigger`);
    return data;
  },

  triggerBackup: async (): Promise<import("./types").BackupTriggerResponse> => {
    const { data } = await apiClient.post<import("./types").BackupTriggerResponse>(
      `${SUPER_ADMIN_BASE}/backup/trigger`,
    );
    return data;
  },
};
