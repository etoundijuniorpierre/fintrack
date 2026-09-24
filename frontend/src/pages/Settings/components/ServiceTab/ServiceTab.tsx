// Onglet de l'administration dedie a la configuration des services internes.

import { memo, useMemo, useCallback, useState } from "react";
import { Space } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate, useSearchParams } from "react-router-dom";
import type { ColumnsType } from "antd/es/table";
import {
  useDepartments,
  useDeleteDepartment,
} from "../../../../hooks/settings";
import { useAuth } from "../../../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../../../utils/permissions/permissions";
import type { ServiceResponse } from "../../../../api/settings/types";
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
  TableEllipsisText,
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

const SERVICE_STATUS_COLORS: Record<string, string> = {
  active: "success",
  inactive: "error",
};

const SERVICE_SORT_ACCESSORS = {
  headOfService: (service: ServiceResponse) =>
    `${service.headOfService?.lastName ?? ""} ${service.headOfService?.firstName ?? ""}`,
};

// Rend le composant ServiceTab pour l'interface service tab.
const ServiceTab = memo(() => {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();

  const canManage = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);

  const { data: services = [], isLoading } = useDepartments();
  const { mutate: deleteDepartment } = useDeleteDepartment();

  const [searchText, setSearchText] = useState("");

  const [searchParams] = useSearchParams();
  const missingHead = searchParams.get("missingHead") === "true";

  const filteredServices = useMemo(() => {
    let filtered = services;
    if (missingHead) {
      filtered = filtered.filter((s) => !s.headOfService);
    }
    if (searchText.trim()) {
      const q = searchText.toLowerCase();
      filtered = filtered.filter(
        (s) =>
          s.name.toLowerCase().includes(q) ||
          (s.description?.toLowerCase() ?? "").includes(q),
      );
    }
    return filtered;
  }, [services, searchText, missingHead]);
  const {
    data: sortedServices,
    presets: sortPresets,
    activePreset,
    selectPreset,
    columnSorter,
    onChange: handleTableChange,
  } = useClientTableSort(
    "settings_services_sort",
    filteredServices,
    "name",
    "createdAt",
    SERVICE_SORT_ACCESSORS,
  );

  // Traite l'ouverture de la creation.
  const handleOpenCreate = useCallback(() => {
    navigate(APP_ROUTES.SETTINGS_SERVICES_CREATE);
  }, [navigate]);

  // Traite l'ouverture de l'edition.
  const handleOpenEdit = useCallback(
    (id: string) => {
      navigate(APP_ROUTES.SETTINGS_SERVICES_EDIT(id));
    },
    [navigate],
  );

  // Traite la selection de ligne.
  const handleRowClick = useCallback(
    (record: ServiceResponse) => {
      navigate(APP_ROUTES.SETTINGS_SERVICES_DETAILS(record.id));
    },
    [navigate],
  );

  const columns = useMemo<ColumnsType<ServiceResponse>>(
    () => [
      {
        ...commonColumnProps,
        title: t("settings.services.table.name"),
        dataIndex: "name",
        key: "name",
        ...columnSorter("name"),
      },
      {
        ...commonColumnProps,
        title: t("settings.services.table.description"),
        dataIndex: "description",
        key: "description",
        ...columnSorter("description"),
        render: (_: unknown, record: ServiceResponse) => (
          <TableEllipsisText value={record.description} />
        ),
      },
      {
        ...commonColumnProps,
        title: t("settings.services.table.status"),
        dataIndex: "isActive",
        key: "status",
        ...columnSorter("isActive"),
        render: (_: unknown, record: ServiceResponse) => {
          const statusKey = record.isActive ? "active" : "inactive";
          return (
            <StatusTag
              status={statusKey}
              colorMap={SERVICE_STATUS_COLORS}
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
        title: t("settings.services.table.headOfService"),
        key: "headOfService",
        ...columnSorter("headOfService"),
        render: (_: unknown, record: ServiceResponse) =>
          record.headOfService
            ? `${record.headOfService.firstName} ${record.headOfService.lastName}`
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
              title: t("settings.services.table.actions"),
              render: (_: unknown, record: ServiceResponse) => (
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
                    title={t("settings.services.messages.delete_confirm")}
                    description={t("settings.services.messages.delete_ask")}
                    onConfirm={() => deleteDepartment(record.id)}
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
    [t, canManage, handleOpenEdit, deleteDepartment, columnSorter],
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
        {t("settings.services.addButton")}
      </Button>
    ) : undefined,
  });

  return (
    <SectionCard
      title={t("settings.tabs.services")}
      toolbar={toolbar}
      noPadding
    >
      <DataGrid<ServiceResponse>
        data={sortedServices}
        columns={columns}
        rowKey="id"
        loading={isLoading}
        pagination={paginationConfig}
        onChange={handleTableChange}
        onRowClick={handleRowClick}
        scroll={{ x: 800 }}
      />
    </SectionCard>
  );
});

ServiceTab.displayName = "ServiceTab";

export default ServiceTab;
