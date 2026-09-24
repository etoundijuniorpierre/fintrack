// Liste generale interactive de tous les comptes utilisateurs.

import { memo, useMemo, useCallback, useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useSessionState } from "../../../hooks/ui/useSessionState/useSessionState";
import { useTableSort } from "../../../hooks/ui/useTableSort/useTableSort";
import { useTranslation } from "react-i18next";
import { usePaginatedUsers } from "../../../hooks/user/useUsers";
import { useOnlinePresence } from "../../../hooks/user/useOnlinePresence/useOnlinePresence";
import { useAuth } from "../../../hooks/auth/useAuth";
import { useRoles } from "../../../hooks/role/useRoles";
import { useAgencies } from "../../../hooks/agency/useAgencies";
import { useServices } from "../../../hooks/service/useServices";
import { userNavigation } from "../../../utils/navigation/users/users";
import {
  PERMISSIONS,
  hasAnyPermission,
} from "../../../utils/permissions/permissions";
import {
  DEFAULT_PAGE_SIZE,
  getPaginationConfig,
} from "../../../utils/table/pagination/paginationConfig";
import { buildTableToolbar } from "../../../utils/table/tableToolbar";
import { actionIcons } from "../../../utils/icons/appIcons";
import {
  ROLE_NAMES,
  normalizeRoleName,
  formatRoleName,
} from "../../../utils/roles/roles";
import { PageLoader } from "../../../components";
import { PageContainer, SectionCard } from "../../../components/Layout";
import {
  PageHeader,
  Button,
  TableSortControl,
  useTableFilters,
} from "../../../components/ui";
import type { TableFilterField } from "../../../components/ui";
import UserTable, {
  type UserTableFilterValues,
} from "../components/Table/UserTable";
import {
  USER_SORT_PRESETS,
  type UserSortPreset,
} from "../../../utils/user/sort/userSort";


// Rend le composant Users pour l'interface view liste utilisateurs.
const Users = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [searchText, setSearchText] = useSessionState("users_searchText", "");
  const [debouncedSearchText, setDebouncedSearchText] = useState("");
  const [searchParams] = useSearchParams();
  const initialStatus = searchParams.get("status") || undefined;
  const initialActive =
    initialStatus === "active"
      ? true
      : initialStatus === "inactive"
        ? false
        : undefined;
  // Redirection « comptes verrouilles » (vue Super Admin) : filtre dedie cote serveur.
  const lockedOnly = initialStatus === "LOCKED";
  // Liens qualite Super Admin : utilisateurs sans role / sans perimetre.
  const missingField = searchParams.get("missingField") || undefined;
  const [tableFilters, setTableFilters] =
    useSessionState<UserTableFilterValues>("users_tableFilters", {
      active: initialActive,
    });
  const { online } = useOnlinePresence();

  const [paginationState, setPaginationState] = useSessionState(
    "users_pagination",
    {
      current: 1,
      pageSize: DEFAULT_PAGE_SIZE,
    },
  );
  const {
    field: sortField,
    order: sortOrder,
    sortParam,
    activePreset: activeSortPreset,
    selectPreset,
    handleColumnSortChange,
  } = useTableSort<UserSortPreset>("users_sort", USER_SORT_PRESETS, "alphabetical", {
    onChange: () =>
      setPaginationState((current) => ({ ...current, current: 1 })),
  });

  useEffect(() => {
    // Traite la recherche apres saisie.
    const handler = setTimeout(() => {
      setDebouncedSearchText(searchText);
      setPaginationState((current) => ({ ...current, current: 1 }));
    }, 500);
    return () => clearTimeout(handler);
  }, [searchText, setDebouncedSearchText, setPaginationState]);

  // Traite le changement de page.
  const handlePageChange = useCallback(
    (page: number, pageSize: number) => {
      setPaginationState({ current: page, pageSize });
    },
    [setPaginationState],
  );

  const params = useMemo(
    () => ({
      page: paginationState.current - 1,
      size: paginationState.pageSize,
      ...(debouncedSearchText.trim()
        ? { keyword: debouncedSearchText.trim() }
        : {}),
      ...(tableFilters.role ? { role: tableFilters.role } : {}),
      ...(tableFilters.agencyId ? { agencyId: tableFilters.agencyId } : {}),
      ...(tableFilters.serviceId ? { serviceId: tableFilters.serviceId } : {}),
      ...(tableFilters.active !== undefined
        ? { active: tableFilters.active }
        : {}),
      ...(tableFilters.connected !== undefined
        ? { connected: tableFilters.connected }
        : {}),
      ...(lockedOnly ? { locked: true } : {}),
      ...(missingField === "role" || missingField === "scope"
        ? { missingField }
        : {}),
      sort: sortParam,
    }),
    [
      paginationState,
      debouncedSearchText,
      tableFilters,
      lockedOnly,
      missingField,
      sortParam,
    ],
  );

  const {
    data: usersData,
    isLoading,
    isError,
    error,
  } = usePaginatedUsers(
    params,
    tableFilters.connected === undefined ? undefined : online,
  );
  const shouldLoadTableCatalogs = !isLoading && !isError;
  const { data: roles } = useRoles(shouldLoadTableCatalogs);
  const { data: agencies } = useAgencies({ enabled: shouldLoadTableCatalogs });
  const { data: services } = useServices({ enabled: shouldLoadTableCatalogs });

  const canCreateUser = useMemo(
    () =>
      hasAnyPermission(hasPermission, [
        PERMISSIONS.USER.CREATE_ALL_AGENT,
        PERMISSIONS.USER.CREATE_AGENT_AGENCY,
        PERMISSIONS.USER.CREATE_AGENT_SERVICE,
        PERMISSIONS.USER.CREATE_AGENCY_MANAGER,
        PERMISSIONS.USER.CREATE_SERVICE_MANAGER,
        PERMISSIONS.USER.CREATE_ADMIN,
      ]),
    [hasPermission],
  );

  const allUsers = useMemo(() => usersData?.content ?? [], [usersData]);

  const total = usersData?.totalElements ?? 0;
  const { current: currentPage, pageSize: currentPageSize } = paginationState;
  const tablePagination = useMemo(
    () =>
      getPaginationConfig({
        current: currentPage,
        pageSize: currentPageSize,
        total,
        onChange: handlePageChange,
        onShowSizeChange: handlePageChange,
      }),
    [currentPage, currentPageSize, total, handlePageChange],
  );

  // Traite la creation d'utilisateur.
  const handleAddUser = useCallback(
    () => userNavigation.navigateToUserCreate(navigate),
    [navigate],
  );

  // Filtres role/agence/service reserves aux administrateurs (vue transverse).
  const canSeeScopeFilters = hasPermission(PERMISSIONS.USER.VIEW_ALL);

  const roleOptions = useMemo(
    () =>
      (roles ?? [])
        .filter((r) => normalizeRoleName(r.name) !== ROLE_NAMES.SUPER_ADMIN)
        .map((r) => ({ label: formatRoleName(r, t), value: r.name })),
    [roles, t],
  );
  const agencyOptions = useMemo(
    () => (agencies ?? []).map((a) => ({ label: a.name, value: a.id })),
    [agencies],
  );
  const serviceOptions = useMemo(
    () => (services ?? []).map((s) => ({ label: s.name, value: s.id })),
    [services],
  );

  const setFilter = useCallback(
    (patch: UserTableFilterValues) => {
      setTableFilters((prev) => ({ ...prev, ...patch }));
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [setTableFilters, setPaginationState],
  );

  const filterFields = useMemo<TableFilterField[]>(() => {
    const fields: TableFilterField[] = [];
    if (canSeeScopeFilters) {
      fields.push(
        {
          key: "role",
          type: "select",
          label: t("users.table.roles"),
          options: roleOptions,
          value: tableFilters.role,
          onChange: (value) => setFilter({ role: value }),
        },
        {
          key: "agency",
          type: "select",
          label: t("users.table.agency"),
          options: agencyOptions,
          value: tableFilters.agencyId,
          onChange: (value) => setFilter({ agencyId: value }),
        },
        {
          key: "service",
          type: "select",
          label: t("users.table.service"),
          options: serviceOptions,
          value: tableFilters.serviceId,
          onChange: (value) => setFilter({ serviceId: value }),
        },
      );
    }
    fields.push({
      key: "status",
      type: "select",
      label: t("users.table.status"),
      options: [
        { label: t("users.table.status_active"), value: "true" },
        { label: t("users.table.status_inactive"), value: "false" },
      ],
      value:
        tableFilters.active === undefined
          ? undefined
          : tableFilters.active
            ? "true"
            : "false",
      onChange: (value) =>
        setFilter({
          active: value === undefined ? undefined : value === "true",
        }),
    });
    fields.push({
      key: "presence",
      type: "select",
      label: t("users.table.presence"),
      options: [
        { label: t("users.table.connected"), value: "true" },
        { label: t("users.table.disconnected"), value: "false" },
      ],
      value:
        tableFilters.connected === undefined
          ? undefined
          : tableFilters.connected
            ? "true"
            : "false",
      onChange: (value) =>
        setFilter({
          connected: value === undefined ? undefined : value === "true",
        }),
    });
    return fields;
  }, [
    canSeeScopeFilters,
    t,
    roleOptions,
    agencyOptions,
    serviceOptions,
    tableFilters,
    setFilter,
  ]);

  const { trigger: filtersTrigger, chips: filtersChips } =
    useTableFilters(filterFields);

  const sortControl = (
    <TableSortControl<UserSortPreset>
      presets={USER_SORT_PRESETS}
      labels={{
        alphabetical: t("users.sort.alphabetical"),
        seniority: t("users.sort.seniority"),
      }}
      label={t("users.sort.label")}
      activePreset={activeSortPreset}
      onSelect={selectPreset}
    />
  );

  const toolbar = buildTableToolbar({
    searchValue: searchText,
    onSearch: (e) => setSearchText(e.target.value),
    searchPlaceholder: t("users.search_placeholder"),
    leading: sortControl,
    extra: filtersTrigger,
    activeFilters: filtersChips,
    addButton: canCreateUser ? (
      <Button variant="primary" icon={actionIcons.add} onClick={handleAddUser}>
        {t("users.add_button")}
      </Button>
    ) : undefined,
  });

  return (
    <PageContainer>
      <PageHeader title={t("users.title")} subtitle={t("users.subtitle")} />

      {isError ? (
        <PageLoader
          isLoading={false}
          isError={true}
          errorMessage={
            error instanceof Error ? error.message : t("common.unknown_error")
          }
        >
          <span />
        </PageLoader>
      ) : (
        <SectionCard toolbar={toolbar} noPadding>
          <UserTable
            users={allUsers}
            loading={isLoading}
            pagination={tablePagination}
            onPageChange={handlePageChange}
            sort={{ field: sortField, order: sortOrder }}
            onSortChange={handleColumnSortChange}
          />
        </SectionCard>
      )}
    </PageContainer>
  );
});

Users.displayName = "Users";

export default Users;
