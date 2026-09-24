// Constantes frontend : routes, stockage et cles de cache partagees.

export const APP_ROUTES = {
  ROOT: "/",
  LOGIN: "/login",
  DASHBOARD: "/dashboard",
  SUPER_ADMIN: "/dashboard/super-admin",
  USERS: "/dashboard/users",
  INCIDENTS: "/dashboard/incidents",
  INCIDENTS_DETAIL: (id: string) => `/dashboard/incidents/${id}`,
  REPORTS: "/dashboard/reports",
  SETTINGS: "/dashboard/settings",
  SETTINGS_ROLES: "/dashboard/settings/roles",
  SETTINGS_ROLES_CREATE: "/dashboard/settings/roles/new",
  SETTINGS_ROLES_EDIT: (id: string) => `/dashboard/settings/roles/edit/${id}`,
  SETTINGS_ROLES_DETAILS: (id: string) => `/dashboard/settings/roles/${id}`,

  SETTINGS_AGENCIES: "/dashboard/settings/agencies",
  SETTINGS_AGENCIES_CREATE: "/dashboard/settings/agencies/new",
  SETTINGS_AGENCIES_EDIT: (id: string) =>
    `/dashboard/settings/agencies/edit/${id}`,
  SETTINGS_AGENCIES_DETAILS: (id: string) =>
    `/dashboard/settings/agencies/${id}`,

  SETTINGS_SERVICES: "/dashboard/settings/services",
  SETTINGS_SERVICES_CREATE: "/dashboard/settings/services/new",
  SETTINGS_SERVICES_EDIT: (id: string) =>
    `/dashboard/settings/services/edit/${id}`,
  SETTINGS_SERVICES_DETAILS: (id: string) =>
    `/dashboard/settings/services/${id}`,

  SETTINGS_INCIDENT_TYPES: "/dashboard/settings/incident-types",
  SETTINGS_INCIDENT_TYPES_CREATE: "/dashboard/settings/incident-types/new",
  SETTINGS_INCIDENT_TYPES_EDIT: (id: string) =>
    `/dashboard/settings/incident-types/edit/${id}`,
  SETTINGS_INCIDENT_TYPES_DETAILS: (id: string) =>
    `/dashboard/settings/incident-types/${id}`,

  SETTINGS_REPORT_SCHEDULES: "/dashboard/settings/report-schedules",
  SETTINGS_REPORT_SCHEDULES_CREATE: "/dashboard/settings/report-schedules/new",
  SETTINGS_REPORT_SCHEDULES_EDIT: (id: string) =>
    `/dashboard/settings/report-schedules/edit/${id}`,
  SETTINGS_REPORT_SCHEDULES_DETAILS: (id: string) =>
    `/dashboard/settings/report-schedules/${id}`,
  AUDIT: "/dashboard/audit",
  PROFILE: "/dashboard/profile",
  NOTIFICATIONS: "/dashboard/notifications",
  HELP: "/dashboard/help",
} as const;

// Liste les cles de stockage navigateur partagees par l'application.
export const STORAGE_KEYS = {
  TOKEN: "fintrack_token",
  USER: "fintrack_user",
  FIRST_LOGIN: "firstLogin",
  DASHBOARD_VIEW: "dashboard_view",
  DASHBOARD_DISPLAY_MODE: "dashboard_display_mode",
  DASHBOARD_PERIOD: "dashboard_period",
  INCIDENT_VIEW: "incidents_view",
  INCIDENT_DISPLAY_MODE: "incidents_display_mode",
} as const;

// Liste les cles de cache React Query partagees par domaine fonctionnel.
export const QUERY_KEYS = {
  AUTH: {
    USER: ["auth", "user"] as const,
  },
  USERS: {
    ALL: ["users"] as const,
    DETAIL: (id: string) => ["users", id] as const,
  },
  ROLES: {
    ALL: ["roles"] as const,
  },
  AGENCIES: {
    ALL: ["agencies"] as const,
  },
  SERVICES: {
    ALL: ["services"] as const,
  },
  PERMISSIONS: {
    ALL: ["permissions"] as const,
  },
  INCIDENTS: {
    ALL: ["incidents"] as const,
    DETAIL: (id: string) => ["incidents", id] as const,
    COMMENTS: (id: string) => ["incidents", id, "comments"] as const,
    HISTORY: (id: string) => ["incidents", id, "history"] as const,
    STATUSES: ["incidents", "statuses"] as const,
    CRITICALITIES: ["incidents", "criticalities"] as const,
    ACTION_TYPES: ["incidents", "action-types"] as const,
    PERIOD_TYPES: ["incidents", "period-types"] as const,
    TYPES: ["incidents", "types"] as const,
    CAUSES: ["incidents", "causes"] as const,
  },
  DOCUMENTS: {
    BY_INCIDENT: (incidentId: string) =>
      ["documents", "incident", incidentId] as const,
  },
  SETTINGS: {
    INCIDENT_TYPES: ["settings", "incident-types"] as const,
    REPORT_SCHEDULES: ["settings", "report-schedules"] as const,
  },
  AUDIT: {
    ALL: ["audit"] as const,
    DETAIL: (id: string) => ["audit", id] as const,
    BY_USER: (userId: string) => ["audit", "user", userId] as const,
    BY_ACTION: (action: string) => ["audit", "action", action] as const,
    BY_RESOURCE_TYPE: (resourceType: string) =>
      ["audit", "resource", resourceType] as const,
    BY_STATUS: (status: string) => ["audit", "status", status] as const,
    ACTIONS: ["audit", "actions"] as const,
    STATUSES: ["audit", "statuses"] as const,
  },
  NOTIFICATIONS: {
    ALL: ["notifications"] as const,
    DETAIL: (id: string) => ["notifications", id] as const,
    BY_STATUS: (status: string) => ["notifications", "status", status] as const,
    BY_INCIDENT: (incidentId: string) =>
      ["notifications", "incident", incidentId] as const,
    RECIPIENTS: ["notifications", "recipient"] as const,
    BY_RECIPIENT: (recipient: string) =>
      ["notifications", "recipient", recipient] as const,
    TYPES: ["notifications", "types"] as const,
    STATUSES: ["notifications", "statuses"] as const,
  },
  PRESENCE: {
    ONLINE: ["presence", "online"] as const,
  },
  DASHBOARD: {
    STATS: (filter: Record<string, unknown>) =>
      ["dashboard", "stats", filter] as const,
    USER_STATS: (filter: Record<string, unknown>) =>
      ["dashboard", "user-stats", filter] as const,
  },
  SUPER_ADMIN: {
    OVERVIEW: ["super-admin", "overview"] as const,
    GOVERNANCE: ["super-admin", "governance"] as const,
    HEALTH: ["super-admin", "health"] as const,
    CONTROLS_QUALITY: ["super-admin", "controls-quality"] as const,
    OPERATIONS: ["super-admin", "operations"] as const,
    AUDIT_OVERVIEW: ["super-admin", "audit-overview"] as const,
    SYSTEM_CONFIG: ["super-admin", "system-config"] as const,
    REPORTING_OVERVIEW: ["super-admin", "reporting-overview"] as const,
  },
  MY_PROFILE: (id: string) => ["myProfile", id] as const,
} as const;
