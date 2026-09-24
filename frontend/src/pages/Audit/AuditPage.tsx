// Page Audit : affiche le journal des actions, avec recherche, filtre par dates et detail modal.
import { memo, useMemo, useCallback, useState, useEffect } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { Space, Button, Typography } from "antd";
import { LinkOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import type { Dayjs } from "dayjs";
import { useAuth } from "../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../utils/permissions/permissions";
import {
  useAuditLogs,
  useAuditActions,
  useAuditStatuses,
} from "../../hooks/audit";
import type { AuditLogResponse } from "../../api/audit";
import { AUDIT_STATUS_COLORS } from "../../api/audit";
import {
  formatDateTime,
  formatUserName,
} from "../../utils/formatters/formatters";
import { resourceNavigation } from "../../utils/navigation";
import {
  DEFAULT_PAGE_SIZE,
  getPaginationConfig,
} from "../../utils/table/pagination/paginationConfig";
import { commonColumnProps } from "../../utils/table/tableColumns/tableColumns";
import { buildTableToolbar } from "../../utils/table/tableToolbar";
import { PageLoader } from "../../components";
import {
  PageHeader,
  DataGrid,
  StatusTag,
  TableSortControl,
  useTableFilters,
} from "../../components/ui";
import type { TableFilterField } from "../../components/ui";
import { PageContainer, SectionCard } from "../../components/Layout";
import AuditDetailModal from "./components/AuditDetailModal/AuditDetailModal";
import { useTableSort } from "../../hooks/ui/useTableSort/useTableSort";
import {
  createTableSortPresets,
  readTableSort,
  TABLE_SORT_DIRECTIONS,
  toTableSortOrder,
  type TableSortMode,
} from "../../utils/table/sorting/tableSorting";

const { Text } = Typography;

const AUDIT_SORT_PRESETS = createTableSortPresets("username", "timestamp");

// Rend le composant AuditPageInner pour l'interface audit page.
const AuditPageInner = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: auditActions = [] } = useAuditActions();
  const { data: auditStatuses = [] } = useAuditStatuses();

  const [filterDateRange, setFilterDateRange] = useState<
    [Dayjs | null, Dayjs | null] | null
  >(null);
  const [filterAction, setFilterAction] = useState<string | undefined>(
    undefined,
  );
  const [filterStatus, setFilterStatus] = useState<string | undefined>(
    undefined,
  );
  const [searchText, setSearchText] = useState("");
  const [debouncedSearchText, setDebouncedSearchText] = useState("");
  const [selectedRecord, setSelectedRecord] = useState<AuditLogResponse | null>(
    null,
  );
  const [modalOpen, setModalOpen] = useState<boolean>(false);
  const [paginationState, setPaginationState] = useState({
    current: 1,
    pageSize: DEFAULT_PAGE_SIZE,
  });
  const {
    field: sortField,
    order: sortOrder,
    sortParam,
    activePreset: activeSortPreset,
    selectPreset,
    handleColumnSortChange,
  } = useTableSort<TableSortMode>(
    "audit_sort",
    AUDIT_SORT_PRESETS,
    "seniority",
    {
      onChange: () =>
        setPaginationState((current) => ({ ...current, current: 1 })),
    },
  );

  useEffect(() => {
    // Differe la recherche pour limiter les requetes pendant la saisie.
    const handler = setTimeout(() => {
      setDebouncedSearchText(searchText);
      setPaginationState((current) => ({ ...current, current: 1 }));
    }, 500);
    return () => clearTimeout(handler);
  }, [searchText]);

  // Construit les parametres de requete a partir de la plage de dates selectionnee.
  const params = useMemo(
    () => ({
      page: paginationState.current - 1,
      size: paginationState.pageSize,
      ...(filterAction ? { action: filterAction } : {}),
      ...(filterStatus ? { status: filterStatus } : {}),
      ...(debouncedSearchText.trim()
        ? { keyword: debouncedSearchText.trim() }
        : {}),
      ...(filterDateRange?.[0]
        ? { from: filterDateRange[0].toISOString() }
        : {}),
      ...(filterDateRange?.[1] ? { to: filterDateRange[1].toISOString() } : {}),
      sort: sortParam,
    }),
    [
      filterAction,
      filterStatus,
      debouncedSearchText,
      filterDateRange,
      paginationState,
      sortParam,
    ],
  );

  const { data, isLoading, isError } = useAuditLogs(params);

  // Ouvre le modal de detail sur la ligne cliquee.
  const handleRowClick = useCallback((record: AuditLogResponse) => {
    setSelectedRecord(record);
    setModalOpen(true);
  }, []);

  // Traite la fermeture de la modale.
  const handleCloseModal = useCallback(() => {
    setModalOpen(false);
    setSelectedRecord(null);
  }, []);

  // Traite l'action utilisateur liee a date periode change.
  const handleDateRangeChange = useCallback(
    (dates: [Dayjs | null, Dayjs | null] | null) => {
      setFilterDateRange(dates);
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [],
  );
  // Traite l'action utilisateur liee a search.
  const handleSearch = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => setSearchText(e.target.value),
    [],
  );
  // Traite le changement de page.
  const handlePageChange = useCallback((page: number, pageSize: number) => {
    setPaginationState({ current: page, pageSize });
  }, []);
  // Les filtres action/statut sont appliques cote serveur (les logs sont
  // pagines : un filtre client ne verrait que la page courante). Tout
  // changement reinitialise la pagination a la premiere page.
  const handleActionChange = useCallback((value?: string) => {
    setFilterAction(value);
    setPaginationState((current) => ({ ...current, current: 1 }));
  }, []);
  // Traite le changement de statut.
  const handleStatusChange = useCallback((value?: string) => {
    setFilterStatus(value);
    setPaginationState((current) => ({ ...current, current: 1 }));
  }, []);

  const actionOptions = useMemo(
    () =>
      auditActions.map((item) => ({
        label: t(`audit.action.${item.code}`, item.name),
        value: item.code,
      })),
    [auditActions, t],
  );

  const statusOptions = useMemo(
    () =>
      auditStatuses.map((item) => ({
        label: t(`audit.status.${item.code}`, item.name),
        value: item.code,
      })),
    [auditStatuses, t],
  );

  const columns = useMemo<ColumnsType<AuditLogResponse>>(
    () => [
      {
        ...commonColumnProps,
        title: t("audit.table.timestamp"),
        dataIndex: "timestamp",
        key: "timestamp",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "timestamp" ? toTableSortOrder(sortOrder) : undefined,
        render: (_: unknown, record: AuditLogResponse) =>
          formatDateTime(record.timestamp),
      },
      {
        ...commonColumnProps,
        title: t("audit.table.user"),
        dataIndex: "username",
        key: "user",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "username" ? toTableSortOrder(sortOrder) : undefined,
        render: (_: unknown, record: AuditLogResponse) => {
          // Formate le nom affichable d'un acteur d'audit.
          const userNameStr = formatUserName(
            record.user,
            record.username,
            t("audit.table.unknownUser"),
          );
          const userId = record.user?.id;
          const userLabel = (
            <Space direction="vertical" size={0}>
              <span>{userNameStr}</span>
              {userId && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {userId}
                </Text>
              )}
            </Space>
          );
          if (record.user?.id) {
            return (
              <Space>
                {userLabel}
                <Button
                  type="link"
                  size="small"
                  icon={<LinkOutlined />}
                  onClick={(e) => {
                    e.stopPropagation();
                    resourceNavigation.navigateToResourceDetail(
                      navigate,
                      "USER",
                      record.user!.id,
                    );
                  }}
                  title={t("audit.table.goToUser")}
                />
              </Space>
            );
          }
          return userLabel;
        },
      },
      {
        ...commonColumnProps,
        title: t("audit.table.action"),
        dataIndex: "action",
        key: "action",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "action" ? toTableSortOrder(sortOrder) : undefined,
        render: (_: unknown, record: AuditLogResponse) => {
          const matchingEnum = auditActions.find(
            (a) => a.code === record.action,
          );
          return t(
            `audit.action.${record.action}`,
            matchingEnum ? matchingEnum.name : record.action,
          );
        },
      },
      {
        ...commonColumnProps,
        title: t("audit.table.resourceType"),
        dataIndex: "resourceType",
        key: "resourceType",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "resourceType"
            ? toTableSortOrder(sortOrder)
            : undefined,
        render: (type: string, record: AuditLogResponse) => {
          const path = resourceNavigation.getResourceDetailPath(
            type,
            record.resourceId,
          );
          if (path) {
            return (
              <Space>
                {type}
                <Button
                  type="link"
                  size="small"
                  icon={<LinkOutlined />}
                  onClick={(e) => {
                    e.stopPropagation(); // Évite d'ouvrir le modal de détail d'audit
                    navigate(path);
                  }}
                  title={t("audit.table.goToResource")}
                />
              </Space>
            );
          }
          return type;
        },
      },
      {
        ...commonColumnProps,
        title: t("audit.table.resourceId"),
        dataIndex: "resourceId",
        key: "resourceId",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "resourceId"
            ? toTableSortOrder(sortOrder)
            : undefined,
        render: (_: unknown, record: AuditLogResponse) =>
          record.resourceId || "-",
      },
      {
        ...commonColumnProps,
        title: t("audit.table.ipAddress"),
        dataIndex: "ipAddress",
        key: "ipAddress",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "ipAddress" ? toTableSortOrder(sortOrder) : undefined,
        render: (_: unknown, record: AuditLogResponse) =>
          record.ipAddress ?? "-",
      },
      {
        ...commonColumnProps,
        title: t("audit.table.status"),
        dataIndex: "status",
        key: "status",
        sorter: true,
        sortDirections: TABLE_SORT_DIRECTIONS,
        sortOrder:
          sortField === "status" ? toTableSortOrder(sortOrder) : undefined,
        render: (_: unknown, record: AuditLogResponse) => (
          <StatusTag
            status={record.status}
            colorMap={AUDIT_STATUS_COLORS}
            label={t(`audit.status.${record.status}`)}
          />
        ),
      },
    ],
    [t, auditActions, navigate, sortField, sortOrder],
  );

  const dataSource = useMemo(() => data?.content ?? [], [data]);
  const total = data?.totalElements ?? 0;
  const { current, pageSize } = paginationState;
  const tablePagination = useMemo<TablePaginationConfig>(
    () =>
      getPaginationConfig({
        current,
        pageSize,
        total,
        onChange: handlePageChange,
        onShowSizeChange: handlePageChange,
      }),
    [handlePageChange, current, pageSize, total],
  );

  // Barre d'outils : recherche globale et selecteur de plage de dates.
  const filterFields = useMemo<TableFilterField[]>(
    () => [
      {
        key: "action",
        type: "select",
        label: t("audit.filters.action"),
        options: actionOptions,
        value: filterAction,
        onChange: handleActionChange,
      },
      {
        key: "status",
        type: "select",
        label: t("audit.filters.status"),
        options: statusOptions,
        value: filterStatus,
        onChange: handleStatusChange,
      },
      {
        key: "dateRange",
        type: "daterange",
        label: t("audit.filters.dateRange"),
        value: filterDateRange,
        onChange: handleDateRangeChange,
      },
    ],
    [
      t,
      actionOptions,
      filterAction,
      handleActionChange,
      statusOptions,
      filterStatus,
      handleStatusChange,
      filterDateRange,
      handleDateRangeChange,
    ],
  );

  const { trigger: filtersTrigger, chips: filtersChips } =
    useTableFilters(filterFields);

  const toolbar = buildTableToolbar({
    searchValue: searchText,
    onSearch: handleSearch,
    searchPlaceholder: t("audit.filters.search"),
    leading: (
      <TableSortControl<TableSortMode>
        presets={AUDIT_SORT_PRESETS}
        labels={{
          alphabetical: t("common.sort.alphabetical"),
          seniority: t("common.sort.seniority"),
        }}
        label={t("common.sort.label")}
        activePreset={activeSortPreset}
        onSelect={selectPreset}
      />
    ),
    extra: filtersTrigger,
    activeFilters: filtersChips,
  });

  // Ant Design fournit `extra.action` : tri QUE sur "sort", pagination QUE sur "paginate".
  // Sinon un clic de page declenche aussi le tri, qui remet a la page 1 et annule la navigation.
  const handleTableChange = useCallback(
    (
      paginationParam: TablePaginationConfig,
      _filters: unknown,
      sorter: Parameters<typeof readTableSort<AuditLogResponse>>[0],
      extra?: { action?: "paginate" | "sort" | "filter" },
    ) => {
      if (extra?.action === "sort") {
        const { field, order } = readTableSort(sorter);
        handleColumnSortChange(field, order);
        return;
      }
      handlePageChange(
        paginationParam.current ?? 1,
        paginationParam.pageSize ?? DEFAULT_PAGE_SIZE,
      );
    },
    [handleColumnSortChange, handlePageChange],
  );

  return (
    <PageContainer>
      <PageHeader
        title={t("audit.pageTitle")}
        subtitle={t("audit.pageSubtitle")}
      />

      {isError ? (
        <PageLoader isLoading={false} isError={true}>
          <span />
        </PageLoader>
      ) : (
        <SectionCard toolbar={toolbar} noPadding>
          <DataGrid<AuditLogResponse>
            data={dataSource}
            columns={columns}
            loading={isLoading}
            rowKey="id"
            onRowClick={handleRowClick}
            pagination={tablePagination}
            onChange={handleTableChange}
            scroll={{ x: 1000 }}
          />
        </SectionCard>
      )}

      <AuditDetailModal
        open={modalOpen}
        record={selectedRecord}
        onClose={handleCloseModal}
      />
    </PageContainer>
  );
});

AuditPageInner.displayName = "AuditPageInner";

// Garde de permission : redirige vers le dashboard si l'utilisateur n'a pas le droit AUDIT_VIEW.
const AuditPage = memo(() => {
  const { hasPermission } = useAuth();

  if (!hasPermission(PERMISSIONS.AUDIT.VIEW)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <AuditPageInner />;
});

AuditPage.displayName = "AuditPage";

export default AuditPage;
