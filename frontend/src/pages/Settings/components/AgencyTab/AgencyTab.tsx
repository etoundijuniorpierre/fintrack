// Onglet de l'administration dedie a la gestion des agences.

import { memo, useMemo, useCallback, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Space } from "antd";
import { useTranslation } from "react-i18next";
import type { ColumnsType } from "antd/es/table";
import { useAgencies, useDeleteAgency } from "../../../../hooks/settings";
import { useAuth } from "../../../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../../../utils/permissions/permissions";
import type { AgencyResponse } from "../../../../api/settings/types";
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
  TableSortControl,
} from "../../../../components/ui";
import { SectionCard } from "../../../../components/Layout";
import { APP_ROUTES } from "../../../../utils/constants";
import { buildTableToolbar } from "../../../../utils/table/tableToolbar";
import { actionIcons } from "../../../../utils/icons/appIcons";
import dataGridStyles from "../../../../components/ui/DataGrid/DataGrid.module.scss";
import { useClientTableSort } from "../../../../hooks/ui/useClientTableSort/useClientTableSort";
import type { TableSortMode } from "../../../../utils/table/sorting/tableSorting";
import { formatDate } from "../../../../utils/formatters/formatters";

const AGENCY_STATUS_COLORS: Record<string, string> = {
  active: "success",
  inactive: "error",
};

const AGENCY_SORT_ACCESSORS = {
  headOfAgency: (agency: AgencyResponse) =>
    `${agency.headOfAgency?.lastName ?? ""} ${agency.headOfAgency?.firstName ?? ""}`,
};

// Rend le composant AgencyTab pour l'interface agence tab.
const AgencyTab = memo(() => {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();

  const canManage = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);

  const { data: agencies = [], isLoading } = useAgencies();
  const { mutate: deleteAgency } = useDeleteAgency();

  const [searchText, setSearchText] = useState("");

  const [searchParams] = useSearchParams();
  const missingHead = searchParams.get("missingHead") === "true";

  const filteredAgencies = useMemo(() => {
    let filtered = agencies;
    if (missingHead) {
      filtered = filtered.filter((a) => !a.headOfAgency);
    }
    if (searchText.trim()) {
      const q = searchText.toLowerCase();
      filtered = filtered.filter(
        (a) =>
          a.name.toLowerCase().includes(q) ||
          (a.code?.toLowerCase() ?? "").includes(q),
      );
    }
    return filtered;
  }, [agencies, searchText, missingHead]);
  const {
    data: sortedAgencies,
    presets: sortPresets,
    activePreset,
    selectPreset,
    columnSorter,
    onChange: handleTableChange,
  } = useClientTableSort(
    "settings_agencies_sort",
    filteredAgencies,
    "name",
    "createdAt",
    AGENCY_SORT_ACCESSORS,
  );

  // Traite l'ouverture de la creation.
  const handleOpenCreate = useCallback(() => {
    navigate(APP_ROUTES.SETTINGS_AGENCIES_CREATE);
  }, [navigate]);

  // Traite l'ouverture de l'edition.
  const handleOpenEdit = useCallback(
    (id: string) => {
      navigate(APP_ROUTES.SETTINGS_AGENCIES_EDIT(id));
    },
    [navigate],
  );

  // Traite la selection de ligne.
  const handleRowClick = useCallback(
    (record: AgencyResponse) => {
      navigate(APP_ROUTES.SETTINGS_AGENCIES_DETAILS(record.id));
    },
    [navigate],
  );

  const columns = useMemo<ColumnsType<AgencyResponse>>(
    () => [
      {
        ...commonColumnProps,
        title: t("settings.agencies.table.code"),
        dataIndex: "code",
        key: "code",
        ...columnSorter("code"),
      },
      {
        ...commonColumnProps,
        title: t("settings.agencies.table.name"),
        dataIndex: "name",
        key: "name",
        ...columnSorter("name"),
      },
      {
        ...commonColumnProps,
        title: t("settings.agencies.table.address"),
        dataIndex: "address",
        key: "address",
        ...columnSorter("address"),
        render: (_: unknown, record: AgencyResponse) => record.address ?? "-",
      },
      {
        ...commonColumnProps,
        title: t("settings.agencies.table.status"),
        dataIndex: "isActive",
        key: "status",
        ...columnSorter("isActive"),
        render: (_: unknown, record: AgencyResponse) => {
          const statusKey = record.isActive ? "active" : "inactive";
          return (
            <StatusTag
              status={statusKey}
              colorMap={AGENCY_STATUS_COLORS}
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
        title: t("settings.agencies.table.headOfAgency"),
        key: "headOfAgency",
        ...columnSorter("headOfAgency"),
        render: (_: unknown, record: AgencyResponse) =>
          record.headOfAgency
            ? `${record.headOfAgency.firstName} ${record.headOfAgency.lastName}`
            : "-",
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
              title: t("settings.agencies.table.actions"),
              render: (_: unknown, record: AgencyResponse) => (
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
                    title={t("settings.agencies.messages.delete_confirm")}
                    description={t("settings.agencies.messages.delete_ask")}
                    onConfirm={() => deleteAgency(record.id)}
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
    [t, canManage, handleOpenEdit, deleteAgency, columnSorter],
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
        {t("settings.agencies.addButton")}
      </Button>
    ) : undefined,
  });

  return (
    <SectionCard
      title={t("settings.tabs.agencies")}
      toolbar={toolbar}
      noPadding
    >
      <DataGrid<AgencyResponse>
        data={sortedAgencies}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={paginationConfig}
        onChange={handleTableChange}
        onRowClick={handleRowClick}
        scroll={{ x: 900 }}
      />
    </SectionCard>
  );
});

AgencyTab.displayName = "AgencyTab";

export default AgencyTab;
