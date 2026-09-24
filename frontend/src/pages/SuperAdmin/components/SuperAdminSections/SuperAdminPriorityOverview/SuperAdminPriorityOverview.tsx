// Composant React : porte l'interface de super-administration priorite vue d ensemble.

import { useMemo } from "react";
import type { NavigateFunction } from "react-router-dom";
import {
  Col,
  Descriptions,
  Divider,
  List,
  Progress,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { useTranslation } from "react-i18next";
import type { TFunction } from "i18next";
import {
  CartesianGrid,
  Bar as RechartsBar,
  BarChart as RechartsBarChart,
  ResponsiveContainer as RechartsResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis as RechartsXAxis,
  YAxis as RechartsYAxis,
} from "recharts";
import type {
  GovernanceSection,
  ServiceHealthItem,
} from "../../../../../api/superAdmin";
import { SectionCard } from "../../../../../components/Layout";
import { APP_ROUTES } from "../../../../../utils/constants";
import { formatDate } from "../../../../../utils/formatters/formatters";
import {
  asArray,
  asEntries,
  asNumber,
  asPercent,
  asRecord,
  asString,
  isRowMap,
  statusTagColor,
} from "../../../../../utils/superAdmin/superAdminSectionUtils/superAdminSectionUtils";
import { getPaginationConfig } from "../../../../../utils/table/pagination/paginationConfig";
import styles from "../../../SuperAdminPage.module.scss";
import {
  compareTableDates,
  compareTableNumbers,
  compareTableText,
  TABLE_SORT_DIRECTIONS,
} from "../../../../../utils/table/sorting/tableSorting";

const { Text, Title } = Typography;

// Definit les proprietes attendues par le composant PriorityOverview.
interface PriorityOverviewProps {
  governance?: GovernanceSection;
  health?: ServiceHealthItem[];
  loading: boolean;
  navigate: NavigateFunction;
}

// Prepare l'affichage lisible de super-administration priorite vue d ensemble.
const formatGovernanceLabel = (kind: string, label: string, t: TFunction) => {
  if (kind === "status") return t(`incidents.status.${label}`, label);
  if (kind === "criticality") return t(`incidents.criticality.${label}`, label);
  if (kind === "agency" && label === "__UNASSIGNED_AGENCY__")
    return t("superAdmin.governance.unassignedAgency");
  if (kind === "service" && label === "__UNASSIGNED_SERVICE__")
    return t("superAdmin.governance.unassignedService");
  if (label === "UNKNOWN") return t("superAdmin.governance.unknown");
  return label;
};

// Rend le composant SuperAdminPriorityOverview pour l'interface super-administration priorite vue d ensemble.
export const SuperAdminPriorityOverview = ({
  governance,
  health,
  loading,
  navigate,
}: PriorityOverviewProps) => {
  const { t } = useTranslation();
  const governanceData = asRecord(governance);
  const healthItems = health ?? [];

  const totalServices = healthItems.length;
  const downServices = healthItems.filter(
    (service) => service.status !== "UP",
  ).length;
  const serviceAvailability = asPercent(
    totalServices - downServices,
    totalServices,
  );
  const visibleAnomalies = Array.isArray(governanceData.visibleAnomalies)
    ? (governanceData.visibleAnomalies as unknown[]).filter(isRowMap)
    : [];

  const healthColumns = useMemo<ColumnsType<ServiceHealthItem>>(
    () => [
      {
        title: t("superAdmin.health.service"),
        dataIndex: "label",
        key: "label",
        defaultSortOrder: "ascend",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.label, right.label),
      },
      {
        title: t("superAdmin.health.status"),
        dataIndex: "status",
        key: "status",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.status, right.status),
        render: (status: ServiceHealthItem["status"]) => (
          <Tag color={statusTagColor(status)}>{status}</Tag>
        ),
      },
      {
        title: t("superAdmin.health.responseTime"),
        dataIndex: "responseTimeMs",
        key: "responseTimeMs",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableNumbers(left.responseTimeMs, right.responseTimeMs),
        render: (value: number | undefined) =>
          value == null ? "-" : `${value} ms`,
      },
      {
        title: t("superAdmin.health.lastCheck"),
        dataIndex: "lastCheckedAt",
        key: "lastCheckedAt",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableDates(left.lastCheckedAt, right.lastCheckedAt),
        render: (value: string | undefined) =>
          value ? formatDate(value) : "-",
      },
    ],
    [t],
  );

  const kpis = asRecord(governanceData.superAdminKpis);

  const renderKpi = (
    title: string,
    value: number,
    opts?: { tone?: "danger" | "warn"; target?: string },
  ) => {
    const color =
      value > 0
        ? opts?.tone === "danger"
          ? "#cf1322"
          : opts?.tone === "warn"
            ? "#fa8c16"
            : undefined
        : undefined;
    const onClick = opts?.target
      ? () => navigate(opts.target as string)
      : undefined;
    return (
      <Col flex="1" style={{ minWidth: 200 }}>
        <div
          role={onClick ? "button" : undefined}
          tabIndex={onClick ? 0 : undefined}
          onClick={onClick}
          onKeyDown={
            onClick
              ? (e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    onClick();
                  }
                }
              : undefined
          }
          style={{ cursor: onClick ? "pointer" : "default", height: "100%" }}
        >
          <SectionCard loading={loading}>
            <Statistic title={title} value={value} valueStyle={{ color }} />
            {onClick && value > 0 && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                {t("common.viewDetails")} →
              </Text>
            )}
          </SectionCard>
        </div>
      </Col>
    );
  };

  return (
    <Space orientation="vertical" size="large" className={styles.fullWidth}>
      <Row
        gutter={[16, 16]}
        className={styles.kpiRow}
        style={{ display: "flex", flexWrap: "wrap" }}
      >
        {renderKpi(t("superAdmin.kpis.servicesDown"), downServices, {
          tone: "danger",
        })}
        {renderKpi(
          t("superAdmin.kpis.criticalAnomalies"),
          asNumber(kpis.criticalAnomalies),
          { tone: "warn", target: `${APP_ROUTES.SUPER_ADMIN}#controls` },
        )}
        {renderKpi(
          t("superAdmin.kpis.failedEmails24h"),
          asNumber(kpis.failedEmails),
          { tone: "danger", target: `${APP_ROUTES.SUPER_ADMIN}#operations` },
        )}
        {renderKpi(
          t("superAdmin.kpis.lockedAccounts"),
          asNumber(kpis.lockedAccounts),
          { tone: "danger", target: `${APP_ROUTES.USERS}?status=LOCKED` },
        )}
        {renderKpi(
          t("superAdmin.kpis.unassignedIncidents"),
          asNumber(kpis.unassignedIncidents),
          {
            tone: "warn",
            target: `${APP_ROUTES.INCIDENTS}?assignedTo=UNASSIGNED`,
          },
        )}
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24}>
          <SectionCard
            title={t("superAdmin.sections.governance")}
            loading={loading}
          >
            <Row gutter={[16, 16]}>
              <Col xs={24} md={24} className={styles.centeredGraph}>
                <Space orientation="vertical" align="center">
                  <Progress type="dashboard" percent={serviceAvailability} />
                  <Text strong>{t("superAdmin.health.availability")}</Text>
                </Space>
              </Col>
            </Row>
            <Divider />
            <Descriptions column={{ xs: 1, md: 3 }} size="small" bordered>
              <Descriptions.Item label={t("superAdmin.governance.agencies")}>
                {asNumber(governanceData.agencies)}
              </Descriptions.Item>
              <Descriptions.Item label={t("superAdmin.governance.services")}>
                {asNumber(governanceData.services)}
              </Descriptions.Item>
              <Descriptions.Item label={t("superAdmin.governance.activeUsers")}>
                {asNumber(governanceData.activeUsers)}
              </Descriptions.Item>
              <Descriptions.Item
                label={`${t("superAdmin.governance.dataIssues")} (Règles échouées / Total)`}
              >
                {visibleAnomalies.length} / 11
              </Descriptions.Item>
            </Descriptions>
            <Divider />
            <Row gutter={[16, 16]}>
              {(
                [
                  [
                    "status",
                    t("superAdmin.governance.byStatus"),
                    governanceData.byStatus,
                  ],
                  [
                    "criticality",
                    t("superAdmin.governance.byCriticality"),
                    governanceData.byCriticality,
                  ],
                ] as const
              ).map(([kind, title, values]) => (
                <Col xs={24} md={12} key={asString(title)}>
                  <Title level={5}>{asString(title)}</Title>
                  <List
                    size="small"
                    dataSource={asEntries(values)}
                    renderItem={([label, count]) => (
                      <List.Item>
                        <Text>
                          {formatGovernanceLabel(kind, String(label), t)}
                        </Text>
                        <Tag>{asNumber(count)}</Tag>
                      </List.Item>
                    )}
                  />
                </Col>
              ))}
            </Row>
          </SectionCard>
        </Col>

        <Col xs={24}>
          <SectionCard
            title={t("superAdmin.sections.health")}
            loading={loading}
          >
            <Table<ServiceHealthItem>
              rowKey="key"
              columns={healthColumns}
              dataSource={healthItems}
              pagination={getPaginationConfig({
                pageSize: 10,
                hideOnSinglePage: true,
              })}
              size="small"
            />
          </SectionCard>
        </Col>

        <Col xs={24}>
          <SectionCard
            title={t("superAdmin.sections.topology")}
            loading={loading}
          >
            <Row gutter={[16, 16]}>
              <Col xs={24} md={8}>
                <Title level={5}>{t("superAdmin.governance.ratios")}</Title>
                <div style={{ width: "100%", height: 200 }}>
                  <RechartsResponsiveContainer width="100%" height="100%">
                    <RechartsBarChart
                      layout="vertical"
                      data={[
                        {
                          name: t("superAdmin.governance.rejectedRate"),
                          value: asNumber(governanceData.rejectedRate),
                        },
                        {
                          name: t("superAdmin.governance.reopenedRate"),
                          value: asNumber(governanceData.reopenedRate),
                        },
                        {
                          name: t("superAdmin.governance.transferredRate"),
                          value: asNumber(governanceData.transferredRate),
                        },
                      ]}
                      margin={{ top: 8, right: 24, bottom: 8, left: 8 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                      <RechartsXAxis
                        type="number"
                        unit="%"
                        tick={{ fontSize: 10 }}
                      />
                      <RechartsYAxis
                        type="category"
                        dataKey="name"
                        width={90}
                        tick={{ fontSize: 11 }}
                      />
                      <RechartsTooltip />
                      <RechartsBar
                        dataKey="value"
                        fill="#caa22e"
                        radius={[0, 6, 6, 0]}
                      />
                    </RechartsBarChart>
                  </RechartsResponsiveContainer>
                </div>
              </Col>
              <Col xs={24} md={8}>
                <Title level={5}>{t("superAdmin.topology.topServices")}</Title>
                <List
                  size="small"
                  dataSource={asArray(governanceData.topServices)}
                  renderItem={(item) => (
                    <List.Item>
                      <Text>
                        {asString(item.serviceName, asString(item.name))}
                      </Text>
                      <Tag>{asNumber(item.count)}</Tag>
                    </List.Item>
                  )}
                />
              </Col>
              <Col xs={24} md={8}>
                <Title level={5}>{t("superAdmin.topology.topAgencies")}</Title>
                <List
                  size="small"
                  dataSource={asArray(governanceData.topAgencies)}
                  renderItem={(item) => (
                    <List.Item>
                      <Text>
                        {asString(item.name, asString(item.agencyName))}
                      </Text>
                      <Tag>{asNumber(item.count)}</Tag>
                    </List.Item>
                  )}
                />
              </Col>
            </Row>
          </SectionCard>
        </Col>
      </Row>
    </Space>
  );
};
