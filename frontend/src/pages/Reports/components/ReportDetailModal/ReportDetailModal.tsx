// Composant React : porte l'interface de rapport detail modale.

import { memo, useCallback, useMemo } from "react";
import { Descriptions, Divider, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useTranslation } from "react-i18next";
import Modal from "../../../../components/ui/Modal/Modal";
import {
  formatDate,
  formatHours,
} from "../../../../utils/formatters/formatters";
import type {
  ReportIncidentSummary,
  ReportNamedSummary,
  ReportPersonSummary,
  ReportResponse,
  ReportMetrics,
} from "../../../../api/reporting/types";
import { REPORT_STATUS } from "../../../../api/reporting/types";
import styles from "./ReportDetailModal.module.scss";
import {
  compareTableDates,
  compareTableText,
  TABLE_SORT_DIRECTIONS,
} from "../../../../utils/table/sorting/tableSorting";

// Definit les proprietes attendues par le composant ReportDetailModal.
interface ReportDetailModalProps {
  open: boolean;
  record: ReportResponse | null;
  onClose: () => void;
}

// Normalise les valeurs de rapport detail modale avant utilisation.
const normalize = (value?: string) => value?.toUpperCase() ?? "";
const { Text } = Typography;

// Determine statut color e partir du contexte fourni.
const getStatusColor = (
  status?: string,
): "success" | "error" | "processing" | "default" => {
  const normalized = normalize(status);
  switch (normalized) {
    case REPORT_STATUS.AVAILABLE:
      return "success";
    case REPORT_STATUS.FAILED:
      return "error";
    case REPORT_STATUS.PENDING:
    case REPORT_STATUS.GENERATING:
      return "processing";
    default:
      return "default";
  }
};

// Determine person nom e partir du contexte fourni.
const getPersonName = (person?: ReportPersonSummary | string) => {
  if (!person) return "-";
  if (typeof person === "string") return person;
  const fullName = [person.firstName, person.lastName]
    .filter(Boolean)
    .join(" ")
    .trim();
  return fullName || person.username || person.email || person.id || "-";
};

// Indicateurs exprimes en heures : affiches avec une decimale et l'unite.
const HOUR_METRIC_KEYS = new Set([
  "avgClosureHours",
  "medianClosureHours",
  "p90ClosureHours",
  "avgResolutionHours",
  "medianResolutionHours",
  "p90ResolutionHours",
]);

// Determine named value e partir du contexte fourni.
const getNamedValue = (value?: ReportNamedSummary | string) => {
  if (!value) return "-";
  if (typeof value === "string") return value;
  return value.displayName || value.name || value.id || "-";
};

// Relit un rapport anterieur a la separation cloture/resolution sous les cles actuelles.
const normalizeMilestoneKeys = (
  metrics?: ReportMetrics,
): ReportMetrics | undefined => {
  if (
    !metrics ||
    metrics.avgClosureHours !== undefined ||
    metrics.avgResolutionHours === undefined
  ) {
    return metrics;
  }
  const {
    avgResolutionHours,
    medianResolutionHours,
    p90ResolutionHours,
    monthlyAvgResolutionHours,
    ...rest
  } = metrics as ReportMetrics & {
    monthlyAvgResolutionHours?: ReportMetrics["monthlyAvgClosureHours"];
  };
  return {
    ...rest,
    avgClosureHours: avgResolutionHours,
    medianClosureHours: medianResolutionHours,
    p90ClosureHours: p90ResolutionHours,
    monthlyAvgClosureHours: monthlyAvgResolutionHours,
  };
};

// Determine indicateur value e partir du contexte fourni.
const getMetricValue = (
  metrics: ReportMetrics | undefined,
  key: string,
): unknown => {
  if (!metrics) return undefined;
  switch (key) {
    case "totalIncidents":
      return metrics.totalIncidents;
    case "activeIncidents":
      return metrics.activeIncidents;
    case "closedIncidents":
      return metrics.closedIncidents;
    case "rejectedIncidents":
      return metrics.rejectedIncidents;
    case "avgClosureHours":
      return metrics.avgClosureHours;
    case "medianClosureHours":
      return metrics.medianClosureHours;
    case "p90ClosureHours":
      return metrics.p90ClosureHours;
    case "avgResolutionHours":
      return metrics.avgResolutionHours;
    case "medianResolutionHours":
      return metrics.medianResolutionHours;
    case "p90ResolutionHours":
      return metrics.p90ResolutionHours;
    case "assignedToMe":
      return metrics.assignedToMe;
    case "transferredByMe":
      return metrics.transferredByMe;
    case "closedByMe":
      return metrics.closedByMe;
    default:
      return undefined;
  }
};

// Rend le composant ReportDetailModal pour l'interface rapport detail modale.
const ReportDetailModal = memo(
  ({ open, record, onClose }: ReportDetailModalProps) => {
    const { t } = useTranslation();

    // Traite la fermeture.
    const handleClose = useCallback(() => onClose(), [onClose]);
    const summaryMetrics = useMemo(() => {
      const metrics = normalizeMilestoneKeys(record?.metrics);
      if (!metrics) {
        return [];
      }
      const preferred = [
        "totalIncidents",
        "activeIncidents",
        "closedIncidents",
        "rejectedIncidents",
        "avgClosureHours",
        "medianClosureHours",
        "p90ClosureHours",
        "avgResolutionHours",
        "medianResolutionHours",
        "p90ResolutionHours",
        "assignedToMe",
        "transferredByMe",
        "closedByMe",
      ];
      return preferred
        .filter((key) => getMetricValue(metrics, key) !== undefined)
        .map((key) => {
          const val = getMetricValue(metrics, key);
          return {
            key,
            value:
              HOUR_METRIC_KEYS.has(key) && typeof val === "number"
                ? formatHours(val)
                : String(val ?? "-"),
          };
        });
    }, [record?.metrics]);

    const incidentsCount = Array.isArray(record?.metrics?.incidentsList)
      ? record.metrics.incidentsList.length
      : 0;

    const incidents = useMemo(
      () => record?.metrics?.incidentsList ?? [],
      [record?.metrics?.incidentsList],
    );

    const incidentColumns = useMemo<ColumnsType<ReportIncidentSummary>>(
      () => [
        {
          title: t("reports.incidents.columns.incident"),
          key: "incident",
          width: 220,
          defaultSortOrder: "ascend",
          sortDirections: TABLE_SORT_DIRECTIONS,
          sorter: (left, right) =>
            compareTableText(left.title, right.title),
          render: (_, incident) => (
            <Space direction="vertical" size={0}>
              <Text strong>{incident.title || "-"}</Text>
              {incident.id && <Text type="secondary">{incident.id}</Text>}
            </Space>
          ),
        },
        {
          title: t("reports.incidents.columns.creator"),
          key: "creator",
          width: 150,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sorter: (left, right) =>
            compareTableText(
              getPersonName(left.createdBy),
              getPersonName(right.createdBy),
            ),
          render: (_, incident) => getPersonName(incident.createdBy),
        },
        {
          title: t("reports.incidents.columns.assignment"),
          key: "assignment",
          width: 190,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sorter: (left, right) =>
            compareTableText(
              getPersonName(left.assignedTo),
              getPersonName(right.assignedTo),
            ),
          render: (_, incident) => (
            <Space direction="vertical" size={0}>
              <Text>{getPersonName(incident.assignedTo)}</Text>
              <Text type="secondary">
                {getNamedValue(incident.transferredToService) !== "-"
                  ? getNamedValue(incident.transferredToService)
                  : incident.serviceName || "-"}
              </Text>
            </Space>
          ),
        },
        {
          title: t("reports.incidents.columns.status"),
          key: "status",
          width: 150,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sorter: (left, right) =>
            compareTableText(left.status, right.status),
          render: (_, incident) => (
            <Space direction="vertical" size={4}>
              <Tag>{incident.status || "-"}</Tag>
              <Tag color="volcano">{incident.criticality || "-"}</Tag>
            </Space>
          ),
        },
        {
          title: t("reports.incidents.columns.dates"),
          key: "dates",
          width: 190,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sorter: (left, right) =>
            compareTableDates(
              left.incidentDate ?? left.createdAt,
              right.incidentDate ?? right.createdAt,
            ),
          render: (_, incident) => (
            <Space direction="vertical" size={0}>
              <Text>
                {incident.incidentDate
                  ? formatDate(incident.incidentDate)
                  : "-"}
              </Text>
              {incident.resolvedAt && (
                <Text type="secondary">{formatDate(incident.resolvedAt)}</Text>
              )}
              {incident.closedAt && (
                <Text type="secondary">{formatDate(incident.closedAt)}</Text>
              )}
            </Space>
          ),
        },
      ],
      [t],
    );

    return (
      <Modal
        open={open}
        title={t("reports.detail.title")}
        onClose={handleClose}
        cancelText={t("common.close")}
        footer={true}
        width={920}
        destroyOnHidden
      >
        {record && (
          <>
            <Divider titlePlacement="left">
              {t("reports.detail.generalInfo")}
            </Divider>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              <Descriptions.Item label={t("reports.table.name")} span={2}>
                {record.name}
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.table.type")}>
                {t(`reports.reportType.${record.type}`)}
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.table.contentType")}>
                {t(
                  `reports.contentType.${record.contentType || "OPERATIONAL"}`,
                )}
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.table.generationType")}>
                {record.generationType ? (
                  <Tag
                    color={
                      record.generationType === "MANUAL" ? "geekblue" : "purple"
                    }
                  >
                    {t(`reports.generationType.${record.generationType}`)}
                  </Tag>
                ) : (
                  "-"
                )}
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.table.period")} span={2}>
                {record.period}
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.table.format")}>
                <Tag color="blue">
                  {t(`reports.reportFormat.${record.format}`)}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.table.status")}>
                <Tag color={getStatusColor(record.status)}>
                  {t(`reports.status.${normalize(record.status)}`)}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.table.generatedAt")}>
                {formatDate(record.generatedAt)}
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.detail.createdBy")}>
                {record.createdByLabel || record.createdBy || "-"}
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.detail.scope")}>
                {record.scope ? t(`reports.scope.${record.scope}`) : "-"}
              </Descriptions.Item>
              <Descriptions.Item label={t("reports.detail.incidentsCount")}>
                {incidentsCount}
              </Descriptions.Item>
              {record.recipients && record.recipients.length > 0 && (
                <Descriptions.Item
                  label={t("reports.table.recipients")}
                  span={2}
                >
                  {record.recipients.join(", ")}
                </Descriptions.Item>
              )}
              {record.fileSize !== undefined && record.fileSize !== null && (
                <Descriptions.Item label={t("reports.detail.fileSize")}>
                  {(record.fileSize / 1024).toFixed(2)} KB
                </Descriptions.Item>
              )}
            </Descriptions>
          </>
        )}
        {record && summaryMetrics.length > 0 && (
          <>
            <Divider titlePlacement="left">
              {t("reports.detail.metrics")}
            </Divider>
            <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
              {summaryMetrics.map((metric) => (
                <Descriptions.Item
                  key={metric.key}
                  label={t(`reports.metrics.${metric.key}`)}
                >
                  <Text strong>{metric.value}</Text>
                </Descriptions.Item>
              ))}
            </Descriptions>
          </>
        )}
        {record && incidents.length > 0 && (
          <>
            <Divider titlePlacement="left">
              {t("reports.detail.incidents")}
            </Divider>
            <Table<ReportIncidentSummary>
              className={styles.incidentTable}
              rowKey={(incident) =>
                incident.id ||
                `${incident.title ?? "incident"}-${incident.createdAt ?? ""}`
              }
              columns={incidentColumns}
              dataSource={incidents}
              pagination={false}
              size="small"
              scroll={{ x: 1100 }}
              expandable={{
                expandedRowRender: (incident) => (
                  <div className={styles.incidentExpanded}>
                    <div>
                      <Text type="secondary">
                        {t("reports.incidents.fields.type")}
                      </Text>
                      <Text>{getNamedValue(incident.type)}</Text>
                    </div>
                    <div>
                      <Text type="secondary">
                        {t("reports.incidents.fields.agency")}
                      </Text>
                      <Text>{getNamedValue(incident.agency)}</Text>
                    </div>
                    <div>
                      <Text type="secondary">
                        {t("reports.incidents.fields.cause")}
                      </Text>
                      <Text>
                        {incident.causeDetail || incident.cause || "-"}
                      </Text>
                    </div>
                    <div className={styles.incidentExpandedWide}>
                      <Text type="secondary">
                        {t("reports.incidents.fields.description")}
                      </Text>
                      <Text>{incident.description || "-"}</Text>
                    </div>
                  </div>
                ),
              }}
            />
          </>
        )}
      </Modal>
    );
  },
);

ReportDetailModal.displayName = "ReportDetailModal";

export default ReportDetailModal;
