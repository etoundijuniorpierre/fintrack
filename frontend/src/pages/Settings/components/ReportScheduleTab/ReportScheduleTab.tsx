// Onglet de l'administration dedie a la planification de rapports.

import { memo, useMemo, useCallback, useState } from "react";
import { Space, Tooltip, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import type { ColumnsType } from "antd/es/table";
import {
  useReportSchedules,
  useDeleteReportSchedule,
} from "../../../../hooks/settings";
import { useAuth } from "../../../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../../../utils/permissions/permissions";
import type { ReportScheduleResponse } from "../../../../api/settings/types";
import { formatDate } from "../../../../utils/formatters/formatters";
import { paginationConfig } from "../../../../utils/table/pagination/paginationConfig";
import {
  commonColumnProps,
  actionsColumnProps,
} from "../../../../utils/table/tableColumns/tableColumns";
import {
  Button,
  DataGrid,
  ConfirmDialog,
  IconOnlyButton,
  StatusTag,
  EmptyState,
  TableSortControl,
} from "../../../../components/ui";
import { SectionCard } from "../../../../components/Layout";
import { APP_ROUTES } from "../../../../utils/constants";
import { buildTableToolbar } from "../../../../utils/table/tableToolbar";
import { actionIcons } from "../../../../utils/icons/appIcons";
import dataGridStyles from "../../../../components/ui/DataGrid/DataGrid.module.scss";
import { useClientTableSort } from "../../../../hooks/ui/useClientTableSort/useClientTableSort";
import type { TableSortMode } from "../../../../utils/table/sorting/tableSorting";

const { Text } = Typography;

const REPORT_SCHEDULE_STATUS_COLORS: Record<string, string> = {
  active: "success",
  inactive: "error",
};

const REPORT_SCHEDULE_SORT_ACCESSORS = {
  recipients: (schedule: ReportScheduleResponse) =>
    schedule.recipientEmails?.join(", ") ?? "",
};

// Rend le composant ReportScheduleTab pour l'interface planification de rapport tab.
const ReportScheduleTab = memo(() => {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();

  const canManage = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);

  const { data: reportSchedules = [], isLoading } = useReportSchedules();
  const { mutate: deleteReportSchedule } = useDeleteReportSchedule();

  const [searchText, setSearchText] = useState("");

  const filteredSchedules = useMemo(() => {
    if (!searchText.trim()) return reportSchedules;
    const q = searchText.toLowerCase();
    return reportSchedules.filter((r) => r.name.toLowerCase().includes(q));
  }, [reportSchedules, searchText]);
  const {
    data: sortedSchedules,
    presets: sortPresets,
    activePreset,
    selectPreset,
    columnSorter,
    onChange: handleTableChange,
  } = useClientTableSort(
    "settings_report_schedules_sort",
    filteredSchedules,
    "name",
    "createdAt",
    REPORT_SCHEDULE_SORT_ACCESSORS,
  );

  // Traite l'ouverture de la creation.
  const handleOpenCreate = useCallback(() => {
    navigate(APP_ROUTES.SETTINGS_REPORT_SCHEDULES_CREATE);
  }, [navigate]);

  // Traite l'ouverture de l'edition.
  const handleOpenEdit = useCallback(
    (id: string) => {
      navigate(APP_ROUTES.SETTINGS_REPORT_SCHEDULES_EDIT(id));
    },
    [navigate],
  );

  // Traite la selection de ligne.
  const handleRowClick = useCallback(
    (record: ReportScheduleResponse) => {
      navigate(APP_ROUTES.SETTINGS_REPORT_SCHEDULES_DETAILS(record.id));
    },
    [navigate],
  );

  const columns = useMemo<ColumnsType<ReportScheduleResponse>>(
    () => [
      {
        ...commonColumnProps,
        title: t("settings.reportSchedules.table.name"),
        dataIndex: "name",
        key: "name",
        ...columnSorter("name"),
        render: (value: string) => <Text>{value}</Text>,
      },
      {
        ...commonColumnProps,
        title: t("settings.reportSchedules.table.type"),
        dataIndex: "type",
        key: "type",
        ...columnSorter("type"),
        render: (_: unknown, record: ReportScheduleResponse) =>
          t(`settings.reportSchedules.reportType.${record.type}`, record.type),
      },
      {
        ...commonColumnProps,
        title: t("settings.reportSchedules.table.format"),
        dataIndex: "format",
        key: "format",
        ...columnSorter("format"),
        render: (_: unknown, record: ReportScheduleResponse) =>
          t(
            `settings.reportSchedules.reportFormat.${record.format}`,
            record.format,
          ),
      },
      {
        ...commonColumnProps,
        title: t("settings.reportSchedules.table.recipients"),
        key: "recipients",
        ...columnSorter("recipients"),
        render: (_: unknown, record: ReportScheduleResponse) => {
          const emails = record.recipientEmails ?? [];
          if (emails.length === 0) return "-";
          const display = emails.slice(0, 2).join(", ");
          return emails.length > 2 ? (
            <Tooltip title={emails.join(", ")}>
              <Text>{display}…</Text>
            </Tooltip>
          ) : (
            <Text>{display}</Text>
          );
        },
      },
      {
        ...commonColumnProps,
        title: t("settings.reportSchedules.table.sendTime"),
        dataIndex: "sendTime",
        key: "sendTime",
        ...columnSorter("sendTime"),
        render: (_: unknown, record: ReportScheduleResponse) => (
          <Text>{record.sendTime ?? "-"}</Text>
        ),
      },
      {
        ...commonColumnProps,
        title: t("settings.reportSchedules.table.scope"),
        dataIndex: "scope",
        key: "scope",
        ...columnSorter("scope"),
        render: (_: unknown, record: ReportScheduleResponse) => (
          <Text>
            {record.scope
              ? t(`reports.scope.${record.scope.toLowerCase()}`)
              : "-"}
          </Text>
        ),
      },
      {
        ...commonColumnProps,
        title: t("settings.reportSchedules.table.status"),
        dataIndex: "isActive",
        key: "status",
        ...columnSorter("isActive"),
        render: (_: unknown, record: ReportScheduleResponse) => {
          const statusKey = record.isActive ? "active" : "inactive";
          return (
            <StatusTag
              status={statusKey}
              colorMap={REPORT_SCHEDULE_STATUS_COLORS}
              label={t(
                record.isActive
                  ? "settings.status.active"
                  : "settings.status.inactive",
              )}
            />
          );
        },
      },
      {
        ...commonColumnProps,
        title: t("settings.reportSchedules.table.lastGeneratedAt"),
        dataIndex: "lastGeneratedAt",
        key: "lastGeneratedAt",
        ...columnSorter("lastGeneratedAt"),
        render: (_: unknown, record: ReportScheduleResponse) =>
          record.lastGeneratedAt ? formatDate(record.lastGeneratedAt) : "-",
      },
      {
        ...commonColumnProps,
        title: t("users.table.created"),
        dataIndex: "createdAt",
        key: "createdAt",
        ...columnSorter("createdAt"),
        render: (date: string) => formatDate(date),
      },
      ...(canManage
        ? [
            {
              ...actionsColumnProps(),
              title: t("settings.reportSchedules.table.actions"),
              render: (_: unknown, record: ReportScheduleResponse) => (
                <Space
                  size={0}
                  className={dataGridStyles.actionsSpace}
                  onClick={(e: React.MouseEvent) => e.stopPropagation()}
                >
                  <IconOnlyButton
                    variant="text"
                    size="sm"
                    icon={actionIcons.edit}
                    iconClassName={dataGridStyles.editIcon}
                    onClick={() => handleOpenEdit(record.id)}
                    label={t("settings.buttons.edit")}
                  />
                  <ConfirmDialog
                    title={t(
                      "settings.reportSchedules.messages.delete_confirm",
                    )}
                    description={t(
                      "settings.reportSchedules.messages.delete_ask",
                    )}
                    onConfirm={() => deleteReportSchedule(record.id)}
                    confirmText={t("settings.buttons.confirm")}
                    cancelText={t("settings.buttons.cancel")}
                    danger
                  >
                    <IconOnlyButton
                      variant="text"
                      size="sm"
                      icon={actionIcons.delete}
                      iconClassName={dataGridStyles.deleteIcon}
                      label={t("settings.buttons.delete")}
                    />
                  </ConfirmDialog>
                </Space>
              ),
            },
          ]
        : []),
    ],
    [t, canManage, handleOpenEdit, deleteReportSchedule, columnSorter],
  );

  const toolbar = buildTableToolbar({
    searchValue: searchText,
    onSearch: (e) => setSearchText(e.target.value),
    searchPlaceholder: t("common.search"),
    leading: (
      <TableSortControl<TableSortMode>
        presets={sortPresets}
        labels={{
          alphabetical: t("common.sort.alphabetical"),
          seniority: t("common.sort.seniority"),
        }}
        label={t("common.sort.label")}
        activePreset={activePreset}
        onSelect={selectPreset}
      />
    ),
    addButton: canManage ? (
      <Button
        variant="primary"
        icon={actionIcons.add}
        onClick={handleOpenCreate}
      >
        {t("settings.reportSchedules.addButton")}
      </Button>
    ) : undefined,
  });

  return (
    <SectionCard
      title={t("settings.tabs.reportSchedules")}
      toolbar={toolbar}
      noPadding
    >
      <DataGrid<ReportScheduleResponse>
        data={sortedSchedules}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={paginationConfig}
        onChange={handleTableChange}
        onRowClick={handleRowClick}
        scroll={{ x: 1100 }}
        emptyState={<EmptyState title={t("settings.common.noData")} />}
      />
    </SectionCard>
  );
});

ReportScheduleTab.displayName = "ReportScheduleTab";

export default ReportScheduleTab;
