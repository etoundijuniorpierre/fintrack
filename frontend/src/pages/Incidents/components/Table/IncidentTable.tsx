// Composant React : porte l'interface de incident tableau.

import { Tag, Typography } from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import { useTranslation } from "react-i18next";
import { useMemo, memo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import type { IncidentSummaryResponse } from "../../../../api/incident/types";
import {
  STATUS_COLORS,
  CRITICALITY_COLORS,
} from "../../../../api/incident/types";
import {
  formatDateTime,
  formatUserName,
} from "../../../../utils/formatters/formatters";
import {
  incidentNavigation,
  incidentPathIdentifier,
} from "../../../../utils/navigation/incidents/incidents";
import { paginationConfig } from "../../../../utils/table/pagination/paginationConfig";
import { useAuth } from "../../../../hooks/auth/useAuth";
import {
  DataGrid,
  EmptyState,
  TableEllipsisText,
} from "../../../../components/ui";
import { commonColumnProps } from "../../../../utils/table/tableColumns/tableColumns";
import {
  getIncidentAgeInfo,
  getIncidentSlaInfo,
  type IncidentSlaState,
} from "../../../../utils/incidents/incidentTiming";
import {
  formatIncidentAge,
  formatIncidentSla,
} from "../../../../utils/incidents/incidentPresentation";
import styles from "./IncidentTable.module.scss";
import {
  readTableSort,
  TABLE_SORT_DIRECTIONS,
  toTableSortOrder,
} from "../../../../utils/table/sorting/tableSorting";

// Definit les proprietes attendues par le composant IncidentTable.
interface IncidentTableProps {
  incidents: IncidentSummaryResponse[];
  loading: boolean;
  pagination?: TablePaginationConfig;
  onPageChange?: (page: number, pageSize: number) => void;
  sort?: { field?: string; order?: "asc" | "desc" };
  onSortChange?: (field?: string, order?: "asc" | "desc") => void;
}

const SLA_TAG_COLORS = new Map<IncidentSlaState, string>([
  ["none", "default"],
  ["closed", "green"],
  ["stopped", "default"],
  ["overdue", "red"],
  ["dueToday", "orange"],
  ["dueSoon", "gold"],
  ["onTrack", "blue"],
]);

const criticalityColorMap = new Map<string, string>(
  Object.entries(CRITICALITY_COLORS),
);
const statusColorMap = new Map<string, string>(Object.entries(STATUS_COLORS));

// Rend le composant IncidentTable.
const IncidentTable = memo(
  ({
    incidents,
    loading,
    pagination,
    onPageChange,
    sort,
    onSortChange,
  }: IncidentTableProps) => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { user } = useAuth();

    // Traite la selection de ligne.
    const handleRowClick = useCallback(
      (record: IncidentSummaryResponse) => {
        incidentNavigation.navigateToIncidentDetail(
          navigate,
          incidentPathIdentifier(record),
        );
      },
      [navigate],
    );

    const columns = useMemo<ColumnsType<IncidentSummaryResponse>>(
      () => [
        {
          ...commonColumnProps,
          title: t("incidents.table.reference"),
          dataIndex: "reference",
          key: "reference",
          fixed: "left",
          width: 160,
          sorter: true,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sortOrder:
            sort?.field === "reference"
              ? toTableSortOrder(sort.order)
              : undefined,
          render: (reference: string | undefined) => (
            <Typography.Text code>{reference ?? "-"}</Typography.Text>
          ),
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.title"),
          dataIndex: "title",
          key: "title",
          sorter: true,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sortOrder:
            sort?.field === "title"
              ? toTableSortOrder(sort.order)
              : undefined,
          render: (text: string, record: IncidentSummaryResponse) => {
            const isMyResponsibility =
              user?.id === record.assignedTo?.id ||
              user?.agencyId === record.agency?.id;

            return (
              <span className={styles.incidentTitleContent}>
                {["OPEN", "PENDING_VALIDATION"].includes(record.status) && (
                  <span
                    className={styles.newIncidentDot}
                    title={t("incidents.status.OPEN")}
                  />
                )}
                <TableEllipsisText
                  value={text}
                  maxWidth={300}
                  strong={isMyResponsibility}
                />
                {isMyResponsibility && (
                  <Tag className={styles.myResponsibilityTag}>
                    {t("incidents.tags.myResponsibility")}
                  </Tag>
                )}
              </span>
            );
          },
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.type"),
          key: "type",
          render: (_: unknown, record: IncidentSummaryResponse) =>
            record.type?.displayName ?? "-",
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.criticality"),
          dataIndex: "criticality",
          key: "criticality",
          sorter: true,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sortOrder:
            sort?.field === "criticality"
              ? toTableSortOrder(sort.order)
              : undefined,
          render: (_: unknown, record: IncidentSummaryResponse) => (
            <Tag
              color={criticalityColorMap.get(record.criticality) || "default"}
            >
              {t(`incidents.criticality.${record.criticality}`)}
            </Tag>
          ),
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.status"),
          dataIndex: "status",
          key: "status",
          sorter: true,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sortOrder:
            sort?.field === "status" ? toTableSortOrder(sort.order) : undefined,
          render: (_: unknown, record: IncidentSummaryResponse) => (
            <Tag color={statusColorMap.get(record.status) || "default"}>
              {t(`incidents.status.${record.status}`)}
            </Tag>
          ),
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.sla"),
          key: "sla",
          render: (_: unknown, record: IncidentSummaryResponse) => {
            const sla = getIncidentSlaInfo(record);
            if (sla.state === "none")
              return <span className={styles.mutedText}>-</span>;

            return (
              <Tag
                color={SLA_TAG_COLORS.get(sla.state) || "default"}
                className={styles.slaTag}
              >
                {formatIncidentSla(sla, t)}
              </Tag>
            );
          },
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.assigned_to"),
          key: "assignedTo",
          render: (_: unknown, record: IncidentSummaryResponse) =>
            record.assignedTo ? (
              <Typography.Text strong>
                {formatUserName(record.assignedTo)}
              </Typography.Text>
            ) : (
              <Typography.Text type="secondary">
                {t("incidents.detail.no_assigned")}
              </Typography.Text>
            ),
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.created_at"),
          dataIndex: "createdAt",
          key: "createdAt",
          sorter: true,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sortOrder:
            sort?.field === "createdAt"
              ? toTableSortOrder(sort.order)
              : undefined,
          render: (_: unknown, record: IncidentSummaryResponse) =>
            formatDateTime(record.createdAt),
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.age"),
          key: "age",
          render: (_: unknown, record: IncidentSummaryResponse) => (
            <span className={styles.ageText}>
              {formatIncidentAge(getIncidentAgeInfo(record.createdAt), t)}
            </span>
          ),
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.created_by"),
          key: "createdBy",
          render: (_: unknown, record: IncidentSummaryResponse) => (
            <Typography.Text strong>
              {formatUserName(record.createdBy)}
            </Typography.Text>
          ),
        },
        {
          ...commonColumnProps,
          title: t("incidents.table.agency"),
          key: "agency",
          render: (_: unknown, record: IncidentSummaryResponse) =>
            record.agency.name,
        },
      ],
      [t, user?.id, user?.agencyId, sort],
    );

    const handleTableChange = useCallback(
      (
        paginationParam: TablePaginationConfig,
        _filters: unknown,
        sorter: Parameters<typeof readTableSort<IncidentSummaryResponse>>[0],
        extra?: { action?: "paginate" | "sort" | "filter" },
      ) => {
        if (extra?.action === "sort") {
          const { field, order } = readTableSort(sorter);
          onSortChange?.(field, order);
          return;
        }
        onPageChange?.(
          paginationParam.current ?? 1,
          paginationParam.pageSize ?? 10,
        );
      },
      [onSortChange, onPageChange],
    );

    const tablePagination = pagination
      ? {
          ...paginationConfig,
          ...pagination,
          onChange: onPageChange || pagination.onChange,
        }
      : paginationConfig;

    return (
      <DataGrid<IncidentSummaryResponse>
        data={incidents}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={tablePagination}
        scroll={{ x: 1400 }}
        onRowClick={handleRowClick}
        onChange={handleTableChange}
        virtualizationThreshold={100}
        emptyState={
          <EmptyState
            title={t("incidents.empty_title")}
            description={t("incidents.empty_description")}
          />
        }
      />
    );
  },
);

IncidentTable.displayName = "IncidentTable";

export default IncidentTable;
