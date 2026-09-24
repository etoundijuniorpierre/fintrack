// Composant React : porte l'interface de super-administration reporting.

import { useMemo, useState } from "react";
import dayjs from "dayjs";
import {
  Alert,
  App,
  Button,
  Descriptions,
  Empty,
  Modal,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
} from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import {
  superAdminApi,
  type ReportingOverviewSection,
} from "../../../../../api/superAdmin";
import { SectionCard } from "../../../../../components/Layout";
import { formatDate } from "../../../../../utils/formatters/formatters";
import { QUERY_KEYS } from "../../../../../utils/constants";
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
import {
  compareTableDates,
  compareTableText,
  TABLE_SORT_DIRECTIONS,
} from "../../../../../utils/table/sorting/tableSorting";

const { Text, Title, Paragraph } = Typography;

// Au-dela de ce delai, un rapport encore en generation est considere bloque.
const STUCK_THRESHOLD_MIN = 10;

// Definit les proprietes attendues par le composant Reporting.
interface ReportingProps {
  section?: ReportingOverviewSection;
  loading: boolean;
}

// Rend le composant SuperAdminReporting pour l'interface super-administration reporting.
export const SuperAdminReporting = ({ section, loading }: ReportingProps) => {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const reporting = asRecord(section);
  const failedReports = asArray(reporting.failedReports);
  const pendingReports = asArray(reporting.pendingReports);
  const [now] = useState(() => dayjs());

  // Un rapport encore PENDING/GENERATING au-dela de ce seuil est anormal (bloque).
  const stuckReports = pendingReports.filter((r) => {
    // Prepare d pour le flux courant.
    const created = asString(r.createdAt, "");
    return created
      ? now.diff(dayjs(created), "minute") > STUCK_THRESHOLD_MIN
      : false;
  });
  const availableWithoutFileReports = asArray(
    reporting.availableWithoutFileReports,
  );
  const [errorPreview, setErrorPreview] = useState<{
    id: string;
    name: string;
    error: string;
  } | null>(null);
  const retryMutation = useMutation({
    mutationFn: (reportId: string) => superAdminApi.retryReport(reportId),
    onSuccess: () => {
      message.success(t("superAdmin.reporting.retryStarted"));
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.SUPER_ADMIN.REPORTING_OVERVIEW,
      });
      queryClient.invalidateQueries({ queryKey: ["reports", "generated"] });
    },
    onError: () => message.error(t("superAdmin.reporting.retryFailed")),
  });

  const reportColumns = useMemo<ColumnsType<RowMap>>(
    () => [
      {
        title: t("superAdmin.reporting.name"),
        dataIndex: "name",
        key: "name",
        defaultSortOrder: "ascend",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.name, right.name),
      },
      {
        title: t("superAdmin.reporting.type"),
        dataIndex: "type",
        key: "type",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.type, right.type),
        render: (value: unknown) => asString(value),
      },
      {
        title: t("superAdmin.reporting.status"),
        dataIndex: "status",
        key: "status",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) => compareTableText(left.status, right.status),
        render: (status: unknown) => (
          <Tag color={statusTagColor(status)}>{asString(status)}</Tag>
        ),
      },
      {
        title: t("superAdmin.reporting.generatedAt"),
        dataIndex: "createdAt",
        key: "createdAt",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableDates(left.createdAt, right.createdAt),
        render: (value: string) => (value ? formatDate(value) : "-"),
      },
      {
        title: t("superAdmin.reporting.period"),
        key: "period",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableDates(left.periodStart, right.periodStart),
        render: (_: unknown, record) => {
          const start = asString(record.periodStart, "");
          const end = asString(record.periodEnd, "");
          if (!start && !end) return "-";
          return `${start ? formatDate(start) : "-"} - ${end ? formatDate(end) : "-"}`;
        },
      },
      {
        title: t("superAdmin.reporting.emailRecipients"),
        dataIndex: "emailRecipients",
        key: "emailRecipients",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(left.emailRecipients, right.emailRecipients),
        render: (value: unknown) =>
          Array.isArray(value) && value.length > 0 ? value.join(", ") : "-",
      },
      {
        title: t("superAdmin.reporting.errorHeader"),
        dataIndex: "errorMessage",
        key: "errorMessage",
        sortDirections: TABLE_SORT_DIRECTIONS,
        sorter: (left, right) =>
          compareTableText(left.errorMessage, right.errorMessage),
        render: (errorMessage: unknown, record) => {
          const err = asString(errorMessage, "");
          if (!err) return <Text type="secondary">-</Text>;
          return (
            <Button
              size="small"
              type="link"
              onClick={() =>
                setErrorPreview({
                  id: asString(record.id),
                  name: asString(record.name),
                  error: err,
                })
              }
            >
              {t("superAdmin.reporting.viewError")}
            </Button>
          );
        },
      },
      {
        title: t("superAdmin.reporting.actions"),
        key: "actions",
        render: (_: unknown, record) => {
          const reportId = asString(record.id, "");
          const isFailed = asString(record.status).toUpperCase() === "FAILED";
          if (!isFailed || !reportId) return <Text type="secondary">-</Text>;
          return (
            <Button
              size="small"
              onClick={() => retryMutation.mutate(reportId)}
              loading={
                retryMutation.isPending && retryMutation.variables === reportId
              }
            >
              {t("superAdmin.reporting.retryButton")}
            </Button>
          );
        },
      },
    ],
    [t, setErrorPreview, retryMutation],
  );

  return (
    <SectionCard title={t("superAdmin.sections.reporting")} loading={loading}>
      <Space direction="vertical" size="middle" className={styles.fullWidth}>
        <Paragraph type="secondary">
          {t("superAdmin.reporting.intro")}
        </Paragraph>
        <Descriptions column={3} bordered size="small">
          <Descriptions.Item label={t("superAdmin.reporting.total")}>
            {asNumber(reporting.total)}
          </Descriptions.Item>
          <Descriptions.Item label={t("superAdmin.reporting.failed")}>
            <Tag color={asNumber(reporting.failed) > 0 ? "error" : "default"}>
              {asNumber(reporting.failed)}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label={t("superAdmin.reporting.pending")}>
            <Tooltip title={t("superAdmin.reporting.pendingHint")}>
              <Tag
                color={
                  asNumber(reporting.pending) > 0 ? "processing" : "default"
                }
              >
                {asNumber(reporting.pending)}
              </Tag>
            </Tooltip>
          </Descriptions.Item>
        </Descriptions>

        {availableWithoutFileReports.length > 0 && (
          <Alert
            type="warning"
            showIcon
            message={t("superAdmin.reporting.availableWithoutFileAlert", {
              count: availableWithoutFileReports.length,
            })}
            description={t(
              "superAdmin.reporting.availableWithoutFileDescription",
            )}
          />
        )}

        {stuckReports.length > 0 && (
          <>
            <Title level={5}>{t("superAdmin.reporting.stuckTableTitle")}</Title>
            <Alert
              type="warning"
              showIcon
              message={t("superAdmin.reporting.stuckAlert", {
                count: stuckReports.length,
                minutes: STUCK_THRESHOLD_MIN,
              })}
              description={t("superAdmin.reporting.stuckAlertDescription")}
            />
            <Table<RowMap>
              rowKey={(row) => asString(row.id)}
              columns={reportColumns}
              dataSource={stuckReports}
              pagination={false}
              size="small"
            />
          </>
        )}

        <Title level={5}>{t("superAdmin.reporting.failedTableTitle")}</Title>
        <Table<RowMap>
          rowKey={(row) => asString(row.id)}
          columns={reportColumns}
          dataSource={failedReports}
          pagination={getPaginationConfig({
            pageSize: 10,
            hideOnSinglePage: true,
          })}
          size="small"
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={t("superAdmin.reporting.noFailed")}
              />
            ),
          }}
        />

        <Title level={5}>
          {t("superAdmin.reporting.availableWithoutFileTableTitle")}
        </Title>
        <Table<RowMap>
          rowKey={(row) => asString(row.id)}
          columns={reportColumns}
          dataSource={availableWithoutFileReports}
          pagination={getPaginationConfig({
            pageSize: 10,
            hideOnSinglePage: true,
          })}
          size="small"
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={t("superAdmin.reporting.noAvailableWithoutFile")}
              />
            ),
          }}
        />

        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label={t("superAdmin.reporting.avgTime")}>
            {reporting.averageGenerationTimeSeconds == null
              ? "-"
              : `${asNumber(reporting.averageGenerationTimeSeconds).toFixed(1)} s`}
          </Descriptions.Item>
        </Descriptions>
      </Space>

      <Modal
        open={Boolean(errorPreview)}
        onCancel={() => setErrorPreview(null)}
        footer={null}
        title={t("superAdmin.reporting.errorModalTitle", {
          name: errorPreview?.name ?? "-",
        })}
        width={720}
      >
        {errorPreview && (
          <pre className={styles.stackTrace}>{errorPreview.error}</pre>
        )}
      </Modal>
    </SectionCard>
  );
};
