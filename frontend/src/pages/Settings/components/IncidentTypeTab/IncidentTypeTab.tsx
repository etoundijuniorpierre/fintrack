// Onglet de l'administration dedie aux types d'incidents.

import { memo, useMemo, useCallback, useState } from "react";
import { Space, Typography, Tag } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { superAdminApi } from "../../../../api/superAdmin";
import { APP_ROUTES, QUERY_KEYS } from "../../../../utils/constants";
import type { ColumnsType } from "antd/es/table";
import {
  useSettingsIncidentTypes,
  useDeleteIncidentType,
} from "../../../../hooks/settings";
import { useAuth } from "../../../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../../../utils/permissions/permissions";
import type { IncidentTypeConfigResponse } from "../../../../api/settings/types";
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

import { buildTableToolbar } from "../../../../utils/table/tableToolbar";
import { actionIcons } from "../../../../utils/icons/appIcons";
import dataGridStyles from "../../../../components/ui/DataGrid/DataGrid.module.scss";
import { useClientTableSort } from "../../../../hooks/ui/useClientTableSort/useClientTableSort";
import type { TableSortMode } from "../../../../utils/table/sorting/tableSorting";
import { formatDate } from "../../../../utils/formatters/formatters";

const { Text } = Typography;

const INCIDENT_TYPE_STATUS_COLORS: Record<string, string> = {
  active: "success",
  inactive: "error",
};

const INCIDENT_TYPE_SORT_ACCESSORS = {
  defaultTargetService: (type: IncidentTypeConfigResponse) =>
    type.defaultTargetService?.name ?? "",
  defaultTargetUser: (type: IncidentTypeConfigResponse) =>
    `${type.defaultTargetUser?.lastName ?? ""} ${type.defaultTargetUser?.firstName ?? ""}`,
};

// Rend le composant IncidentTypeTab pour l'interface incident type tab.
const IncidentTypeTab = memo(() => {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();

  const canManage = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);

  const { data: incidentTypes = [], isLoading } = useSettingsIncidentTypes();
  const { mutate: deleteIncidentType } = useDeleteIncidentType();

  const [searchText, setSearchText] = useState("");

  const [searchParams] = useSearchParams();
  const unused = searchParams.get("unused") === "true";

  const { data: controlsQuality, isFetching: isControlsFetching } = useQuery({
    queryKey: QUERY_KEYS.SUPER_ADMIN.CONTROLS_QUALITY,
    queryFn: ({ signal }) => superAdminApi.getControlsQuality(signal),
    enabled: unused && canManage,
    staleTime: 60_000,
  });

  const filteredTypes = useMemo(() => {
    let filtered = incidentTypes;

    if (unused) {
      const unusedList = controlsQuality?.dataQuality?.unusedIncidentTypes as
        | Array<{ id: string }>
        | undefined;
      if (unusedList) {
        const unusedIds = unusedList.map((item) => item.id);
        filtered = filtered.filter((it) => unusedIds.includes(it.id));
      } else {
        // Donnees pas encore chargees — afficher rien en attendant
        filtered = [];
      }
    }

    if (searchText.trim()) {
      const q = searchText.toLowerCase();
      filtered = filtered.filter(
        (it) =>
          it.name.toLowerCase().includes(q) ||
          it.displayName.toLowerCase().includes(q),
      );
    }

    return filtered;
  }, [incidentTypes, searchText, unused, controlsQuality]);
  const {
    data: sortedTypes,
    presets: sortPresets,
    activePreset,
    selectPreset,
    columnSorter,
    onChange: handleTableChange,
  } = useClientTableSort(
    "settings_incident_types_sort",
    filteredTypes,
    "displayName",
    "createdAt",
    INCIDENT_TYPE_SORT_ACCESSORS,
  );

  // Traite l'ouverture de la creation.
  const handleOpenCreate = useCallback(() => {
    navigate(APP_ROUTES.SETTINGS_INCIDENT_TYPES_CREATE);
  }, [navigate]);

  // Traite l'ouverture de l'edition.
  const handleOpenEdit = useCallback(
    (id: string) => {
      navigate(APP_ROUTES.SETTINGS_INCIDENT_TYPES_EDIT(id));
    },
    [navigate],
  );

  // Traite la selection de ligne.
  const handleRowClick = useCallback(
    (record: IncidentTypeConfigResponse) => {
      navigate(APP_ROUTES.SETTINGS_INCIDENT_TYPES_DETAILS(record.id));
    },
    [navigate],
  );

  const columns = useMemo<ColumnsType<IncidentTypeConfigResponse>>(
    () => [
      {
        ...commonColumnProps,
        title: t("settings.incidentTypes.table.displayName"),
        dataIndex: "displayName",
        key: "displayName",
        ...columnSorter("displayName"),
        render: (_: string, record: IncidentTypeConfigResponse) => (
          <div style={{ display: "flex", flexDirection: "column", gap: "2px" }}>
            <Text style={{ fontWeight: 600 }}>{record.displayName}</Text>
            <Text type="secondary" style={{ fontSize: "12px" }}>
              {record.name}
            </Text>
          </div>
        ),
      },
      {
        ...commonColumnProps,
        title: t("settings.incidentTypes.table.slaHours"),
        dataIndex: "slaHours",
        key: "slaHours",
        ...columnSorter("slaHours"),
        render: (_: unknown, record: IncidentTypeConfigResponse) =>
          record.slaHours ? (
            <Tag color="blue">{record.slaHours} h</Tag>
          ) : (
            "-"
          ),
      },
      {
        ...commonColumnProps,
        title: t("settings.incidentTypes.table.closerType"),
        dataIndex: "closerRoles",
        key: "closerRoles",
        render: (value: IncidentTypeConfigResponse["closerRoles"]) =>
          value && value.length > 0 ? (
            <Space size={[0, 4]} wrap>
              {value.map((role) => (
                <Tag key={role} color="purple">
                  {t(`settings.incidentTypes.form.actorRoles.${role}`)}
                </Tag>
              ))}
            </Space>
          ) : (
            "-"
          ),
      },
      {
        ...commonColumnProps,
        title: t("settings.incidentTypes.table.status"),
        dataIndex: "isActive",
        key: "status",
        ...columnSorter("isActive"),
        render: (_: unknown, record: IncidentTypeConfigResponse) => {
          const statusKey = record.isActive ? "active" : "inactive";
          return (
            <StatusTag
              status={statusKey}
              colorMap={INCIDENT_TYPE_STATUS_COLORS}
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
        title: t("settings.incidentTypes.table.defaultTargetService"),
        key: "defaultTargetService",
        ...columnSorter("defaultTargetService"),
        render: (_: unknown, record: IncidentTypeConfigResponse) =>
          record.defaultTargetService?.name ?? "-",
      },
      {
        ...commonColumnProps,
        title: t("settings.incidentTypes.table.defaultTargetUser"),
        key: "defaultTargetUser",
        ...columnSorter("defaultTargetUser"),
        render: (_: unknown, record: IncidentTypeConfigResponse) =>
          record.defaultTargetUser
            ? `${record.defaultTargetUser.firstName} ${record.defaultTargetUser.lastName}`
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
              title: t("settings.incidentTypes.table.actions"),
              render: (_: unknown, record: IncidentTypeConfigResponse) => (
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
                    title={t("settings.incidentTypes.messages.delete_confirm")}
                    description={t(
                      "settings.incidentTypes.messages.delete_ask",
                    )}
                    onConfirm={() => deleteIncidentType(record.id)}
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
    [t, canManage, handleOpenEdit, deleteIncidentType, columnSorter],
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
        {t("settings.incidentTypes.addButton")}
      </Button>
    ) : undefined,
  });

  return (
    <SectionCard
      title={t("settings.tabs.incidentTypes")}
      toolbar={toolbar}
      noPadding
    >
      <DataGrid<IncidentTypeConfigResponse>
        data={sortedTypes}
        columns={columns}
        rowKey="id"
        loading={isLoading || (unused && isControlsFetching)}
        pagination={paginationConfig}
        onChange={handleTableChange}
        onRowClick={handleRowClick}
        scroll={{ x: 900 }}
      />
    </SectionCard>
  );
});

IncidentTypeTab.displayName = "IncidentTypeTab";

export default IncidentTypeTab;
