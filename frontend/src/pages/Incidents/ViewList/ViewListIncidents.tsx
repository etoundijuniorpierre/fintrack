// Page liste des incidents : affichage tableau ou cartes, filtre par portee, recherche et permissions.
import { memo, useCallback, useEffect, useMemo, useState } from "react";
import {
  useNavigate,
  Navigate,
  useSearchParams,
  useLocation,
} from "react-router-dom";
import { useSessionState } from "../../../hooks/ui/useSessionState/useSessionState";
import { useTableSort } from "../../../hooks/ui/useTableSort/useTableSort";
import {
  INCIDENT_SORT_PRESETS,
  type IncidentSortPreset,
} from "../../../utils/incident/sort/incidentSort";
import { useTranslation } from "react-i18next";
import dayjs, { type Dayjs } from "dayjs";
import { DATE_FORMATS } from "../../../utils/date/dateUtils";
import { Segmented, Select, Tag } from "antd";
import { PageLoader } from "../../../components";
import { PageContainer, SectionCard } from "../../../components/Layout";
import {
  PageHeader,
  Button,
  TableSortControl,
  useTableFilters,
} from "../../../components/ui";
import type { TableFilterField } from "../../../components/ui";
import { buildTableToolbar } from "../../../utils/table/tableToolbar";
import IncidentTable from "../components/Table/IncidentTable";
import IncidentCards from "../components/IncidentCards/IncidentCards";
import {
  useIncidents,
  useIncidentStatuses,
  useCriticalities,
  useIncidentTypes,
} from "../../../hooks/incident/useIncidents/useIncidents";
import { useAgencies } from "../../../hooks/agency/useAgencies";
import { useServices } from "../../../hooks/service/useServices";
import { usePaginatedUsers } from "../../../hooks/user/useUsers/useUsers";
import { IncidentStatus } from "../../../api/incident/types";
import { useAuth } from "../../../hooks/auth/useAuth";
import { incidentNavigation } from "../../../utils/navigation/incidents/incidents";
import { APP_ROUTES, STORAGE_KEYS } from "../../../utils/constants";
import {
  DEFAULT_PAGE_SIZE,
  getPaginationConfig,
} from "../../../utils/table/pagination/paginationConfig";
import { displayModeIcons, actionIcons } from "../../../utils/icons/appIcons";
import {
  INCIDENT_VIEW_PERMISSIONS,
  PERMISSIONS,
  getIncidentViewOptions,
  getPreferredView,
  hasAnyPermission,
  type ScopeView,
} from "../../../utils/permissions/permissions";
import { usePersistentState } from "../../../hooks/ui/usePersistentState/usePersistentState";
import styles from "./ViewListIncidents.module.scss";

// Type les valeurs incident display mode utilisees par l'interface.
type IncidentDisplayMode = "table" | "cards";

const DISPLAY_MODE_OPTIONS: readonly IncidentDisplayMode[] = ["table", "cards"];

// Portee par defaut de la page incidents : on privilegie la portee du role
const INCIDENT_VIEW_FALLBACK_ORDER: readonly ScopeView[] = [
  "all",
  "agency",
  "byAgency",
  "service",
  "byService",
  "own",
];
// Statuts de sortie du circuit, masquables separement : clore, rejeter et annuler
// sont trois decisions distinctes, et on ne les consulte pas ensemble.
const HIDEABLE_STATUSES = [
  IncidentStatus.CLOSED,
  IncidentStatus.REJECTED,
  IncidentStatus.CANCELLED,
] as const;

const ALL_INCIDENT_STATUSES = Object.values(IncidentStatus);

// Etapes de passage du parcours nominal, omises du selecteur de statuts.
const TRANSIT_STATUSES: ReadonlySet<string> = new Set([
  IncidentStatus.OPEN,
  IncidentStatus.VALIDATED,
  IncidentStatus.TRANSFERRED,
]);

// Rend le composant ViewListIncidents pour l'interface view liste incidents.
const ViewListIncidents = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasPermission, user } = useAuth();

  const canCreate = hasPermission(PERMISSIONS.INCIDENT.CREATE);
  // Le filtre transverse suit la permission de consultation globale.
  const canFilterUnassigned = hasPermission(PERMISSIONS.INCIDENT.VIEW_ALL);
  // Filtrer par agence n'a de sens que pour qui en voit plusieurs : la permission
  // transverse (SUPER_ADMIN/ADMIN). Un profil scope agence ne voit deja qu'elle.
  const canFilterByAgency = hasPermission(PERMISSIONS.INCIDENT.VIEW_ALL);
  // Le filtre par utilisateur
  const canFilterByUser = hasAnyPermission(hasPermission, [
    PERMISSIONS.USER.VIEW_ALL,
    PERMISSIONS.USER.VIEW_AGENCY,
    PERMISSIONS.USER.VIEW_SERVICE,
  ]);

  const hasViewPermission = hasAnyPermission(hasPermission, [
    ...INCIDENT_VIEW_PERMISSIONS,
    PERMISSIONS.INCIDENT.TREAT,
    PERMISSIONS.INCIDENT.RESOLVE,
  ]);

  const membership = useMemo(
    () => ({
      hasAgency: Boolean(user?.agencyId) || Boolean(user?.managedAgencyId),
      hasService:
        Boolean(user?.serviceId) ||
        Boolean(user?.managedServiceIds && user.managedServiceIds.length > 0),
    }),
    [
      user?.agencyId,
      user?.managedAgencyId,
      user?.serviceId,
      user?.managedServiceIds,
    ],
  );

  const availableViews = useMemo(
    () => getIncidentViewOptions(hasPermission, t, membership),
    [hasPermission, t, membership],
  );

  const [view, setView] = useSessionState<ScopeView>("incidents_view", () =>
    getPreferredView(availableViews, null, INCIDENT_VIEW_FALLBACK_ORDER),
  );

  // Une portee memorisee peut devenir invalide
  useEffect(() => {
    if (
      availableViews.length > 0 &&
      !availableViews.some((option) => option.value === view)
    ) {
      setView(getPreferredView(availableViews, null, INCIDENT_VIEW_FALLBACK_ORDER));
    }
  }, [availableViews, view, setView]);

  const [displayMode, setDisplayMode] = usePersistentState<IncidentDisplayMode>(
    STORAGE_KEYS.INCIDENT_DISPLAY_MODE,
    "table",
    DISPLAY_MODE_OPTIONS,
  );

  // Agence / service choisis pour les portees "Par agence" / "Par service".
  const [scopeAgencyId, setScopeAgencyId] = useSessionState<string | undefined>(
    "incidents_scopeAgencyId",
    undefined,
  );
  const [scopeServiceId, setScopeServiceId] = useSessionState<
    string | undefined
  >("incidents_scopeServiceId", undefined);

  const { data: agencies = [] } = useAgencies({
    enabled: view === "byAgency" || canFilterByAgency,
  });
  const { data: services = [] } = useServices({
    enabled: view === "byService",
  });

  // Les portees "Par agence/service" passent par la vue globale + filtre explicite.
  const effectiveView = useMemo(
    () => (view === "byAgency" || view === "byService" ? "all" : view),
    [view],
  );
  const effectiveAgencyId = view === "byAgency" ? scopeAgencyId : undefined;
  const effectiveServiceId = view === "byService" ? scopeServiceId : undefined;

  const [searchParams] = useSearchParams();
  const initialStatuses = searchParams.get("status")?.split(",") || [];
  const initialCriticalities =
    searchParams.get("criticality")?.split(",") || [];
  // Pre-filtre par type : permet le lien "voir les incidents de ce type" (base de connaissance).
  const initialTypes = searchParams.get("type")?.split(",").filter(Boolean) || [];
  const initialAssignedTo = searchParams.get("assignedTo") || undefined;
  // Perimetre "incidents d'un utilisateur" (lien depuis la fiche utilisateur).
  const initialCreatedBy = searchParams.get("createdBy") || undefined;
  // Lien qualite Super Admin : incidents sans agence / sans service.
  const initialMissingField = searchParams.get("missingField") || undefined;
  // Lien qualite Super Admin : incidents assignes a un utilisateur inactif.
  const assignedToInactiveOnly =
    searchParams.get("assignedToInactive") === "true";

  const [searchText, setSearchText] = useSessionState(
    "incidents_searchText",
    "",
  );
  const [debouncedSearchText, setDebouncedSearchText] = useState("");
  const [selectedStatuses, setSelectedStatuses] = useSessionState<string[]>(
    "incidents_selectedStatuses",
    initialStatuses,
  );
  const [selectedCriticalities, setSelectedCriticalities] = useSessionState<
    string[]
  >("incidents_selectedCriticalities", initialCriticalities);
  const [selectedTypes, setSelectedTypes] = useSessionState<string[]>(
    "incidents_selectedTypes",
    initialTypes,
  );
  const [unassignedOnly, setUnassignedOnly] = useSessionState<boolean>(
    "incidents_unassignedOnly",
    canFilterUnassigned && initialAssignedTo === "UNASSIGNED",
  );
  const [hiddenStatuses, setHiddenStatuses] = useSessionState<string[]>(
    "incidents_hiddenStatuses",
    // Les cloturees encombrent la liste de travail sans jamais rien appeler ;
    // rejets et annulations sont assez rares pour rester visibles.
    initialStatuses.length === 0 ? [IncidentStatus.CLOSED] : [],
  );
  const [selectedPeriod, setSelectedPeriod] = useSessionState<
    [string | null, string | null] | null
  >("incidents_selectedPeriod", null);
  const periodValue = useMemo<[Dayjs | null, Dayjs | null] | null>(
    () =>
      selectedPeriod
        ? [
            selectedPeriod[0] ? dayjs(selectedPeriod[0]) : null,
            selectedPeriod[1] ? dayjs(selectedPeriod[1]) : null,
          ]
        : null,
    [selectedPeriod],
  );

  const [selectedAgencyId, setSelectedAgencyId] = useSessionState<
    string | undefined
  >("incidents_selectedAgency", undefined);
  // La vue "Par agence" porte deja l'agence : le filtre s'efface pour ne pas
  // presenter deux selecteurs qui se contredisent.
  const filterAgencyId =
    canFilterByAgency && view !== "byAgency" ? selectedAgencyId : undefined;
  const [selectedSubjectUserId, setSelectedSubjectUserId] = useSessionState<
    string | undefined
  >("incidents_selectedSubjectUser", undefined);
  const effectiveSubjectUserId =
    selectedSubjectUserId === user?.id ? undefined : selectedSubjectUserId;
  const { data: subjectUsersPage } = usePaginatedUsers(
    { size: 200, active: true },
    undefined,
    { enabled: canFilterByUser },
  );
  const subjectUserOptions = useMemo(
    () =>
      [...(subjectUsersPage?.content ?? [])]
        .filter((subjectUser) => subjectUser.id !== user?.id)
        .sort((a, b) =>
          `${a.firstName} ${a.lastName}`.localeCompare(
            `${b.firstName} ${b.lastName}`,
          ),
        )
        .map((u) => ({
          label: `${u.firstName} ${u.lastName} (${u.username})`,
          value: u.id,
        })),
    [subjectUsersPage, user?.id],
  );

  const agencyFilterOptions = useMemo(
    () =>
      [...agencies]
        .sort((a, b) => a.name.localeCompare(b.name))
        .map((agency) => ({ label: agency.name, value: agency.id })),
    [agencies],
  );

  const [paginationState, setPaginationState] = useSessionState(
    "incidents_pagination",
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
  } = useTableSort<IncidentSortPreset>(
    "incidents_sort",
    INCIDENT_SORT_PRESETS,
    "seniority",
    {
      onChange: () =>
        setPaginationState((current) => ({ ...current, current: 1 })),
    },
  );

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

  const { data: statuses = [] } = useIncidentStatuses();
  const { data: criticalities = [] } = useCriticalities();
  const { data: incidentTypes = [] } = useIncidentTypes();

  const effectiveStatuses = useMemo(() => {
    if (selectedStatuses.length > 0) return selectedStatuses;
    if (hiddenStatuses.length === 0) return [];
    // Le serveur attend la liste de ce qu'on veut voir : on retranche les masques.
    return ALL_INCIDENT_STATUSES.filter(
      (status) => !hiddenStatuses.includes(status),
    );
  }, [selectedStatuses, hiddenStatuses]);

  const params = useMemo(
    () => ({
      view: effectiveView,
      ...(effectiveAgencyId ?? filterAgencyId
        ? { agencyId: effectiveAgencyId ?? filterAgencyId }
        : {}),
      ...(effectiveServiceId ? { serviceId: effectiveServiceId } : {}),
      page: paginationState.current - 1,
      size: paginationState.pageSize,
      ...(debouncedSearchText.trim()
        ? { keyword: debouncedSearchText.trim() }
        : {}),
      ...(effectiveStatuses.length > 0
        ? { status: effectiveStatuses.join(",") }
        : {}),
      ...(selectedCriticalities.length > 0
        ? { criticality: selectedCriticalities.join(",") }
        : {}),
      ...(selectedTypes.length > 0 ? { type: selectedTypes.join(",") } : {}),
      ...(canFilterUnassigned && unassignedOnly
        ? { unassignedOnly: true }
        : initialAssignedTo && initialAssignedTo !== "UNASSIGNED"
          ? { assignedTo: initialAssignedTo }
          : {}),
      ...(initialCreatedBy ? { createdBy: initialCreatedBy } : {}),
      ...(effectiveSubjectUserId
        ? { subjectUserId: effectiveSubjectUserId }
        : {}),
      ...(selectedPeriod?.[0] ? { startDate: selectedPeriod[0] } : {}),
      ...(selectedPeriod?.[1] ? { endDate: selectedPeriod[1] } : {}),
      ...(initialMissingField === "agency" || initialMissingField === "service"
        ? { missingField: initialMissingField }
        : {}),
      ...(assignedToInactiveOnly ? { assignedToInactive: true } : {}),
      sort: sortParam,
    }),
    [
      effectiveView,
      effectiveAgencyId,
      filterAgencyId,
      effectiveServiceId,
      paginationState,
      debouncedSearchText,
      effectiveStatuses,
      selectedCriticalities,
      selectedTypes,
      initialAssignedTo,
      initialCreatedBy,
      effectiveSubjectUserId,
      selectedPeriod,
      canFilterUnassigned,
      unassignedOnly,
      initialMissingField,
      assignedToInactiveOnly,
      sortParam,
    ],
  );

  const { data, isLoading, isError, error } = useIncidents(params);

  const allIncidents = useMemo(() => data?.content ?? [], [data]);

  // Traite le changement de statut.

  const handleHiddenStatusChange = useCallback(
    (status: string, hidden: boolean) => {
      setHiddenStatuses((prev) =>
        hidden
          ? prev.includes(status)
            ? prev
            : [...prev, status]
          : prev.filter((current) => current !== status),
      );
      if (hidden) {
        setSelectedStatuses((prev) =>
          prev.filter((current) => current !== status),
        );
      }
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [setHiddenStatuses, setSelectedStatuses, setPaginationState],
  );

  // Filtre par statut : selectionner un statut clos/rejete leve "Masquer cloturés/rejetés".
  const handleStatusesChange = useCallback(
    (next: string[]) => {
      setSelectedStatuses(next);
      // Demander un statut et le masquer se contredit : la demande l'emporte.
      setHiddenStatuses((prev) =>
        prev.filter((status) => !next.includes(status)),
      );
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [setSelectedStatuses, setHiddenStatuses, setPaginationState],
  );

  const handleCriticalitiesChange = useCallback(
    (next: string[]) => {
      setSelectedCriticalities(next);
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [setSelectedCriticalities, setPaginationState],
  );

  const handleTypesChange = useCallback(
    (next: string[]) => {
      setSelectedTypes(next);
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [setSelectedTypes, setPaginationState],
  );

  // Traite le filtre des incidents non assignes.
  const handleUnassignedChange = useCallback(
    (checked: boolean) => {
      setUnassignedOnly(checked);
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [setUnassignedOnly, setPaginationState],
  );

  const handlePeriodChange = useCallback(
    (value: [Dayjs | null, Dayjs | null] | null) => {
      setSelectedPeriod(
        value
          ? [
              value[0] ? value[0].format(DATE_FORMATS.API) : null,
              value[1] ? value[1].format(DATE_FORMATS.API) : null,
            ]
          : null,
      );
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [setSelectedPeriod, setPaginationState],
  );

  const handleAgencyFilterChange = useCallback(
    (value?: string) => {
      setSelectedAgencyId(value);
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [setSelectedAgencyId, setPaginationState],
  );

  const handleSubjectUserChange = useCallback(
    (value?: string) => {
      setSelectedSubjectUserId(value);
      setPaginationState((current) => ({ ...current, current: 1 }));
    },
    [setSelectedSubjectUserId, setPaginationState],
  );

  const total = data?.totalElements ?? 0;
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

  // Traite l'action utilisateur liee a add inincident.
  const handleAddIncident = useCallback(
    () => incidentNavigation.navigateToIncidentCreate(navigate),
    [navigate],
  );

  const statusOptions = useMemo(
    () =>
      statuses
        .filter((s) => !TRANSIT_STATUSES.has(s.code))
        .map((s) => ({
          label: t(`incidents.status.${s.code}`, s.name),
          value: s.code,
        })),
    [statuses, t],
  );
  const criticalityOptions = useMemo(
    () =>
      criticalities.map((c) => ({
        label: t(`incidents.criticality.${c.code}`, c.name),
        value: c.code,
      })),
    [criticalities, t],
  );
  const typeOptions = useMemo(
    () =>
      incidentTypes.map((type) => ({
        label: type.displayName,
        value: type.id,
      })),
    [incidentTypes],
  );

  // Filtres regroupes dans un popover unique (coherence + barre allegee).
  const filterFields = useMemo<TableFilterField[]>(() => {
    const fields: TableFilterField[] = [
      {
        key: "type",
        type: "multiselect",
        label: t("incidents.controls.type"),
        options: typeOptions,
        value: selectedTypes,
        onChange: handleTypesChange,
        placeholder: t("incidents.controls.typePlaceholder"),
      },
      {
        key: "status",
        type: "multiselect",
        label: t("incidents.controls.status"),
        options: statusOptions,
        value: selectedStatuses,
        onChange: handleStatusesChange,
        placeholder: t("incidents.controls.statusPlaceholder"),
      },
      {
        key: "criticality",
        type: "multiselect",
        label: t("incidents.controls.criticality"),
        options: criticalityOptions,
        value: selectedCriticalities,
        onChange: handleCriticalitiesChange,
      },
    ];
    fields.push({
      key: "period",
      type: "daterange",
      label: effectiveSubjectUserId
        ? t("incidents.controls.periodActivity")
        : t("incidents.controls.periodDeclared"),
      value: periodValue,
      onChange: handlePeriodChange,
    });
    if (canFilterByAgency && view !== "byAgency") {
      fields.push({
        key: "agency",
        type: "select",
        label: t("incidents.controls.agencyFilter"),
        options: agencyFilterOptions,
        value: filterAgencyId,
        onChange: handleAgencyFilterChange,
        placeholder: t("incidents.controls.agencyFilterPlaceholder"),
      });
    }
    if (canFilterByUser) {
      fields.push({
        key: "subjectUser",
        type: "select",
        label: t("incidents.controls.userFilter"),
        options: subjectUserOptions,
        value: effectiveSubjectUserId,
        onChange: handleSubjectUserChange,
        placeholder: t("incidents.controls.userFilterPlaceholder"),
      });
    }
    if (canFilterUnassigned) {
      fields.push({
        key: "unassigned",
        type: "toggle",
        label: t("incidents.controls.unassignedOnly"),
        value: unassignedOnly,
        onChange: handleUnassignedChange,
      });
    }
    HIDEABLE_STATUSES.forEach((status) => {
      fields.push({
        key: `hide${status}`,
        type: "toggle",
        label: t(`incidents.controls.hide.${status}`),
        value: hiddenStatuses.includes(status),
        onChange: (checked: boolean) =>
          handleHiddenStatusChange(status, checked),
      });
    });
    return fields;
  }, [
    t,
    typeOptions,
    selectedTypes,
    handleTypesChange,
    statusOptions,
    selectedStatuses,
    handleStatusesChange,
    criticalityOptions,
    selectedCriticalities,
    handleCriticalitiesChange,
    periodValue,
    handlePeriodChange,
    canFilterByAgency,
    view,
    agencyFilterOptions,
    filterAgencyId,
    handleAgencyFilterChange,
    canFilterByUser,
    subjectUserOptions,
    effectiveSubjectUserId,
    handleSubjectUserChange,
    canFilterUnassigned,
    unassignedOnly,
    handleUnassignedChange,
    hiddenStatuses,
    handleHiddenStatusChange,
  ]);

  const { trigger: filtersTrigger, chips: filtersChips } =
    useTableFilters(filterFields);

  // Perimetre "incidents d'un utilisateur" : puce retirable. Le libelle voyage
  // dans l'etat de navigation (jamais dans l'URL : donnee personnelle).
  const userScope = useMemo(() => {
    const navState = location.state as {
      incidentUserLabel?: string;
    } | null;
    if (initialCreatedBy) {
      return { mode: "createdBy" as const, label: navState?.incidentUserLabel };
    }
    if (initialAssignedTo && initialAssignedTo !== "UNASSIGNED") {
      return { mode: "assignedTo" as const, label: navState?.incidentUserLabel };
    }
    return null;
  }, [initialCreatedBy, initialAssignedTo, location.state]);

  const handleClearUserScope = useCallback(
    () => navigate(APP_ROUTES.INCIDENTS),
    [navigate],
  );

  const userScopeChip = userScope ? (
    <Tag closable onClose={handleClearUserScope}>
      {t(
        userScope.mode === "createdBy"
          ? "incidents.controls.createdByLabel"
          : "incidents.controls.assignedToLabel",
      )}
      {userScope.label ? ` : ${userScope.label}` : ""}
    </Tag>
  ) : null;

  const activeFilters =
    filtersChips || userScopeChip ? (
      <>
        {userScopeChip}
        {filtersChips}
      </>
    ) : undefined;

  const toolbar = buildTableToolbar({
    searchValue: searchText,
    onSearch: (e) => setSearchText(e.target.value),
    searchPlaceholder: t("incidents.search_placeholder"),
    leading: (
      <TableSortControl<IncidentSortPreset>
        presets={INCIDENT_SORT_PRESETS}
        labels={{
          alphabetical: t("incidents.sort.alphabetical"),
          seniority: t("incidents.sort.seniority"),
        }}
        label={t("incidents.sort.label")}
        activePreset={activeSortPreset}
        onSelect={selectPreset}
      />
    ),
    extra: (
      <div className={styles.toolbarControls}>
        <Select
          value={view}
          onChange={setView}
          options={availableViews}
          className={styles.viewSelect}
          aria-label={t("incidents.controls.view")}
        />
        {view === "byAgency" && (
          <Select
            value={scopeAgencyId}
            onChange={(value) => {
              setScopeAgencyId(value);
              setPaginationState((current) => ({ ...current, current: 1 }));
            }}
            options={agencies.map((agency) => ({
              label: agency.name,
              value: agency.id,
            }))}
            placeholder={t("incidents.controls.selectAgency")}
            allowClear
            showSearch
            optionFilterProp="label"
            className={styles.viewSelect}
            aria-label={t("incidents.controls.selectAgency")}
          />
        )}
        {view === "byService" && (
          <Select
            value={scopeServiceId}
            onChange={(value) => {
              setScopeServiceId(value);
              setPaginationState((current) => ({ ...current, current: 1 }));
            }}
            options={services.map((service) => ({
              label: service.name,
              value: service.id,
            }))}
            placeholder={t("incidents.controls.selectService")}
            allowClear
            showSearch
            optionFilterProp="label"
            className={styles.viewSelect}
            aria-label={t("incidents.controls.selectService")}
          />
        )}
        {filtersTrigger}
        <Segmented
          value={displayMode}
          onChange={(v) => setDisplayMode(v as "table" | "cards")}
          options={[
            {
              value: "table",
              icon: displayModeIcons.table,
              label: t("incidents.controls.table"),
            },
            {
              value: "cards",
              icon: displayModeIcons.cards,
              label: t("incidents.controls.cards"),
            },
          ]}
        />
      </div>
    ),
    activeFilters,
    addButton: canCreate ? (
      <Button
        variant="primary"
        icon={actionIcons.add}
        onClick={handleAddIncident}
      >
        {t("incidents.add_button")}
      </Button>
    ) : undefined,
  });

  if (!hasViewPermission) {
    return <Navigate to={APP_ROUTES.DASHBOARD} replace />;
  }

  return (
    <PageContainer>
      <PageHeader
        title={t("incidents.title")}
        subtitle={t("incidents.subtitle")}
      />

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
        <SectionCard toolbar={toolbar} noPadding={displayMode === "table"}>
          {displayMode === "table" ? (
            <IncidentTable
              incidents={allIncidents}
              loading={isLoading}
              pagination={tablePagination}
              onPageChange={handlePageChange}
              sort={{ field: sortField, order: sortOrder }}
              onSortChange={handleColumnSortChange}
            />
          ) : (
            <IncidentCards incidents={allIncidents} loading={isLoading} />
          )}
        </SectionCard>
      )}
    </PageContainer>
  );
});

ViewListIncidents.displayName = "ViewListIncidents";

export default ViewListIncidents;
