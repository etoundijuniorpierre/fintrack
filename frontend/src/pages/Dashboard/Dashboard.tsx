// Page Dashboard : affiche les KPI, graphiques et activites recentes selon les permissions et la configuration utilisateur.
import { Col, Empty, Row, Segmented, Select, Skeleton } from "antd";
import { Suspense, lazy, memo, useCallback, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { PageContainer, SectionCard } from "../../components/Layout";
import { IconOnlyButton, IconOnlyLabel, PageHeader } from "../../components/ui";
import { useAuth } from "../../hooks/auth/useAuth";
import { useAgencies } from "../../hooks/agency/useAgencies";
import { useServices } from "../../hooks/service/useServices";
import { useDashboardMetrics } from "../../hooks/dashboard/useDashboardMetrics/useDashboardMetrics";
import { usePersistentState } from "../../hooks/ui/usePersistentState/usePersistentState";
import { useUsers } from "../../hooks/user/useUsers/useUsers";
import {
  type CohortCompletion,
  type DashboardConfig,
  type DashboardMetricsResponse,
  type PeriodFilter,
  WidgetType,
  PeriodType,
} from "../../types/dashboard";
import { APP_ROUTES, STORAGE_KEYS } from "../../utils/constants";
import { loadDashboardConfig } from "../../utils/dashboard/config/dashboardConfig";
import {
  getVisibleWidgets,
  isWidgetAllowedInView,
} from "../../utils/dashboard/widgetPermissions/widgetPermissions";
import {
  formatDurationLong,
  formatDate,
} from "../../utils/formatters/formatters";
import {
  actionIcons,
  dashboardKpiIcons,
  displayModeIcons,
} from "../../utils/icons/appIcons";
import {
  PERMISSIONS,
  getDashboardViewOptions,
  hasPermissionName,
  type ScopeView,
} from "../../utils/permissions/permissions";
import { usePersistentScopeView } from "../../hooks/ui/usePersistentScopeView/usePersistentScopeView";
import DashboardConfigPanel from "./components/DashboardConfigPanel/DashboardConfigPanel";
import KpiCard from "./components/KpiCard";
import { SectionTitle } from "../../components/ui";
import type { KpiCardProps } from "./components/KpiCard";
import RecentIncidentsList from "./components/RecentIncidentsList/RecentIncidentsList";
import type { TFunction } from "i18next";
import PeriodSelector from "./components/PeriodSelector/PeriodSelector";
import styles from "./Dashboard.module.scss";

// Type les valeurs dashboard display mode utilisees par l'interface.
type DashboardDisplayMode = "stats" | "graphs";

// Modele la structure kpi definition manipulee par le frontend.
interface KpiDefinition {
  widget: WidgetType;
  title: string;
  value: number | string;
  icon: ReactNode;
  tone: KpiCardProps["tone"];
  subtitle?: string;
  details?: string[];
  /** Aide enrichie des chiffres secondaires ; a defaut, le texte statique du widget. */
  help?: string;
  isLoading?: boolean;
}

// Garde-fou doctrine : un taux n'est affiche que si son effectif (base a risque) atteint le seuil minimal.
const MIN_RATE_SAMPLE = 5;

const DISPLAY_MODE_OPTIONS: readonly DashboardDisplayMode[] = [
  "stats",
  "graphs",
];
const DASHBOARD_VIEW_FALLBACK_ORDER: readonly ScopeView[] = [
  "all",
  "agency",
  "service",
  "own",
];
const DashboardGraphs = lazy(
  () => import("./components/DashboardGraphs/DashboardGraphs"),
);
const CURRENT_KPI_WIDGETS = new Set<WidgetType>([
  WidgetType.INCIDENT_ACTIVE,
  WidgetType.INCIDENT_BLOCKED,
  WidgetType.INCIDENT_ASSIGNED_TO_ME,
  WidgetType.INCIDENT_SLA_BREACH_NOW,
]);

// Restaure la periode propre a l'utilisateur avec un repli sur les 30 derniers jours.
const loadPeriodFilter = (userId?: string): PeriodFilter => {
  if (!userId) return { period: PeriodType.LAST_30_DAYS };
  try {
    const stored = localStorage.getItem(
      `${STORAGE_KEYS.DASHBOARD_PERIOD}:${userId}`,
    );
    if (!stored) return { period: PeriodType.LAST_30_DAYS };
    const parsed = JSON.parse(stored) as PeriodFilter;
    if (!Object.values(PeriodType).includes(parsed.period)) {
      return { period: PeriodType.LAST_30_DAYS };
    }
    if (
      parsed.period === PeriodType.CUSTOM &&
      (!parsed.dateFrom || !parsed.dateTo)
    ) {
      return { period: PeriodType.LAST_30_DAYS };
    }
    return parsed;
  } catch {
    return { period: PeriodType.LAST_30_DAYS };
  }
};

// Deux absences a ne pas confondre : une population vide (personne n'etait concerne)
// et une mesure impossible (trop peu de dossiers termines pour conclure).
const EMPTY_VALUE = "\u2014";

// Une duree mise en avant ne s'invente pas : sans dossier mesure, on le dit.
const durationKpi = (
  hours: number | undefined,
  sampleSize: number,
  t: TFunction,
): Pick<KpiDefinition, "value" | "subtitle"> =>
  sampleSize > 0
    ? {
        value: formatDurationLong(hours, t),
        subtitle: t("dashboard.kpi.measured_on", { count: sampleSize }),
      }
    : { value: EMPTY_VALUE, subtitle: t("dashboard.kpi.not_enough_data") };

// Trois cartes suivent la meme forme : le compte concret en tete (« 16 » sur « 20 »),
// que le lecteur n'a plus a reconstituer, et le pourcentage relegue dans l'aide,
// affiche seulement quand la base est assez fournie pour vouloir dire quelque chose.
const rateKpi = (
  count: number,
  base: number,
  baseLabel: string,
  rate: number,
  help: string,
  t: TFunction,
): Pick<KpiDefinition, "value" | "subtitle" | "help"> => ({
  value: base === 0 ? EMPTY_VALUE : count,
  subtitle: base === 0 ? t("dashboard.kpi.no_incident_concerned") : baseLabel,
  help:
    base >= MIN_RATE_SAMPLE
      ? `${help} ${t("dashboard.kpi.rate_detail", { rate: rate.toFixed(1) })}`
      : help,
});

// Le detail du flux enumere chaque mouvement separement : « sorties » ne disait pas
// quelle action avait eu lieu, et le solde affiche en tete est exactement la somme
// de ces lignes (declares + rouverts - clotures - refuses - annules).
const flowDetail = (
  metrics: DashboardMetricsResponse | undefined,
  t: TFunction,
): string => {
  const reopened = metrics?.backlogReEntries ?? 0;
  return [
    t("dashboard.kpi.flow_declared", { count: metrics?.inflow ?? 0 }),
    ...(reopened > 0
      ? [t("dashboard.kpi.flow_reopened", { count: reopened })]
      : []),
    t("dashboard.kpi.flow_closed", { count: metrics?.exitsClosed ?? 0 }),
    t("dashboard.kpi.flow_rejected", { count: metrics?.exitsRejected ?? 0 }),
    t("dashboard.kpi.flow_cancelled", { count: metrics?.exitsCancelled ?? 0 }),
  ].join(" \u00b7 ");
};

// Le sens du mouvement se dit en toutes lettres : « solde net » demandait de savoir
// lire un signe pour comprendre que la pile grossit.
const flowSubtitle = (net: number, t: TFunction): string => {
  if (net > 0) return t("dashboard.kpi.flow_up", { count: net });
  if (net < 0) return t("dashboard.kpi.flow_down", { count: -net });
  return t("dashboard.kpi.flow_flat");
};

// La cohorte annonce son avancement (« 12 clotures sur 20 »), pas une duree : un
// percentile que la cohorte n'a pas atteint n'existe pas encore et ne peut pas etre
// remplace par celui des seuls dossiers termines. La duree passe dans l'aide.
const cohortHelp = (cohort: CohortCompletion, t: TFunction): string =>
  `${t("dashboard.kpi.help.INCIDENT_COHORT_COMPLETION")} ${
    cohort.p50Reached
      ? t("dashboard.kpi.cohort_p50_reached", {
          duration: formatDurationLong(cohort.p50Hours, t),
        })
      : t("dashboard.kpi.cohort_not_reached")
  }`;

// Rend le composant KpiSkeleton pour l'interface tableau de bord.
const KpiSkeleton = () => (
  <Skeleton active paragraph={false} title={{ width: 80 }} />
);

// Rend le composant Dashboard pour l'interface tableau de bord.
const Dashboard = () => {
  const { t } = useTranslation();
  const { user, hasPermission } = useAuth();

  // Derive les permissions utiles a l'ecran courant.
  const userPermissions = useMemo(
    () => user?.permissions ?? [],
    [user?.permissions],
  );

  const [isConfigPanelOpen, setIsConfigPanelOpen] = useState(false);
  const [dashboardConfig, setDashboardConfig] =
    useState<DashboardConfig | null>(() =>
      user?.id ? loadDashboardConfig(user.id) : null,
    );

  const [prevUserId, setPrevUserId] = useState(user?.id);
  if (user?.id !== prevUserId) {
    setPrevUserId(user?.id);
    setDashboardConfig(user?.id ? loadDashboardConfig(user.id) : null);
  }

  // Vues de portee disponibles (tout, agence, service, perso) selon les permissions.
  const availableViews = useMemo(
    () =>
      getDashboardViewOptions(hasPermission, t, {
        hasAgency: Boolean(user?.agencyId) || Boolean(user?.managedAgencyId),
        hasService:
          Boolean(user?.serviceId) ||
          (user?.managedServiceIds && user.managedServiceIds.length > 0),
      }),
    [
      hasPermission,
      t,
      user?.agencyId,
      user?.managedAgencyId,
      user?.serviceId,
      user?.managedServiceIds,
    ],
  );

  const [view, setView] = usePersistentScopeView(
    STORAGE_KEYS.DASHBOARD_VIEW,
    availableViews,
    DASHBOARD_VIEW_FALLBACK_ORDER,
  );

  const [displayMode, setDisplayMode] =
    usePersistentState<DashboardDisplayMode>(
      STORAGE_KEYS.DASHBOARD_DISPLAY_MODE,
      "stats",
      DISPLAY_MODE_OPTIONS,
    );

  // Filtres agence/service/utilisateur de la vue globale admin (mutuellement exclusifs).
  const [filterAgencyId, setFilterAgencyId] = useState<string | undefined>(
    undefined,
  );
  const [filterServiceId, setFilterServiceId] = useState<string | undefined>(
    undefined,
  );
  const [filterUserId, setFilterUserId] = useState<string | undefined>(
    undefined,
  );
  const [periodSelection, setPeriodSelection] = useState<{
    userId?: string;
    filter: PeriodFilter;
  } | null>(null);
  const [selectedYear, setSelectedYear] = useState<number>(() =>
    new Date().getFullYear(),
  );
  const currentUserId = user?.id;
  const periodFilter = useMemo(
    () =>
      periodSelection && periodSelection.userId === currentUserId
        ? periodSelection.filter
        : loadPeriodFilter(currentUserId),
    [currentUserId, periodSelection],
  );
  const handlePeriodFilterChange = useCallback(
    (filter: PeriodFilter) => {
      setPeriodSelection({ userId: currentUserId, filter });
      if (currentUserId) {
        localStorage.setItem(
          `${STORAGE_KEYS.DASHBOARD_PERIOD}:${currentUserId}`,
          JSON.stringify(filter),
        );
      }
    },
    [currentUserId],
  );
  const showGlobalFilters =
    view === "all" &&
    hasPermissionName(userPermissions, PERMISSIONS.INCIDENT.VIEW_ALL);
  // Filtre sur un utilisateur unique : les classements/ comparaisons inter-entites
  // (top services/agences/resolveurs, charge par personne) n'ont plus de sens.
  const hasUserFilter = view === "all" && Boolean(filterUserId);
  // Comparaison inter-agences/services : pertinente uniquement en perimetre global brut
  // (le backend ne calcule top services/agences que la). Sous un filtre agence/service,
  // top agences compare une seule entite on masque pour ne pas afficher du vide.
  const isGlobalScope =
    view === "all" && !filterAgencyId && !filterServiceId && !filterUserId;

  const [isAgencyDropdownOpen, setIsAgencyDropdownOpen] = useState(false);
  const [isServiceDropdownOpen, setIsServiceDropdownOpen] = useState(false);
  const [isUserDropdownOpen, setIsUserDropdownOpen] = useState(false);

  const { data: agencies = [] } = useAgencies({
    enabled: showGlobalFilters && isAgencyDropdownOpen,
  });
  const { data: services = [] } = useServices({
    enabled: showGlobalFilters && isServiceDropdownOpen,
  });
  const { data: allUsers = [] } = useUsers({
    enabled: showGlobalFilters && isUserDropdownOpen,
  });

  const agencyOptions = useMemo(
    () => agencies.map((a) => ({ label: a.name, value: a.id })),
    [agencies],
  );
  const serviceOptions = useMemo(
    () => services.map((s) => ({ label: s.name, value: s.id })),
    [services],
  );
  // L'utilisateur connecte est exclu : il consulte ses propres stats via la vue Mes incidents .
  const userOptions = useMemo(
    () =>
      // Filtre analytique : les inactifs restent selectionnables pour consulter
      // l'historique d'un agent desactive (offboarding), contrairement aux
      // selecteurs d'action (assignation) reserves aux actifs.
      allUsers
        .filter((u) => u.id !== user?.id)
        .map((u) => ({
          label: `${u.firstName} ${u.lastName}`.trim() || u.username,
          value: u.id,
        })),
    [allUsers, user?.id],
  );

  // Traite le changement du filtre agence.
  const handleAgencyFilterChange = useCallback((value?: string) => {
    setFilterAgencyId(value);
    if (value) {
      setFilterServiceId(undefined);
      setFilterUserId(undefined);
    }
  }, []);
  // Traite le changement du filtre service.
  const handleServiceFilterChange = useCallback((value?: string) => {
    setFilterServiceId(value);
    if (value) {
      setFilterAgencyId(undefined);
      setFilterUserId(undefined);
    }
  }, []);
  // Traite le changement du filtre utilisateur.
  const handleUserFilterChange = useCallback((value?: string) => {
    setFilterUserId(value);
    if (value) {
      setFilterAgencyId(undefined);
      setFilterServiceId(undefined);
    }
  }, []);

  const {
    data: metrics,
    isLoading,
    isError,
    refetch,
  } = useDashboardMetrics(view, {
    agencyId: view === "all" ? filterAgencyId : undefined,
    serviceId: view === "all" ? filterServiceId : undefined,
    targetUserId: view === "all" ? filterUserId : undefined,
    year: selectedYear,
    period: periodFilter.period,
    dateFrom: periodFilter.dateFrom,
    dateTo: periodFilter.dateTo,
  });

  // Widgets effectivement affiches : intersection des widgets autorises et de la
  // config utilisateur, puis filtres selon la portee du selecteur de vue.
  // Une vue de portee incident (perso/agence/service) masque les boxes
  // utilisateurs (statistiques globales), pour que selecteur et personnalisation
  const effectiveWidgets = useMemo(() => {
    const permissionedWidgets = getVisibleWidgets(userPermissions);

    const configuredWidgets =
      !user?.id || dashboardConfig === null
        ? permissionedWidgets
        : dashboardConfig.visibleWidgets.filter((widget) =>
            permissionedWidgets.includes(widget),
          );

    return configuredWidgets.filter((widget) =>
      isWidgetAllowedInView(widget, view),
    );
  }, [dashboardConfig, user?.id, userPermissions, view]);

  const isWidgetEnabled = useCallback(
    (widget: WidgetType) => effectiveWidgets.includes(widget),
    [effectiveWidgets],
  );

  // Traite l'ouverture du panneau de configuration.
  const handleOpenConfigPanel = useCallback(() => {
    setIsConfigPanelOpen(true);
  }, []);

  // Traite la fermeture du panneau de configuration.
  const handleCloseConfigPanel = useCallback(() => {
    setIsConfigPanelOpen(false);
  }, []);

  // Traite la sauvegarde de la configuration.
  const handleSaveConfig = useCallback((config: DashboardConfig) => {
    setDashboardConfig(config);
  }, []);

  const canConfigure = hasPermissionName(
    userPermissions,
    PERMISSIONS.DASHBOARD.CONFIGURE,
  );

  // Definition de tous les KPI possibles (titre, valeur, icone, ton).
  const kpiDefinitions = useMemo<KpiDefinition[]>(
    () => [
      {
        widget: WidgetType.INCIDENT_TOTAL,
        title: t("dashboard.kpi.total_incidents"),
        value: metrics?.totalIncidents ?? 0,
        icon: dashboardKpiIcons.totalIncidents,
        tone: "info",
      },
      {
        widget: WidgetType.INCIDENT_ACTIVE,
        title: t("dashboard.kpi.active_incidents"),
        value: metrics?.activeIncidents ?? 0,
        icon: dashboardKpiIcons.activeIncidents,
        tone: "warning",
      },
      {
        widget: WidgetType.INCIDENT_CLOSED,
        title: t("dashboard.kpi.closed_incidents"),
        value: metrics?.closedIncidents ?? 0,
        icon: dashboardKpiIcons.closedIncidents,
        tone: "success",
      },
      {
        widget: WidgetType.INCIDENT_REJECTED,
        title: t("dashboard.kpi.rejected_incidents"),
        value: metrics?.rejectedIncidents ?? 0,
        icon: dashboardKpiIcons.rejectedIncidents,
        tone: "danger",
      },
      {
        widget: WidgetType.INCIDENT_BLOCKED,
        title: t("dashboard.kpi.blocked_incidents"),
        value: metrics?.blockedIncidents ?? 0,
        icon: dashboardKpiIcons.blockedIncidents,
        tone: "danger",
      },
      {
        widget: WidgetType.INCIDENT_AVG_CLOSURE,
        title: t("dashboard.kpi.closure_hours"),
        // Une seule duree en tete : moyenne, p90 et duree hors attente disaient trois
        // choses differentes sous le meme chiffre, elles passent dans l'aide.
        ...durationKpi(
          metrics?.medianClosureHours,
          metrics?.closureSampleSize ?? 0,
          t,
        ),
        help: `${t("dashboard.kpi.help.INCIDENT_AVG_CLOSURE")} ${t(
          "dashboard.kpi.closure_detail",
          {
            mean: formatDurationLong(metrics?.avgClosureHours, t),
            p90: formatDurationLong(metrics?.p90ClosureHours, t),
            net: formatDurationLong(metrics?.medianNetClosureHours, t),
          },
        )}`,
        icon: dashboardKpiIcons.durationHours,
        tone: "info",
      },
      {
        widget: WidgetType.INCIDENT_AVG_RESOLUTION,
        title: t("dashboard.kpi.resolution_hours"),
        ...durationKpi(
          metrics?.medianResolutionHours,
          metrics?.resolutionSampleSize ?? 0,
          t,
        ),
        help: `${t("dashboard.kpi.help.INCIDENT_AVG_RESOLUTION")} ${t(
          "dashboard.kpi.resolution_detail",
          {
            mean: formatDurationLong(metrics?.avgResolutionHours, t),
            p90: formatDurationLong(metrics?.p90ResolutionHours, t),
          },
        )}`,
        icon: dashboardKpiIcons.durationHours,
        tone: "info",
      },
      {
        widget: WidgetType.INCIDENT_ASSIGNED_TO_ME,
        title: t("dashboard.kpi.assigned_to_me"),
        value: metrics?.assignedToMe ?? 0,
        icon: dashboardKpiIcons.assignedToMe,
        tone: "info",
      },
      {
        widget: WidgetType.INCIDENT_TRANSFERRED_BY_ME,
        title: t("dashboard.kpi.transferred_by_me"),
        value: metrics?.transferredByMe ?? 0,
        icon: dashboardKpiIcons.transferredByMe,
        tone: "info",
      },
      {
        widget: WidgetType.INCIDENT_CLOSED_BY_ME,
        title: t("dashboard.kpi.closed_by_me"),
        value: metrics?.closedByMe ?? 0,
        icon: dashboardKpiIcons.closedByMe,
        tone: "success",
      },
      {
        widget: WidgetType.INCIDENT_CREATED_BY_ME,
        title: t("dashboard.kpi.created_by_me"),
        value: metrics?.createdByMe ?? 0,
        icon: dashboardKpiIcons.assignedToMe,
        tone: "info",
      },
      {
        widget: WidgetType.INCIDENT_RESOLVED_BY_ME,
        title: t("dashboard.kpi.resolved_by_me"),
        value: metrics?.resolvedByMe ?? 0,
        icon: actionIcons.validate,
        tone: "success",
      },
      {
        widget: WidgetType.INCIDENT_TRANSFER_RATE,
        title: t("dashboard.kpi.transfer_rate"),
        ...rateKpi(
          metrics?.transferCount ?? 0,
          metrics?.transferDenominator ?? 0,
          t("dashboard.kpi.transfer_detail", {
            count: metrics?.transferDenominator ?? 0,
          }),
          metrics?.transferRate ?? 0,
          t("dashboard.kpi.help.INCIDENT_TRANSFER_RATE"),
          t,
        ),
        icon: actionIcons.transfer,
        tone: "warning",
      },
      {
        widget: WidgetType.INCIDENT_SLA_COMPLIANCE,
        title: t("dashboard.kpi.sla_compliance"),
        ...rateKpi(
          metrics?.slaCompliantCount ?? 0,
          metrics?.slaDenominator ?? 0,
          t("dashboard.kpi.sla_compliance_detail", {
            count: metrics?.slaDenominator ?? 0,
          }),
          metrics?.slaComplianceRate ?? 0,
          t("dashboard.kpi.help.INCIDENT_SLA_COMPLIANCE"),
          t,
        ),
        icon: actionIcons.validate,
        tone: "success",
      },
      {
        widget: WidgetType.INCIDENT_REOPEN_RATE,
        title: t("dashboard.kpi.resolution_reopen_rate"),
        ...rateKpi(
          metrics?.reopenedCount ?? 0,
          metrics?.reopenDenominator ?? 0,
          t("dashboard.kpi.reopen_detail", {
            count: metrics?.reopenDenominator ?? 0,
          }),
          metrics?.resolutionReopenRate ?? 0,
          t("dashboard.kpi.help.INCIDENT_REOPEN_RATE"),
          t,
        ),
        icon: actionIcons.reopen,
        tone: "danger",
      },
      {
        widget: WidgetType.INCIDENT_AVG_FIRST_RESPONSE,
        title: t("dashboard.kpi.avg_first_response"),
        // L'effectif des prises en charge n'est pas remonte : seule la presence d'une
        // duree distingue « aucune prise en charge mesuree » d'un delai nul.
        value: metrics?.avgTimeToFirstResponse
          ? formatDurationLong(metrics.avgTimeToFirstResponse, t)
          : EMPTY_VALUE,
        subtitle: metrics?.avgTimeToFirstResponse
          ? undefined
          : t("dashboard.kpi.not_enough_data"),
        icon: dashboardKpiIcons.durationHours,
        tone: "info",
      },
      {
        widget: WidgetType.INCIDENT_INFLOW_OUTFLOW,
        title: t("dashboard.kpi.inflow_outflow"),
        value:
          (metrics?.netBacklog ?? 0) > 0
            ? `+${metrics?.netBacklog ?? 0}`
            : String(metrics?.netBacklog ?? 0),
        subtitle: flowSubtitle(metrics?.netBacklog ?? 0, t),
        details: [flowDetail(metrics, t)],
        icon: dashboardKpiIcons.closedIncidents,
        tone: (metrics?.netBacklog ?? 0) > 0 ? "warning" : "success",
      },
      {
        widget: WidgetType.INCIDENT_SLA_BREACH_NOW,
        title: t("dashboard.kpi.sla_breach_now"),
        value: metrics?.slaBreachNow ?? 0,
        icon: dashboardKpiIcons.blockedIncidents,
        tone: "danger",
      },
      {
        widget: WidgetType.INCIDENT_CANCELLED,
        title: t("dashboard.kpi.cancelled_incidents"),
        value: metrics?.cancelledIncidents ?? 0,
        icon: dashboardKpiIcons.rejectedIncidents,
        tone: "warning",
      },
      {
        widget: WidgetType.INCIDENT_COHORT_COMPLETION,
        title: t("dashboard.kpi.cohort_completion"),
        // L'indicateur porte l'avancement, pas une duree : « 12 dossiers clotures
        // sur 20 » se lit sans explication la ou un percentile en demandait une.
        value: metrics?.cohortCompletion
          ? metrics.cohortCompletion.closedCount
          : EMPTY_VALUE,
        subtitle: metrics?.cohortCompletion
          ? t("dashboard.kpi.cohort_detail", {
              count: metrics.cohortCompletion.closedCount,
              size: metrics.cohortCompletion.size,
            })
          : t("dashboard.kpi.no_incident_concerned"),
        details:
          metrics?.cohortCompletion &&
          metrics.cohortCompletion.closedCount < metrics.cohortCompletion.size
            ? [
                t("dashboard.kpi.cohort_open_age", {
                  age: formatDurationLong(
                    metrics.cohortCompletion.openMedianAgeHours,
                    t,
                  ),
                }),
              ]
            : undefined,
        help: metrics?.cohortCompletion
          ? cohortHelp(metrics.cohortCompletion, t)
          : undefined,
        icon: dashboardKpiIcons.durationHours,
        tone: metrics?.cohortCompletion?.p50Reached ? "info" : "warning",
      },
      {
        widget: WidgetType.INCIDENT_SCORECARD,
        // La note sur 100 masquait ses raisons : les trois resultats sont annonces
        // separement, chacun avec la population sur laquelle il est mesure.
        title: t("dashboard.kpi.scorecard"),
        value:
          (metrics?.slaDenominator ?? 0) === 0
            ? EMPTY_VALUE
            : (metrics?.slaCompliantCount ?? 0),
        subtitle:
          (metrics?.slaDenominator ?? 0) === 0
            ? t("dashboard.kpi.no_incident_concerned")
            : t("dashboard.kpi.scorecard_on_time", {
                count: metrics?.slaCompliantCount ?? 0,
                total: metrics?.slaDenominator ?? 0,
              }),
        details: [
          t("dashboard.kpi.scorecard_reopened", {
            count: metrics?.reopenedCount ?? 0,
            total: metrics?.reopenDenominator ?? 0,
          }),
          t("dashboard.kpi.scorecard_closed", {
            count: metrics?.closedIncidents ?? 0,
            total: metrics?.totalIncidents ?? 0,
          }),
        ],
        icon: dashboardKpiIcons.durationHours,
        tone: "info",
      },
    ],
    [metrics, t],
  );

  // L'indice d'efficacite n'est calcule (backend) que pour un perimetre agence/service :
  // on ne l'affiche que la (vue agence/service, ou vue globale filtree par agence/service),
  // jamais en global brut ni sous filtre utilisateur, pour ne pas montrer un trompeur.
  const scorecardRelevant =
    view === "agency" ||
    view === "service" ||
    (view === "all" && (Boolean(filterAgencyId) || Boolean(filterServiceId)));

  const visibleKpis = useMemo(
    () =>
      kpiDefinitions.filter(
        (definition) =>
          isWidgetEnabled(definition.widget) &&
          (definition.widget !== WidgetType.INCIDENT_SCORECARD ||
            scorecardRelevant),
      ),
    [isWidgetEnabled, kpiDefinitions, scorecardRelevant],
  );

  const currentKpis = useMemo(
    () =>
      visibleKpis.filter((definition) =>
        CURRENT_KPI_WIDGETS.has(definition.widget),
      ),
    [visibleKpis],
  );
  const periodKpis = useMemo(
    () =>
      visibleKpis.filter(
        (definition) => !CURRENT_KPI_WIDGETS.has(definition.widget),
      ),
    [visibleKpis],
  );

  const yearOptions = useMemo(() => {
    const currentYear = new Date().getFullYear();
    return Array.from({ length: 6 }, (_, index) => currentYear - index).map(
      (year) => ({ label: String(year), value: year }),
    );
  }, []);
  const showsYearPicker =
    displayMode === "graphs" &&
    (isWidgetEnabled(WidgetType.INCIDENT_MONTHLY_CLOSURES) ||
      isWidgetEnabled(WidgetType.INCIDENT_MONTHLY_AVG_CLOSURE));

  // En-tete : selecteur de portee, filtres, bascule stats/graphiques, config.
  const headerActions = useMemo(() => {
    const actions: ReactNode[] = [
      <Select
        key="scope"
        className={styles.scopeSelect}
        value={view}
        onChange={setView}
        // Selecteur desactive tant qu'un filtre agence/service/utilisateur est actif.
        options={availableViews}
        disabled={
          availableViews.length === 0 ||
          Boolean(filterAgencyId) ||
          Boolean(filterServiceId) ||
          Boolean(filterUserId)
        }
        aria-label={t("dashboard.controls.scope")}
      />,
    ];

    if (showGlobalFilters) {
      actions.push(
        <Select
          key="agency-filter"
          allowClear
          showSearch
          optionFilterProp="label"
          placeholder={t("dashboard.controls.filterAgency")}
          style={{ minWidth: 180 }}
          value={filterAgencyId}
          onChange={handleAgencyFilterChange}
          disabled={Boolean(filterServiceId) || Boolean(filterUserId)}
          options={agencyOptions}
          onDropdownVisibleChange={setIsAgencyDropdownOpen}
          aria-label={t("dashboard.controls.filterAgency")}
        />,
        <Select
          key="service-filter"
          allowClear
          showSearch
          optionFilterProp="label"
          placeholder={t("dashboard.controls.filterService")}
          style={{ minWidth: 180 }}
          value={filterServiceId}
          onChange={handleServiceFilterChange}
          disabled={Boolean(filterAgencyId) || Boolean(filterUserId)}
          options={serviceOptions}
          onDropdownVisibleChange={setIsServiceDropdownOpen}
          aria-label={t("dashboard.controls.filterService")}
        />,
        <Select
          key="user-filter"
          allowClear
          showSearch
          optionFilterProp="label"
          placeholder={t("dashboard.controls.filterUser")}
          style={{ minWidth: 180 }}
          value={filterUserId}
          onChange={handleUserFilterChange}
          disabled={Boolean(filterAgencyId) || Boolean(filterServiceId)}
          options={userOptions}
          onDropdownVisibleChange={setIsUserDropdownOpen}
          aria-label={t("dashboard.controls.filterUser")}
        />,
      );
    }

    actions.push(
      <PeriodSelector
        key={`period-${user?.id ?? "anonymous"}`}
        value={periodFilter}
        onChange={handlePeriodFilterChange}
      />,
    );

    actions.push(
      <Segmented
        key="display-mode"
        className={styles.displayModeSegment}
        value={displayMode}
        onChange={(value) => setDisplayMode(value as DashboardDisplayMode)}
        options={[
          {
            value: "stats",
            label: (
              <IconOnlyLabel
                label={t("dashboard.controls.stats")}
                icon={displayModeIcons.stats}
              />
            ),
          },
          {
            value: "graphs",
            label: (
              <IconOnlyLabel
                label={t("dashboard.controls.graphs")}
                icon={displayModeIcons.graphs}
              />
            ),
          },
        ]}
      />,
    );

    if (canConfigure && user?.id) {
      actions.push(
        <IconOnlyButton
          key="configure"
          variant="secondary"
          label={t("dashboard.widgets.customize")}
          icon={actionIcons.configure}
          onClick={handleOpenConfigPanel}
        />,
      );
    }

    return actions;
  }, [
    view,
    setView,
    availableViews,
    filterAgencyId,
    filterServiceId,
    filterUserId,
    t,
    showGlobalFilters,
    displayMode,
    canConfigure,
    user?.id,
    handleAgencyFilterChange,
    agencyOptions,
    handleServiceFilterChange,
    serviceOptions,
    handleUserFilterChange,
    userOptions,
    setDisplayMode,
    handleOpenConfigPanel,
    periodFilter,
    handlePeriodFilterChange,
  ]);

  const activityPeriodLabel =
    metrics?.period === PeriodType.ALL
      ? t("dashboard.period.allTime")
      : metrics?.effectiveDateFrom && metrics?.effectiveDateTo
        ? t("dashboard.period.appliedRange", {
            from: formatDate(metrics.effectiveDateFrom).split(" ")[0],
            to: formatDate(metrics.effectiveDateTo).split(" ")[0],
          })
        : undefined;

  return (
    <>
      <PageContainer>
        <PageHeader
          title={t("dashboard.title")}
          subtitle={activityPeriodLabel ?? t("dashboard.subtitle")}
          actions={headerActions}
        />

        {displayMode === "stats" ? (
          <div className={styles.kpiSections}>
            {currentKpis.length > 0 && (
              <SectionTitle title={t("dashboard.sections.current")} />
            )}
            <Row gutter={[24, 24]} className={styles.kpiRow}>
              {currentKpis.map((definition) => (
                <Col xs={24} sm={12} md={8} lg={6} key={definition.widget}>
                  {(definition.isLoading ?? isLoading) ? (
                    <KpiSkeleton />
                  ) : (
                    <KpiCard
                      title={definition.title}
                      value={definition.value}
                      icon={definition.icon}
                      tone={definition.tone}
                      subtitle={definition.subtitle}
                      details={definition.details}
                      help={
                        definition.help ??
                        t(`dashboard.kpi.help.${definition.widget}`)
                      }
                    />
                  )}
                </Col>
              ))}
            </Row>
            {periodKpis.length > 0 && (
              <SectionTitle title={t("dashboard.sections.period")} />
            )}
            <Row gutter={[24, 24]} className={styles.kpiRow}>
              {periodKpis.map((definition) => (
                <Col xs={24} sm={12} md={8} lg={6} key={definition.widget}>
                  {(definition.isLoading ?? isLoading) ? (
                    <KpiSkeleton />
                  ) : (
                    <KpiCard
                      title={definition.title}
                      value={definition.value}
                      icon={definition.icon}
                      tone={definition.tone}
                      subtitle={definition.subtitle}
                      details={definition.details}
                      help={
                        definition.help ??
                        t(`dashboard.kpi.help.${definition.widget}`)
                      }
                    />
                  )}
                </Col>
              ))}
            </Row>
            {!isLoading && visibleKpis.length === 0 && (
              <div>
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={t("dashboard.widgets.empty")}
                />
              </div>
            )}
          </div>
        ) : (
          <Suspense fallback={<Skeleton active paragraph={{ rows: 8 }} />}>
            <DashboardGraphs
              showTypeDistribution={isWidgetEnabled(
                WidgetType.INCIDENT_TYPE_DISTRIBUTION,
              )}
              showCriticalityDistribution={isWidgetEnabled(
                WidgetType.INCIDENT_CRITICALITY_DISTRIBUTION,
              )}
              showStatusDistribution={isWidgetEnabled(
                WidgetType.INCIDENT_STATUS_DISTRIBUTION,
              )}
              showAging={isWidgetEnabled(WidgetType.INCIDENT_AGING)}
              showCohort={isWidgetEnabled(WidgetType.INCIDENT_COHORT)}
              showTopServices={
                isWidgetEnabled(WidgetType.INCIDENT_TOP_SERVICES) &&
                isGlobalScope
              }
              showTopAgencies={
                isWidgetEnabled(WidgetType.INCIDENT_TOP_AGENCIES) &&
                isGlobalScope
              }
              showTopResolvers={
                isWidgetEnabled(WidgetType.INCIDENT_TOP_RESOLVERS) &&
                !hasUserFilter
              }
              typeDistribution={
                isWidgetEnabled(WidgetType.INCIDENT_TYPE_DISTRIBUTION)
                  ? (metrics?.distributionByType ?? {})
                  : {}
              }
              criticalityDistribution={
                isWidgetEnabled(WidgetType.INCIDENT_CRITICALITY_DISTRIBUTION)
                  ? (metrics?.distributionByCriticality ?? {})
                  : {}
              }
              statusDistribution={
                isWidgetEnabled(WidgetType.INCIDENT_STATUS_DISTRIBUTION)
                  ? (metrics?.distributionByStatus ?? {})
                  : {}
              }
              ageDistribution={
                isWidgetEnabled(WidgetType.INCIDENT_AGING)
                  ? (metrics?.ageDistribution ?? {})
                  : {}
              }
              cohortOutcome={
                isWidgetEnabled(WidgetType.INCIDENT_COHORT)
                  ? (metrics?.cohortOutcome ?? {})
                  : {}
              }
              topServices={
                isWidgetEnabled(WidgetType.INCIDENT_TOP_SERVICES)
                  ? (metrics?.topServices ?? [])
                  : []
              }
              topAgencies={
                isWidgetEnabled(WidgetType.INCIDENT_TOP_AGENCIES)
                  ? (metrics?.topAgencies ?? [])
                  : []
              }
              topResolvers={
                isWidgetEnabled(WidgetType.INCIDENT_TOP_RESOLVERS)
                  ? (metrics?.topResolvers ?? [])
                  : []
              }
              showWorkload={
                isWidgetEnabled(WidgetType.INCIDENT_WORKLOAD) && !hasUserFilter
              }
              workload={
                isWidgetEnabled(WidgetType.INCIDENT_WORKLOAD)
                  ? (metrics?.workload ?? [])
                  : []
              }
              showMonthlyClosures={isWidgetEnabled(
                WidgetType.INCIDENT_MONTHLY_CLOSURES,
              )}
              showMonthlyAvgClosure={isWidgetEnabled(
                WidgetType.INCIDENT_MONTHLY_AVG_CLOSURE,
              )}
              showClosureByType={isWidgetEnabled(
                WidgetType.INCIDENT_CLOSURE_BY_TYPE,
              )}
              showClosureByCriticality={isWidgetEnabled(
                WidgetType.INCIDENT_CLOSURE_BY_CRITICALITY,
              )}
              monthlyClosures={metrics?.monthlyClosures ?? []}
              monthlyAvgClosureHours={metrics?.monthlyAvgClosureHours ?? []}
              closureHoursByType={metrics?.closureHoursByType ?? []}
              closureHoursByCriticality={
                metrics?.closureHoursByCriticality ?? []
              }
              year={selectedYear}
              yearPickerNode={
                showsYearPicker ? (
                  <Select
                    key="year-picker"
                    style={{ width: 100 }}
                    value={selectedYear}
                    onChange={setSelectedYear}
                    options={yearOptions}
                    aria-label={t("dashboard.controls.year")}
                  />
                ) : undefined
              }
            />
          </Suspense>
        )}

        {/* Activite recente : uniquement en mode stats; les graphiques restent dedies a la data-viz. */}
        {displayMode !== "graphs" &&
          isWidgetEnabled(WidgetType.INCIDENT_RECENT_ACTIVITY) && (
            <Row gutter={[24, 24]} className={styles.mainContentRow}>
              <Col xs={24} lg={24}>
                <SectionCard
                  title={t("dashboard.cards.recent_activity")}
                  toolbar={
                    <Link to={APP_ROUTES.INCIDENTS}>
                      {t("dashboard.cards.view_all")}
                    </Link>
                  }
                >
                  <RecentIncidentsList
                    incidents={metrics?.recentActivities ?? []}
                    isLoading={isLoading}
                    isError={isError}
                    onRetry={refetch}
                  />
                </SectionCard>
              </Col>
            </Row>
          )}
      </PageContainer>

      {isConfigPanelOpen && user?.id && (
        <DashboardConfigPanel
          userId={user.id}
          userPermissions={userPermissions}
          currentConfig={dashboardConfig}
          view={view}
          onClose={handleCloseConfigPanel}
          onSave={handleSaveConfig}
        />
      )}
    </>
  );
};

export default memo(Dashboard);
