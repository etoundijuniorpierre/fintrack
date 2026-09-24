// Composant React : porte l'interface de super-administration audit.

import { useMemo, useState } from "react";
import {
  Alert,
  App,
  Button,
  DatePicker,
  Empty,
  Form,
  InputNumber,
  List,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { superAdminApi } from "../../../../../api/superAdmin";
import type { AuditOverviewSection } from "../../../../../api/superAdmin";
import { SectionCard } from "../../../../../components/Layout";
import { APP_ROUTES } from "../../../../../utils/constants";
import { formatDate } from "../../../../../utils/formatters/formatters";
import { formatToApiDateTime } from "../../../../../utils/date/dateUtils";
import {
  asArray,
  asNumber,
  asRecord,
  asString,
  statusTagColor,
  type RowMap,
} from "../../../../../utils/superAdmin/superAdminSectionUtils/superAdminSectionUtils";
import { getPaginationConfig } from "../../../../../utils/table/pagination/paginationConfig";
import styles from "../../../SuperAdminPage.module.scss";
import { downloadFile } from "../../../../../utils/download/downloadFile";
import { ALL_AUDIT_ACTIONS } from "../../../../../api/audit/types";
import {
  compareTableDates,
  compareTableText,
  TABLE_SORT_DIRECTIONS,
} from "../../../../../utils/table/sorting/tableSorting";

const { Title, Paragraph, Text } = Typography;

// Definit les proprietes attendues par le composant Audit.
interface AuditProps {
  section?: AuditOverviewSection;
  loading: boolean;
}

// Rend le composant SuperAdminAudit pour l'interface super-administration audit.
export const SuperAdminAudit = ({ section, loading }: AuditProps) => {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const navigate = useNavigate();
  const audit = asRecord(section);
  // Insights securite (ce que la page Audit generique n'offre pas).
  const recentSensitive = asArray(audit.recentSensitive);
  const permissionChangeHistory = asArray(audit.permissionChangeHistory);
  const repeatedActors = asArray(audit.repeatedSensitiveActions);
  const [filters, setFilters] = useState<{
    action?: string;
    status?: string;
    from?: string;
    to?: string;
    limit?: number;
  }>({});
  const [exporting, setExporting] = useState(false);

  const actionOptions = useMemo(
    () => ALL_AUDIT_ACTIONS.map((value) => ({ value, label: value })),
    [],
  );

  const journalColumns = useMemo<ColumnsType<RowMap>>(
    () => [
      {
        title: t("superAdmin.audit.time"),
        key: "time",
        defaultSortOrder: "descend",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableDates(
            left.timestamp ?? left.createdAt,
            right.timestamp ?? right.createdAt,
          ),
        render: (_: unknown, row) => {
          const date = asString(row.timestamp, asString(row.createdAt, ""));
          return date ? formatDate(date) : "-";
        },
      },
      {
        title: t("superAdmin.audit.actor"),
        key: "actor",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(
            left.username ?? left.actor,
            right.username ?? right.actor,
          ),
        render: (_: unknown, row) => {
          const username = asString(row.username, asString(row.actor, "-"));
          const nestedUser = asRecord(row.user);
          const userId = asString(row.userId, asString(nestedUser.id));
          return (
            <Space direction="vertical" size={0}>
              <span>{username}</span>
              {userId && <Text type="secondary">{userId}</Text>}
            </Space>
          );
        },
      },
      {
        title: t("superAdmin.audit.action"),
        key: "action",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.action, right.action),
        render: (_: unknown, row) => {
          const action = asString(row.action);
          return (
            <Tag color="purple">{t(`audit.action.${action}`, action)}</Tag>
          );
        },
      },
      {
        title: t("superAdmin.audit.resourceId"),
        key: "resourceId",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(left.resourceId, right.resourceId),
        render: (_: unknown, row) => asString(row.resourceId, "-"),
      },
      {
        title: t("superAdmin.audit.ipAddress"),
        key: "ipAddress",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(left.ipAddress, right.ipAddress),
        render: (_: unknown, row) => asString(row.ipAddress, "-"),
      },
      {
        title: t("superAdmin.audit.status"),
        key: "status",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.status, right.status),
        render: (_: unknown, row) => {
          const status = asString(row.status);
          return (
            <Tag color={statusTagColor(row.status)}>
              {t(`audit.status.${status}`, status)}
            </Tag>
          );
        },
      },
    ],
    [t],
  );

  const onExport = async () => {
    setExporting(true);
    try {
      const blob = await superAdminApi.exportAuditCsv(filters);
      downloadFile(blob, `audit-export-${new Date().toISOString()}.csv`);
      message.success(t("superAdmin.audit.exportSuccess"));
    } catch {
      message.error(t("superAdmin.audit.exportError"));
    } finally {
      setExporting(false);
    }
  };

  return (
    <SectionCard title={t("superAdmin.sections.audit")} loading={loading}>
      <Paragraph type="secondary">{t("superAdmin.audit.intro")}</Paragraph>

      <Space align="center" size="large" style={{ marginBottom: 12 }}>
        <Statistic
          title={t("superAdmin.audit.sensitiveCount")}
          value={asNumber(audit.sensitiveCount)}
        />
        <Button type="link" onClick={() => navigate(APP_ROUTES.AUDIT)}>
          {t("superAdmin.audit.viewFullJournal")} →
        </Button>
      </Space>

      <Title level={5}>{t("superAdmin.audit.sensitiveTitle")}</Title>
      <Paragraph type="secondary" className={styles.tabDescription}>
        {t("superAdmin.audit.sensitiveIntro")}
      </Paragraph>
      <Table<RowMap>
        rowKey={(row) => asString(row.id, asString(row.timestamp))}
        columns={journalColumns}
        dataSource={recentSensitive}
        size="small"
        scroll={{ x: true }}
        pagination={getPaginationConfig({
          pageSize: 10,
          hideOnSinglePage: true,
        })}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={t("superAdmin.audit.noSensitive")}
            />
          ),
        }}
      />

      <Title level={5} style={{ marginTop: 16 }}>
        {t("superAdmin.audit.permissionChangesTitle")}
      </Title>
      <Paragraph type="secondary" className={styles.tabDescription}>
        {t("superAdmin.audit.permissionChangesIntro")}
      </Paragraph>
      <Table<RowMap>
        rowKey={(row) => asString(row.id, asString(row.timestamp))}
        columns={journalColumns}
        dataSource={permissionChangeHistory}
        size="small"
        scroll={{ x: true }}
        pagination={getPaginationConfig({
          pageSize: 5,
          hideOnSinglePage: true,
        })}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={t("superAdmin.audit.noPermissionChanges")}
            />
          ),
        }}
      />

      {repeatedActors.length > 0 && (
        <>
          <Title level={5} style={{ marginTop: 16 }}>
            {t("superAdmin.audit.repeatedActorsTitle")}
          </Title>
          <List
            size="small"
            dataSource={repeatedActors}
            renderItem={(item) => (
              <List.Item>
                <Space>
                  <Tag>{asString(item.username)}</Tag>
                  <Tag color="purple">{asString(item.action)}</Tag>
                  <span>× {asNumber(item.count)}</span>
                </Space>
              </List.Item>
            )}
          />
        </>
      )}

      <Title level={5} style={{ marginTop: 16 }}>
        {t("superAdmin.audit.exportSectionTitle")}
      </Title>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message={t("superAdmin.audit.exportFiltersInfoTitle")}
        description={t("superAdmin.audit.exportFiltersInfoDesc")}
      />
      <Form layout="inline" className={styles.auditFilters}>
        <Form.Item
          label={t("superAdmin.audit.filterAction")}
          tooltip={t("superAdmin.audit.filterActionHint")}
        >
          <Select
            allowClear
            showSearch
            style={{ minWidth: 200 }}
            placeholder="USER_CREATE"
            options={actionOptions}
            onChange={(value) => setFilters({ ...filters, action: value })}
          />
        </Form.Item>
        <Form.Item
          label={t("superAdmin.audit.filterStatus")}
          tooltip={t("superAdmin.audit.filterStatusHint")}
        >
          <Select
            allowClear
            style={{ minWidth: 140 }}
            placeholder="SUCCESS"
            onChange={(value) => setFilters({ ...filters, status: value })}
            options={[
              { value: "SUCCESS", label: "SUCCESS" },
              { value: "FAILURE", label: "FAILURE" },
            ]}
          />
        </Form.Item>
        <Form.Item
          label={t("superAdmin.audit.filterPeriod")}
          tooltip={t("superAdmin.audit.filterPeriodHint")}
        >
          <DatePicker.RangePicker
            onChange={(range) =>
              setFilters({
                ...filters,
                from: formatToApiDateTime(range?.[0], "startOfDay"),
                to: formatToApiDateTime(range?.[1], "endOfDay"),
              })
            }
          />
        </Form.Item>
        <Form.Item
          label={t("superAdmin.audit.filterLimit")}
          tooltip={t("superAdmin.audit.filterLimitHint")}
        >
          <InputNumber
            min={100}
            max={100000}
            precision={0}
            placeholder="5000"
            onChange={(value) =>
              setFilters({
                ...filters,
                limit: typeof value === "number" ? value : undefined,
              })
            }
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" loading={exporting} onClick={onExport}>
            {t("superAdmin.audit.exportButton")}
          </Button>
        </Form.Item>
      </Form>
    </SectionCard>
  );
};
