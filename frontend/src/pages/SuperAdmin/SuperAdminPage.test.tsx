// Tests frontend : verifie le comportement de super-administration page.test.

import { describe, expect, it, vi, beforeEach } from "vitest";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import SuperAdminPage from "./SuperAdminPage";
import { renderWithProviders } from "../../test-utils/renderWithProviders";
import { superAdminApi } from "../../api/superAdmin";
import type {
  AuditOverviewSection,
  ControlsQualitySection,
  GovernanceSection,
  OperationsSection,
  ReportingOverviewSection,
  ServiceHealthItem,
  SystemConfigSection,
} from "../../api/superAdmin";

// Meme raison que AuditPage : page a onglets lourde en jsdom, ~10 s par test seul,
// au-dela des 30 s globales sous charge. Marge locale, seuil global inchange.
// Ce n'est pas un correctif de lenteur : alleger ce montage reste a faire.
vi.setConfig({ testTimeout: 60_000 });

vi.mock("react-i18next", async (importOriginal) => {
  const actual = await importOriginal<typeof import("react-i18next")>();
  return {
    ...actual,
    useTranslation: () => ({
      t: (key: string, fallbackOrOptions?: string | Record<string, unknown>) =>
        typeof fallbackOrOptions === "string" ? fallbackOrOptions : key,
      i18n: { changeLanguage: vi.fn() },
    }),
  };
});

let isSuperAdmin = true;

vi.mock("../../hooks/auth/useAuth", () => ({
  useAuth: () => ({
    user: {
      id: "super-1",
      username: "root",
      roles: ["SUPER_ADMIN"],
      permissions: [],
    },
    isAuthenticated: true,
    // Le gate d'acces suit desormais USER_CREATE_ADMIN ; les autres permissions
    // restent accordees pour ne pas masquer les sections testees.
    hasPermission: (perm: string) =>
      perm === "USER_CREATE_ADMIN" ? isSuperAdmin : true,
    hasRole: (role: string) => isSuperAdmin && role === "SUPER_ADMIN",
  }),
}));

vi.mock("../../api/superAdmin", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/superAdmin")>();
  return {
    ...actual,
    superAdminApi: {
      getGovernance: vi.fn(),
      getSystemHealth: vi.fn(),
      getControlsQuality: vi.fn(),
      getOperations: vi.fn(),
      getAuditOverview: vi.fn(),
      getSystemConfigSection: vi.fn(),
      getReportingOverview: vi.fn(),
      getOverview: vi.fn(),
    },
  };
});

const governance: GovernanceSection = {
  totalIncidents: 12,
  activeIncidents: 5,
  activeUsers: 8,
  agencies: 2,
  services: 3,
  avgClosureHours: 7.4,
  byStatus: { IN_PROGRESS: 5, REJECTED: 1 },
  byCriticality: { CRITICAL: 2, LOW: 10 },
  byAgency: { Finstar: 8 },
  byService: { IT: 6 },
  topServices: [{ name: "IT", count: 6 }],
  topAgencies: [{ name: "Finstar", count: 8 }],
  rejectedRate: 8.3,
  reopenedRate: 4.1,
  transferredRate: 16.7,
  notificationsFailed: 1,
  superAdminKpis: { criticalAnomalies: 2, failedEmails: 1 },
  visibleAnomalies: [
    {
      key: "users-without-role",
      label: "Users without role",
      count: 2,
      severity: "medium",
    },
  ],
};

const health: ServiceHealthItem[] = [
  {
    key: "user",
    label: "User service",
    baseUrl: "http://user-service:8081",
    status: "UP",
    endpoint: "http://user-service:8081/actuator/health",
    responseTimeMs: 12,
    lastCheckedAt: "2026-05-28T10:00:00Z",
    recentErrors: 0,
    recentLogs: [],
  },
  {
    key: "notification",
    label: "Notification service",
    baseUrl: "http://notification-service:8084",
    status: "DOWN",
    endpoint: "http://notification-service:8084/actuator/health",
    responseTimeMs: 50,
    lastCheckedAt: "2026-05-28T10:00:00Z",
    recentErrors: 1,
    recentLogs: [
      {
        id: "err-1",
        action: "EMAIL_SEND",
        status: "FAILED",
        username: "system",
      },
    ],
  },
];

const controlsQuality: ControlsQualitySection = {
  permissions: {
    criticalPermissions: ["REPORT_DELETE", "INCIDENT_VIEW_ALL"],
    systemRoles: [{ name: "SUPER_ADMIN" }],
    rolePermissionMatrix: {
      SUPER_ADMIN: ["REPORT_DELETE", "INCIDENT_VIEW_ALL"],
    },
    changeAuditRequired: true,
    changeAuditObserved: true,
  },
  dataQuality: {
    issues: [
      {
        key: "users-without-role",
        label: "Users without role",
        count: 2,
        severity: "medium",
      },
    ],
    unusedIncidentTypes: [{ id: "type-1", name: "LEGACY" }],
  },
};

const operations: OperationsSection = {
  notifications: {
    total: 6,
    failed: 2,
    pending: 1,
    failedNotifications: [
      {
        id: "notif-1",
        type: "EMAIL",
        recipient: "foo@example.com",
        subject: "Test",
        diagnosis: "DELIVERY_FAILED",
        recommendedAction: "CHECK_PROVIDER_OR_TEMPLATE",
        retryCount: 1,
        maxRetry: 5,
      },
    ],
    invalidRecipients: [
      { id: "notif-2", recipient: "bad-recipient", subject: "Invalid" },
    ],
    volumeByType: { REPORT: 3, INCIDENT: 2 },
    byChannel: {
      EMAIL: {
        total: 5,
        sent: 3,
        failed: 2,
        pending: 0,
        byStatus: { SENT: 3, FAILED: 2 },
      },
      INTERNAL: {
        total: 1,
        sent: 1,
        failed: 0,
        pending: 0,
        byStatus: { SENT: 1 },
      },
    },
  },
};

const auditOverview: AuditOverviewSection = {
  byAction: { ROLE_UPDATE: 1 },
  byStatus: { SUCCESS: 1 },
  recentLogs: [
    {
      id: "audit-1",
      timestamp: "2026-05-28T10:00:00Z",
      username: "root",
      action: "ROLE_UPDATE",
      status: "SUCCESS",
    },
  ],
};

const systemConfig: SystemConfigSection = {
  businessThresholds: {
    defaultSlaHours: 24,
    criticalIncidentHours: 4,
    slaReminderIntervalHours: 24,
    maxTransfersBeforeAlert: 2,
    notificationMaxRetryCount: 5,
    reportRetentionDays: 20,
    loginMaxFailedAttempts: 5,
    tempPasswordValidityMinutes: 30,
  },
};

const reportingOverview: ReportingOverviewSection = {
  total: 4,
  failed: 1,
  pending: 1,
  available: 2,
  failedReports: [
    {
      id: "report-1",
      name: "Monthly failed",
      type: "DAILY",
      status: "FAILED",
      createdAt: "2026-05-28T10:00:00Z",
      errorMessage: "boom",
    },
  ],
  pendingReports: [
    {
      id: "report-2",
      name: "Daily pending",
      type: "DAILY",
      status: "GENERATING",
      createdAt: "2026-05-28T10:00:00Z",
    },
  ],
  availableWithoutFileReports: [
    {
      id: "report-3",
      name: "Available without file",
      type: "DAILY",
      status: "AVAILABLE",
      createdAt: "2026-05-28T10:00:00Z",
    },
  ],
};

describe("SuperAdminPage (lazy tabs)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    isSuperAdmin = true;
    vi.mocked(superAdminApi.getGovernance).mockResolvedValue(governance);
    vi.mocked(superAdminApi.getSystemHealth).mockResolvedValue({
      services: health,
    });
    vi.mocked(superAdminApi.getControlsQuality).mockResolvedValue(
      controlsQuality,
    );
    vi.mocked(superAdminApi.getOperations).mockResolvedValue(operations);
    vi.mocked(superAdminApi.getAuditOverview).mockResolvedValue(auditOverview);
    vi.mocked(superAdminApi.getSystemConfigSection).mockResolvedValue(
      systemConfig,
    );
    vi.mocked(superAdminApi.getReportingOverview).mockResolvedValue(
      reportingOverview,
    );
  });

  it("only fetches the active tab on initial load (overview = governance + health)", async () => {
    renderWithProviders(<SuperAdminPage />);

    await waitFor(() => expect(superAdminApi.getGovernance).toHaveBeenCalled());
    expect(superAdminApi.getSystemHealth).toHaveBeenCalled();

    expect(superAdminApi.getControlsQuality).not.toHaveBeenCalled();
    expect(superAdminApi.getOperations).not.toHaveBeenCalled();
    expect(superAdminApi.getAuditOverview).not.toHaveBeenCalled();
    expect(superAdminApi.getSystemConfigSection).not.toHaveBeenCalled();
    expect(superAdminApi.getReportingOverview).not.toHaveBeenCalled();

    expect(await screen.findByText("User service")).toBeInTheDocument();
    expect(screen.getByText("Notification service")).toBeInTheDocument();
  });

  it("fetches a tab only when activated", async () => {
    renderWithProviders(<SuperAdminPage />);
    await waitFor(() => expect(superAdminApi.getGovernance).toHaveBeenCalled());

    fireEvent.click(screen.getByText("superAdmin.tabs.controls.label"));
    await waitFor(() =>
      expect(superAdminApi.getControlsQuality).toHaveBeenCalled(),
    );
    // Tous les controles sont listes par cle (le mock t renvoie la cle) ; users-without-role a un compte > 0.
    expect(await screen.findByText("users-without-role")).toBeInTheDocument();
    // Q4: la colonne "Permissions critiques" a ete retiree de l'onglet 2 on ne
    // verifie plus la presence de INCIDENT_VIEW_ALL. La section qualite reste seule.

    fireEvent.click(screen.getByText("superAdmin.tabs.operations.label"));
    await waitFor(() => expect(superAdminApi.getOperations).toHaveBeenCalled());
    expect(await screen.findByText("foo@example.com")).toBeInTheDocument();

    fireEvent.click(screen.getByText("superAdmin.tabs.reporting.label"));
    await waitFor(() =>
      expect(superAdminApi.getReportingOverview).toHaveBeenCalled(),
    );
    expect(await screen.findByText("Monthly failed")).toBeInTheDocument();
    expect(screen.getByText("Daily pending")).toBeInTheDocument();
  });

  it("does not call any backend endpoint for non Super Admin users", () => {
    isSuperAdmin = false;

    renderWithProviders(<SuperAdminPage />);

    expect(superAdminApi.getGovernance).not.toHaveBeenCalled();
    expect(superAdminApi.getSystemHealth).not.toHaveBeenCalled();
    expect(superAdminApi.getControlsQuality).not.toHaveBeenCalled();
    expect(superAdminApi.getOperations).not.toHaveBeenCalled();
    expect(superAdminApi.getAuditOverview).not.toHaveBeenCalled();
    expect(superAdminApi.getSystemConfigSection).not.toHaveBeenCalled();
    expect(superAdminApi.getReportingOverview).not.toHaveBeenCalled();
  });
});
