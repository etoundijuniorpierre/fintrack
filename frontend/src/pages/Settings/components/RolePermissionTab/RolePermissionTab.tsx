// Onglet de l'administration dedie a la configuration des roles et habilitations.

import { memo, useMemo, useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Tag, Space } from "antd";
import { useTranslation } from "react-i18next";
import type { ColumnsType } from "antd/es/table";
import { useRoles, useDeleteRole } from "../../../../hooks/settings";
import { useAuth } from "../../../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../../../utils/permissions/permissions";
import type { RoleResponse } from "../../../../api/settings/types";
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
  TableEllipsisText,
  TableSortControl,
} from "../../../../components/ui";
import { SectionCard } from "../../../../components/Layout";
import { APP_ROUTES } from "../../../../utils/constants";
import {
  getRoleTranslationKey,
  formatRoleName,
} from "../../../../utils/roles/roles";
import { buildTableToolbar } from "../../../../utils/table/tableToolbar";
import { actionIcons } from "../../../../utils/icons/appIcons";
import dataGridStyles from "../../../../components/ui/DataGrid/DataGrid.module.scss";
import { useClientTableSort } from "../../../../hooks/ui/useClientTableSort/useClientTableSort";
import type { TableSortMode } from "../../../../utils/table/sorting/tableSorting";
import { formatDate } from "../../../../utils/formatters/formatters";

// Rend le composant RolePermissionTab pour l'interface role permission tab.
const RolePermissionTab = memo(() => {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();

  const canCreate = hasPermission(PERMISSIONS.ROLE.CREATE);
  const canUpdate = hasPermission(PERMISSIONS.ROLE.UPDATE);
  const canDelete = hasPermission(PERMISSIONS.ROLE.DELETE);
  const canManageAny = canCreate || canUpdate || canDelete;

  const { data: roles = [], isLoading } = useRoles();
  const { mutate: deleteRole } = useDeleteRole();

  const [searchText, setSearchText] = useState("");

  const filteredRoles = useMemo(() => {
    if (!searchText.trim()) return roles;
    const q = searchText.toLowerCase();
    return roles.filter(
      (r) =>
        r.name.toLowerCase().includes(q) ||
        (r.description?.toLowerCase() ?? "").includes(q),
    );
  }, [roles, searchText]);
  const roleSortAccessors = useMemo(
    () => ({
      name: (role: RoleResponse) => formatRoleName(role, t),
      permissions: (role: RoleResponse) => role.permissions?.length ?? 0,
    }),
    [t],
  );
  const {
    data: sortedRoles,
    presets: sortPresets,
    activePreset,
    selectPreset,
    columnSorter,
    onChange: handleTableChange,
  } = useClientTableSort(
    "settings_roles_sort",
    filteredRoles,
    "name",
    "createdAt",
    roleSortAccessors,
  );

  // Traite l'ouverture de la creation.
  const handleOpenCreate = useCallback(() => {
    navigate(APP_ROUTES.SETTINGS_ROLES_CREATE);
  }, [navigate]);

  // Traite l'ouverture de l'edition.
  const handleOpenEdit = useCallback(
    (id: string) => {
      navigate(APP_ROUTES.SETTINGS_ROLES_EDIT(id));
    },
    [navigate],
  );

  // Traite la selection de ligne.
  const handleRowClick = useCallback(
    (record: RoleResponse) => {
      navigate(APP_ROUTES.SETTINGS_ROLES_DETAILS(record.id));
    },
    [navigate],
  );

  const columns = useMemo<ColumnsType<RoleResponse>>(
    () => [
      {
        ...commonColumnProps,
        title: t("settings.roles.table.name"),
        key: "name",
        ...columnSorter("name"),
        render: (_: unknown, record: RoleResponse) => formatRoleName(record, t),
      },
      {
        ...commonColumnProps,
        title: t("settings.roles.table.description"),
        dataIndex: "description",
        key: "description",
        ...columnSorter("description"),
        render: (_: unknown, record: RoleResponse) => {
          const key = getRoleTranslationKey(record.name);
          const description = t(`users.role_descriptions.${key}`, {
            defaultValue: record.description ?? "-",
          });
          return <TableEllipsisText value={description} />;
        },
      },
      {
        ...commonColumnProps,
        title: t("settings.roles.table.isSystem"),
        dataIndex: "isSystem",
        key: "isSystem",
        ...columnSorter("isSystem"),
        render: (_: unknown, record: RoleResponse) => (
          <Tag color={record.isSystem ? "gold" : "default"}>
            {record.isSystem
              ? t("settings.roles.system.yes")
              : t("settings.roles.system.no")}
          </Tag>
        ),
      },
      {
        ...commonColumnProps,
        title: t("settings.roles.table.permissions"),
        key: "permissions",
        ...columnSorter("permissions"),
        render: (_: unknown, record: RoleResponse) => (
          <Tag color="blue">{record.permissions?.length ?? 0}</Tag>
        ),
      },
      {
        ...commonColumnProps,
        title: t("users.table.created"),
        dataIndex: "createdAt",
        key: "createdAt",
        ...columnSorter("createdAt"),
        render: (date: string) => formatDate(date),
      },
      ...(canManageAny
        ? [
            {
              ...actionsColumnProps(),
              title: t("settings.roles.table.actions"),
              render: (_: unknown, record: RoleResponse) => (
                <Space
                  size={0}
                  className={dataGridStyles.actionsSpace}
                  onClick={(e: React.MouseEvent) => e.stopPropagation()}
                >
                  {canUpdate && (
                    <IconOnlyButton
                      variant="text"
                      size="sm"
                      icon={actionIcons.edit}
                      iconClassName={dataGridStyles.editIcon}
                      onClick={() => handleOpenEdit(record.id)}
                      label={t("settings.buttons.edit")}
                    />
                  )}
                  {canDelete && (
                    <ConfirmDialog
                      title={t("settings.roles.messages.delete_confirm")}
                      description={t("settings.roles.messages.delete_ask")}
                      onConfirm={() => deleteRole(record.id)}
                      confirmText={t("settings.buttons.confirm")}
                      cancelText={t("settings.buttons.cancel")}
                      danger
                    >
                      <IconOnlyButton
                        variant="text"
                        size="sm"
                        disabled={record.isSystem}
                        icon={actionIcons.delete}
                        iconClassName={dataGridStyles.deleteIcon}
                        label={t("settings.buttons.delete")}
                        tooltip={
                          record.isSystem
                            ? t("settings.roles.messages.system_role_tooltip")
                            : t("settings.buttons.delete")
                        }
                      />
                    </ConfirmDialog>
                  )}
                </Space>
              ),
            },
          ]
        : []),
    ],
    [
      t,
      canManageAny,
      canUpdate,
      canDelete,
      handleOpenEdit,
      deleteRole,
      columnSorter,
    ],
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
    addButton: canCreate ? (
      <Button
        variant="primary"
        icon={actionIcons.add}
        onClick={handleOpenCreate}
      >
        {t("settings.roles.addButton")}
      </Button>
    ) : undefined,
  });

  return (
    <SectionCard
      title={t("settings.tabs.rolesPermissions")}
      toolbar={toolbar}
      noPadding
    >
      <DataGrid<RoleResponse>
        data={sortedRoles}
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

RolePermissionTab.displayName = "RolePermissionTab";

export default RolePermissionTab;
