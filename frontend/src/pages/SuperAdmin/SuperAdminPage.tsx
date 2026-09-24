// Composant React : porte l'interface de super-administration page.

import { memo, useMemo } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Space, Tabs, Tooltip, Typography } from "antd";
import { ReloadOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { superAdminApi } from "../../api/superAdmin";
import { PageContainer } from "../../components/Layout";
import { PageHeader } from "../../components/ui";
import { useAuth } from "../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../utils/permissions/permissions";
import { APP_ROUTES, QUERY_KEYS } from "../../utils/constants";
import {
  SuperAdminAudit,
  SuperAdminControlsQuality,
  SuperAdminOperations,
  SuperAdminPriorityOverview,
  SuperAdminReporting,
  SuperAdminSystemConfig,
} from "./components/SuperAdminSections";
import styles from "./SuperAdminPage.module.scss";

const { Paragraph, Text } = Typography;

// Type les valeurs tab key utilisees par l'interface.
type TabKey =
  | "overview"
  | "controls"
  | "operations"
  | "audit"
  | "config"
  | "reporting";

const STALE_TIME = {
  governance: 60_000,
  health: 15_000,
  controlsQuality: 120_000,
  operations: 60_000,
  auditOverview: 60_000,
  systemConfig: 5 * 60_000,
  reportingOverview: 120_000,
};

const REFETCH_INTERVAL = {
  governance: 60_000,
  health: 30_000,
  controlsQuality: 5 * 60_000,
  operations: 60_000,
  auditOverview: 2 * 60_000,
  systemConfig: false as const,
  reportingOverview: 2 * 60_000,
};

// Rend le composant SuperAdminPage pour l'interface super-administration page.
const SuperAdminPage = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { hash } = useLocation();
  const { hasPermission } = useAuth();
  // Le backend reserve USER_CREATE_ADMIN au super-administrateur actuel.
  const canAccess = hasPermission(PERMISSIONS.USER.CREATE_ADMIN);
  const activeTab = (hash.replace("#", "") || "overview") as TabKey;

  // 7 useQuery independantes, chacune lazy via "enabled: activeTab === '...'".
  // L'onglet "overview" combine governance + health.
  const governanceQuery = useQuery({
    queryKey: QUERY_KEYS.SUPER_ADMIN.GOVERNANCE,
    queryFn: ({ signal }) => superAdminApi.getGovernance(signal),
    enabled: canAccess && activeTab === "overview",
    staleTime: STALE_TIME.governance,
    refetchInterval: REFETCH_INTERVAL.governance,
  });
  const healthQuery = useQuery({
    queryKey: QUERY_KEYS.SUPER_ADMIN.HEALTH,
    queryFn: ({ signal }) => superAdminApi.getSystemHealth(signal),
    enabled: canAccess && activeTab === "overview",
    staleTime: STALE_TIME.health,
    refetchInterval: REFETCH_INTERVAL.health,
  });
  const controlsQualityQuery = useQuery({
    queryKey: QUERY_KEYS.SUPER_ADMIN.CONTROLS_QUALITY,
    queryFn: ({ signal }) => superAdminApi.getControlsQuality(signal),
    enabled: canAccess && activeTab === "controls",
    staleTime: STALE_TIME.controlsQuality,
    refetchInterval: REFETCH_INTERVAL.controlsQuality,
  });
  const operationsQuery = useQuery({
    queryKey: QUERY_KEYS.SUPER_ADMIN.OPERATIONS,
    queryFn: ({ signal }) => superAdminApi.getOperations(signal),
    enabled: canAccess && activeTab === "operations",
    staleTime: STALE_TIME.operations,
    refetchInterval: REFETCH_INTERVAL.operations,
  });
  const auditQuery = useQuery({
    queryKey: QUERY_KEYS.SUPER_ADMIN.AUDIT_OVERVIEW,
    queryFn: ({ signal }) => superAdminApi.getAuditOverview(signal),
    enabled: canAccess && activeTab === "audit",
    staleTime: STALE_TIME.auditOverview,
    refetchInterval: REFETCH_INTERVAL.auditOverview,
  });
  const systemConfigQuery = useQuery({
    queryKey: QUERY_KEYS.SUPER_ADMIN.SYSTEM_CONFIG,
    queryFn: ({ signal }) => superAdminApi.getSystemConfigSection(signal),
    enabled: canAccess && activeTab === "config",
    staleTime: STALE_TIME.systemConfig,
    refetchInterval: REFETCH_INTERVAL.systemConfig,
  });
  const reportingQuery = useQuery({
    queryKey: QUERY_KEYS.SUPER_ADMIN.REPORTING_OVERVIEW,
    queryFn: ({ signal }) => superAdminApi.getReportingOverview(signal),
    enabled: canAccess && activeTab === "reporting",
    staleTime: STALE_TIME.reportingOverview,
    refetchInterval: REFETCH_INTERVAL.reportingOverview,
  });

  const refreshKeys: Record<TabKey, readonly string[][]> = {
    overview: [
      [...QUERY_KEYS.SUPER_ADMIN.GOVERNANCE],
      [...QUERY_KEYS.SUPER_ADMIN.HEALTH],
    ],
    controls: [[...QUERY_KEYS.SUPER_ADMIN.CONTROLS_QUALITY]],
    operations: [[...QUERY_KEYS.SUPER_ADMIN.OPERATIONS]],
    audit: [[...QUERY_KEYS.SUPER_ADMIN.AUDIT_OVERVIEW]],
    config: [[...QUERY_KEYS.SUPER_ADMIN.SYSTEM_CONFIG]],
    reporting: [[...QUERY_KEYS.SUPER_ADMIN.REPORTING_OVERVIEW]],
  };

  // Traite le rafraichissement des donnees.
  const handleRefresh = (tab: TabKey) => {
    const keys = refreshKeys[tab];
    keys.forEach((queryKey) => {
      queryClient.invalidateQueries({ queryKey });
      queryClient.refetchQueries({ queryKey, type: "active" });
    });
  };

  const isError =
    governanceQuery.isError ||
    healthQuery.isError ||
    controlsQualityQuery.isError ||
    operationsQuery.isError ||
    auditQuery.isError ||
    systemConfigQuery.isError ||
    reportingQuery.isError;

  const isFetchingTab = (tab: TabKey): boolean => {
    switch (tab) {
      case "overview":
        return governanceQuery.isFetching || healthQuery.isFetching;
      case "controls":
        return controlsQualityQuery.isFetching;
      case "operations":
        return operationsQuery.isFetching;
      case "audit":
        return auditQuery.isFetching;
      case "config":
        return systemConfigQuery.isFetching;
      case "reporting":
        return reportingQuery.isFetching;
      default:
        return false;
    }
  };

  const tabItems = useMemo(() => {
    const combineDegraded = (...sources: Array<string[] | undefined>) =>
      Array.from(new Set(sources.flatMap((source) => source ?? [])));

    const tabHeader = (
      tab: TabKey,
      titleKey: string,
      descKey: string,
      degraded?: string[],
    ) => (
      <Space orientation="vertical" size={4} className={styles.tabHeader}>
        <Space>
          <Text strong>{t(titleKey)}</Text>
          <Tooltip title={t("superAdmin.refreshTab")}>
            <Button
              size="small"
              icon={<ReloadOutlined />}
              loading={isFetchingTab(tab)}
              onClick={() => handleRefresh(tab)}
              aria-label={t("superAdmin.refreshTab")}
            />
          </Tooltip>
        </Space>
        <Paragraph type="secondary" className={styles.tabDescription}>
          {t(descKey)}
        </Paragraph>
        {degraded && degraded.length > 0 && (
          <Alert
            type="warning"
            showIcon
            title={t("superAdmin.errors.degradedState")}
            description={`${t("superAdmin.errors.degradedModules")}: ${degraded.join(", ")}`}
            style={{ marginTop: 8 }}
          />
        )}
      </Space>
    );

    return [
      {
        key: "overview",
        label: t("superAdmin.tabs.overview.label"),
        children: (
          <Space
            orientation="vertical"
            size="middle"
            className={styles.fullWidth}
          >
            {tabHeader(
              "overview",
              "superAdmin.tabs.overview.title",
              "superAdmin.tabs.overview.description",
              combineDegraded(
                governanceQuery.data?._meta?.degraded,
                healthQuery.data?._meta?.degraded,
              ),
            )}
            <SuperAdminPriorityOverview
              governance={governanceQuery.data}
              health={healthQuery.data?.services}
              loading={governanceQuery.isLoading}
              navigate={navigate}
            />
          </Space>
        ),
      },
      {
        key: "controls",
        label: t("superAdmin.tabs.controls.label"),
        children: (
          <Space
            direction="vertical"
            size="middle"
            className={styles.fullWidth}
          >
            {tabHeader(
              "controls",
              "superAdmin.tabs.controls.title",
              "superAdmin.tabs.controls.description",
              controlsQualityQuery.data?._meta?.degraded,
            )}
            <SuperAdminControlsQuality
              section={controlsQualityQuery.data}
              loading={controlsQualityQuery.isLoading}
              navigate={navigate}
            />
          </Space>
        ),
      },
      {
        key: "operations",
        label: t("superAdmin.tabs.operations.label"),
        children: (
          <Space
            direction="vertical"
            size="middle"
            className={styles.fullWidth}
          >
            {tabHeader(
              "operations",
              "superAdmin.tabs.operations.title",
              "superAdmin.tabs.operations.description",
              operationsQuery.data?._meta?.degraded,
            )}
            <SuperAdminOperations
              section={operationsQuery.data}
              loading={operationsQuery.isLoading}
            />
          </Space>
        ),
      },
      {
        key: "audit",
        label: t("superAdmin.tabs.audit.label"),
        children: (
          <Space
            direction="vertical"
            size="middle"
            className={styles.fullWidth}
          >
            {tabHeader(
              "audit",
              "superAdmin.tabs.audit.title",
              "superAdmin.tabs.audit.description",
              auditQuery.data?._meta?.degraded,
            )}
            <SuperAdminAudit
              section={auditQuery.data}
              loading={auditQuery.isLoading}
            />
          </Space>
        ),
      },
      {
        key: "config",
        label: t("superAdmin.tabs.config.label"),
        children: (
          <Space
            direction="vertical"
            size="middle"
            className={styles.fullWidth}
          >
            {tabHeader(
              "config",
              "superAdmin.tabs.config.title",
              "superAdmin.tabs.config.description",
              systemConfigQuery.data?._meta?.degraded,
            )}
            <SuperAdminSystemConfig
              section={systemConfigQuery.data}
              loading={systemConfigQuery.isLoading}
            />
          </Space>
        ),
      },
      {
        key: "reporting",
        label: t("superAdmin.tabs.reporting.label"),
        children: (
          <Space
            direction="vertical"
            size="middle"
            className={styles.fullWidth}
          >
            {tabHeader(
              "reporting",
              "superAdmin.tabs.reporting.title",
              "superAdmin.tabs.reporting.description",
              reportingQuery.data?._meta?.degraded,
            )}
            <SuperAdminReporting
              section={reportingQuery.data}
              loading={reportingQuery.isLoading}
            />
          </Space>
        ),
      },
    ];
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    t,
    governanceQuery.data,
    governanceQuery.isLoading,
    governanceQuery.isFetching,
    healthQuery.data,
    healthQuery.isLoading,
    healthQuery.isFetching,
    controlsQualityQuery.data,
    controlsQualityQuery.isLoading,
    controlsQualityQuery.isFetching,
    operationsQuery.data,
    operationsQuery.isLoading,
    operationsQuery.isFetching,
    auditQuery.data,
    auditQuery.isLoading,
    auditQuery.isFetching,
    systemConfigQuery.data,
    systemConfigQuery.isLoading,
    systemConfigQuery.isFetching,
    reportingQuery.data,
    reportingQuery.isLoading,
    reportingQuery.isFetching,
    navigate,
  ]);

  if (!canAccess) {
    return <Navigate to={APP_ROUTES.DASHBOARD} replace />;
  }

  return (
    <PageContainer className={styles.page}>
      <PageHeader
        title={t("superAdmin.pageTitle")}
        subtitle={t("superAdmin.pageSubtitle")}
      />

      {isError && (
        <Alert
          type="error"
          showIcon
          message={t("superAdmin.errors.loadFailedTitle")}
          description={t("superAdmin.errors.loadFailedDescription")}
          className={styles.alert}
        />
      )}

      <Tabs
        className={styles.superAdminTabs}
        activeKey={activeTab}
        items={tabItems}
        onChange={(key) =>
          navigate(`${APP_ROUTES.SUPER_ADMIN}#${key}`, { replace: true })
        }
        destroyOnHidden={false}
      />
    </PageContainer>
  );
});

SuperAdminPage.displayName = "SuperAdminPage";

export default SuperAdminPage;
