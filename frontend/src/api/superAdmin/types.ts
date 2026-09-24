// Centralise les contrats de supervision Super Admin.

export type SuperAdminServiceKey =
  | "user"
  | "incident"
  | "document"
  | "notification"
  | "reporting"
  | "audit";

// Centralise la logique d'interface liee a super administrateur tab key.
export type SuperAdminTabKey =
  | "governance"
  | "health"
  | "controls-quality"
  | "operations"
  | "audit-overview"
  | "system-config"
  | "reporting-overview";

// Centralise la logique d'interface liee a service sante response.
export interface ServiceHealthResponse {
  status?: string;
  components?: Record<string, unknown>;
}

// Centralise la logique d'interface liee a service sante item.
export interface ServiceHealthItem {
  key: SuperAdminServiceKey;
  label: string;
  baseUrl: string;
  status: "UP" | "DOWN" | "UNKNOWN";
  endpoint: string;
  responseTimeMs?: number;
  lastCheckedAt?: string;
  recentErrors?: number;
  recentLogs?: Record<string, unknown>[];
  components?: Record<string, unknown>;
}

// Centralise la logique d'interface liee a system sante section.
export interface SystemHealthSection {
  services: ServiceHealthItem[];
  _meta?: SectionMeta;
}

// Centralise la logique d'interface liee a section meta.
export interface SectionMeta {
  section?: string;
  generatedAt?: string;
  buildTimeMs?: number;
  degraded?: string[];
}

// Centralise la logique d'interface liee a governance section.
export interface GovernanceSection extends Record<string, unknown> {
  _meta?: SectionMeta;
}
// Centralise la logique d'interface liee a controls qualite section.
export interface ControlsQualitySection {
  permissions: Record<string, unknown>;
  dataQuality: Record<string, unknown>;
  _meta?: SectionMeta;
}

// Centralise la logique d'interface liee a operations section.
export interface OperationsSection {
  notifications: Record<string, unknown>;
  _meta?: SectionMeta;
}

// Centralise la logique d'interface liee a audit overview section.
export interface AuditOverviewSection extends Record<string, unknown> {
  _meta?: SectionMeta;
}

// Centralise la logique d'interface liee a system configuration section.
export interface SystemConfigSection extends Record<string, unknown> {
  _meta?: SectionMeta;
}

// Centralise la logique d'interface liee a reporting overview section.
export interface ReportingOverviewSection extends Record<string, unknown> {
  _meta?: SectionMeta;
}

// /overview deprecated mais conserve pour retro-compat.
export interface SuperAdminOverview {
  governance: Record<string, unknown>;
  systemHealth: ServiceHealthItem[];
  audit: Record<string, unknown>;
  permissions: Record<string, unknown>;
  systemConfig: Record<string, unknown>;
  dataQuality: Record<string, unknown>;
  reporting: Record<string, unknown>;
  notifications: Record<string, unknown>;
  meta?: Record<string, unknown>;
}

export interface BackupTriggerResponse {
  success: boolean;
  message: string;
  timestamp: string;
  postgresStatus: string;
  mongoStatus: string;
  b2SyncStatus: string;
  backupDirectory: string;
}

// Reglage e-mail d'un evenement (interrupteur + utilisateurs exclus).
export interface EmailNotificationEventSetting {
  event: string;
  category: string;
  enabled: boolean;
  supportsExclusion: boolean;
  excludedUserIds: string[];
}

// Catalogue des reglages e-mail configurables par evenement.
export interface EmailNotificationSettings {
  events: EmailNotificationEventSetting[];
}
