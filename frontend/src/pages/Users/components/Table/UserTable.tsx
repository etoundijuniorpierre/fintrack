// Tableau de bord interactif pour lister et trier les comptes utilisateurs.

import { Tag, Space, Tooltip, Typography } from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import { useTranslation } from "react-i18next";
import { useMemo, memo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import type { User } from "../../../../api/user/types";
import {
  useDeleteUser,
  useToggleUserStatus,
} from "../../../../hooks/user/useUsers";
import {
  formatDate,
  formatUserName,
} from "../../../../utils/formatters/formatters";
import { userNavigation } from "../../../../utils/navigation/users/users";
import {
  formatRoleName,
} from "../../../../utils/roles/roles";
import { paginationConfig } from "../../../../utils/table/pagination/paginationConfig";
import { useAuth } from "../../../../hooks/auth/useAuth";
import {
  PERMISSIONS,
  hasPermissionName,
  getEffectivePermissionNames,
} from "../../../../utils/permissions/permissions";
import { canPerformDestructiveAction } from "../../../../utils/user";
import {
  DataGrid,
  StatusTag,
  ConfirmDialog,
  EmptyState,
  IconOnlyButton,
  TableEllipsisText,
} from "../../../../components/ui";
import {
  commonColumnProps,
  actionsColumnProps,
} from "../../../../utils/table/tableColumns/tableColumns";
import { actionIcons } from "../../../../utils/icons/appIcons";
import dataGridStyles from "../../../../components/ui/DataGrid/DataGrid.module.scss";
import styles from "./UserTable.module.scss";
import { useOnlinePresence } from "../../../../hooks/user/useOnlinePresence/useOnlinePresence";
import {
  readTableSort,
  TABLE_SORT_DIRECTIONS,
  toTableSortOrder,
} from "../../../../utils/table/sorting/tableSorting";

// Definit les proprietes attendues par le composant UserTable.
interface UserTableProps {
  users: User[];
  loading: boolean;
  pagination?: TablePaginationConfig;
  onPageChange?: (page: number, pageSize: number) => void;
  sort?: { field?: string; order?: "asc" | "desc" };
  onSortChange?: (field?: string, order?: "asc" | "desc") => void;
}

// Filtres serveur de la liste utilisateur (pilotes par le popover de filtres).
export interface UserTableFilterValues {
  role?: string;
  agencyId?: string;
  serviceId?: string;
  active?: boolean;
  connected?: boolean;
}

// Rend le composant UserTable pour l'interface utilisateur tableau.
const UserTable = memo(
  ({ users, loading, pagination, onPageChange, sort, onSortChange }: UserTableProps) => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { hasPermission, user } = useAuth();
    const { isOnline } = useOnlinePresence();
    const { mutate: deleteUser } = useDeleteUser();
    const { mutate: toggleStatus } = useToggleUserStatus();

    const canUpdate = useMemo(
      () => hasPermission(PERMISSIONS.USER.UPDATE),
      [hasPermission],
    );
    const canDelete = useMemo(
      () => hasPermission(PERMISSIONS.USER.DELETE),
      [hasPermission],
    );

    // Traite la selection de ligne.
    const handleRowClick = useCallback(
      (record: User) => {
        userNavigation.navigateToUserDetail(navigate, record.id);
      },
      [navigate],
    );

    const columns = useMemo<ColumnsType<User>>(
      () => [
        {
          ...commonColumnProps,
          title: t("users.table.username"),
          dataIndex: "username",
          key: "username",
          sorter: true,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sortOrder:
            sort?.field === "username"
              ? toTableSortOrder(sort.order)
              : undefined,
            render: (text: string, record: User) => {
            const connected =
              record.id === user?.id ||
              isOnline(record.username);
            const presenceLabel = connected
              ? t("users.table.connected")
              : t("users.table.disconnected");
            return (
              <span className={styles.userIdentity}>
                <Tooltip
                  title={presenceLabel}
                >
                  <span
                    role="img"
                    aria-label={presenceLabel}
                    className={`${styles.presenceDot} ${
                      connected ? styles.presenceOnline : styles.presenceOffline
                    }`}
                  />
                </Tooltip>
                <Typography.Text style={{ fontFamily: "monospace" }}>
                  {text}
                </Typography.Text>
              </span>
            );
          },
        },
        {
          ...commonColumnProps,
          title: t("users.table.fullname"),
          dataIndex: "lastName",
          key: "fullName",
          sorter: true,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sortOrder:
            sort?.field === "lastName"
              ? toTableSortOrder(sort.order)
              : undefined,
          render: (_: unknown, record: User) => 
          <Typography.Text strong>
            {formatUserName(record)}
            </Typography.Text>,
        },
        {
          ...commonColumnProps,
          title: t("users.table.email"),
          dataIndex: "email",
          key: "email",
          sorter: true,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sortOrder:
            sort?.field === "email" ? toTableSortOrder(sort.order) : undefined,
          render: (text: string) => (
            <TableEllipsisText value={text} maxWidth={200} />
          ),
        },
        {
          ...commonColumnProps,
          title: t("users.table.roles"),
          dataIndex: "roles",
          key: "roles",
          render: (roles: User["roles"]) => (
            <>
              {(roles || []).map((r) => (
                <Tag key={r?.id} className={styles.roleTag}>
                  {formatRoleName(r, t)}
                </Tag>
              ))}
            </>
          ),
        },
        {
          ...commonColumnProps,
          title: t("users.table.agency"),
          dataIndex: "agency",
          key: "agency",
          render: (agency: User["agency"]) =>
            agency ? <Tag color="gold">{agency.name}</Tag> : "-",
        },
        {
          ...commonColumnProps,
          title: t("users.table.service"),
          dataIndex: "service",
          key: "service",
          render: (service: User["service"]) =>
            service ? <Tag color="blue">{service.name}</Tag> : "-",
        },
        {
          ...commonColumnProps,
          title: t("users.table.status"),
          dataIndex: "isActive",
          key: "isActive",
          render: (isActive: boolean) => (
            <StatusTag
              status={isActive ? "ACTIVE" : "INACTIVE"}
              colorMap={{ ACTIVE: "success", INACTIVE: "error" }}
              label={
                isActive
                  ? t("users.table.status_active")
                  : t("users.table.status_inactive")
              }
            />
          ),
        },
        {
          ...commonColumnProps,
          title: t("users.table.created"),
          dataIndex: "createdAt",
          key: "createdAt",
          sorter: true,
          sortDirections: TABLE_SORT_DIRECTIONS,
          sortOrder:
            sort?.field === "createdAt"
              ? toTableSortOrder(sort.order)
              : undefined,
          render: (date: string) => formatDate(date),
        },
        {
          ...actionsColumnProps(150),
          title: t("users.table.actions"),
          render: (_: unknown, record: User) => {
            const isSuperAdmin = hasPermissionName(
              getEffectivePermissionNames(record),
              PERMISSIONS.USER.CREATE_ADMIN,
            );
            const isCurrentUser = record.id === user?.id;
            const isProtectedFromCurrentUser = isSuperAdmin && !isCurrentUser;
            const canPerformRowDestructiveAction = canPerformDestructiveAction(
              record.id,
              user?.id,
            );
            if (isProtectedFromCurrentUser) {
              return (
                <Typography.Text type="secondary">
                  {t("users.table.protected")}
                </Typography.Text>
              );
            }
            return (
              <Space
                size={0}
                className={dataGridStyles.actionsSpace}
                onClick={(e) => e.stopPropagation()}
              >
                {canUpdate && (
                  <>
                    <IconOnlyButton
                      variant="text"
                      size="sm"
                      icon={actionIcons.edit}
                      iconClassName={dataGridStyles.editIcon}
                      onClick={() =>
                        userNavigation.navigateToUserDetailEdit(
                          navigate,
                          record.id,
                        )
                      }
                      label={t("common.edit")}
                    />

                    {canPerformRowDestructiveAction && (
                      <IconOnlyButton
                        variant="text"
                        size="sm"
                        icon={actionIcons.toggleUser}
                        iconClassName={
                          record.isActive
                            ? styles.toggleInactiveIcon
                            : styles.toggleActiveIcon
                        }
                        onClick={() => toggleStatus(record.id)}
                        label={
                          record.isActive
                            ? t("users.form.actions.deactivate")
                            : t("users.form.actions.activate")
                        }
                      />
                    )}
                  </>
                )}

                {canDelete && canPerformRowDestructiveAction && (
                  <ConfirmDialog
                    title={t("users.form.messages.delete_confirm")}
                    description={t("users.form.messages.delete_ask")}
                    onConfirm={() => deleteUser(record.id)}
                    confirmText={t("common.yes")}
                    cancelText={t("common.no")}
                    danger
                  >
                    <IconOnlyButton
                      variant="text"
                      size="sm"
                      icon={actionIcons.delete}
                      iconClassName={dataGridStyles.deleteIcon}
                      label={t("common.delete")}
                    />
                  </ConfirmDialog>
                )}
              </Space>
            );
          },
        },
      ],
      [
        t,
        deleteUser,
        toggleStatus,
        canUpdate,
        canDelete,
        navigate,
        user?.id,
        isOnline,
        sort,
      ],
    );

    // Ant Design fournit `extra.action` : on ne traite le tri QUE sur "sort" et la
    // pagination QUE sur "paginate". Sinon un clic de page declenche aussi onSortChange,
    // qui remet a la page 1 (via useTableSort.onChange) et annule la navigation.
    const handleTableChange = useCallback(
      (
        paginationParam: TablePaginationConfig,
        _filters: unknown,
        sorter: Parameters<typeof readTableSort<User>>[0],
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
      <DataGrid<User>
        data={users}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={tablePagination}
        scroll={{ x: 1300 }}
        onRowClick={handleRowClick}
        onChange={handleTableChange}
        virtualizationThreshold={100}
        emptyState={
          <EmptyState
            title={t("users.empty_title")}
            description={t("users.empty_description")}
          />
        }
      />
    );
  },
);

UserTable.displayName = "UserTable";

export default UserTable;
