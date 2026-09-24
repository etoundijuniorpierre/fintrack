// Gestionnaires de validation des droits d'acces aux widgets du tableau de bord.
import { WidgetType } from "../../../types/dashboard";
import {
  INCIDENT_VIEW_PERMISSIONS,
  PERMISSIONS,
  hasAnyPermissionName,
  hasPermissionName,
} from "../../permissions/permissions";

// Decrit les permissions requises pour afficher un widget.
interface WidgetPermissionRule {
  all?: readonly string[];
  any?: readonly string[];
}

const incidentViewRule = { any: INCIDENT_VIEW_PERMISSIONS };

// Associe chaque widget aux permissions qui autorisent son affichage.
export const WIDGET_PERMISSION_RULES: Record<WidgetType, WidgetPermissionRule> =
  {
    [WidgetType.INCIDENT_ACTIVE]: incidentViewRule,
    [WidgetType.INCIDENT_CLOSED]: incidentViewRule,
    [WidgetType.INCIDENT_REJECTED]: incidentViewRule,
    [WidgetType.INCIDENT_BLOCKED]: incidentViewRule,
    [WidgetType.INCIDENT_TOTAL]: incidentViewRule,
    [WidgetType.INCIDENT_AVG_CLOSURE]: incidentViewRule,
    [WidgetType.INCIDENT_AVG_RESOLUTION]: incidentViewRule,
    [WidgetType.INCIDENT_ASSIGNED_TO_ME]: {
      any: [PERMISSIONS.INCIDENT.TREAT, PERMISSIONS.INCIDENT.RESOLVE],
    },
    [WidgetType.INCIDENT_TRANSFERRED_BY_ME]: incidentViewRule,
    [WidgetType.INCIDENT_CLOSED_BY_ME]: incidentViewRule,
    [WidgetType.INCIDENT_TYPE_DISTRIBUTION]: incidentViewRule,
    [WidgetType.INCIDENT_CRITICALITY_DISTRIBUTION]: incidentViewRule,
    [WidgetType.INCIDENT_STATUS_DISTRIBUTION]: incidentViewRule,
    [WidgetType.INCIDENT_RECENT_ACTIVITY]: incidentViewRule,
    [WidgetType.INCIDENT_TOP_SERVICES]: {
      all: [PERMISSIONS.INCIDENT.VIEW_ALL],
    },
    [WidgetType.INCIDENT_TOP_AGENCIES]: {
      all: [PERMISSIONS.INCIDENT.VIEW_ALL],
    },
    [WidgetType.INCIDENT_TOP_RESOLVERS]: incidentViewRule,
    // La serie mensuelle suit desormais le perimetre demande : la reserver aux
    // porteurs de VIEW_ALL la rendait invisible dans les vues ou elle est offerte.
    [WidgetType.INCIDENT_MONTHLY_CLOSURES]: incidentViewRule,
    [WidgetType.INCIDENT_MONTHLY_AVG_CLOSURE]: incidentViewRule,
    [WidgetType.INCIDENT_CANCELLED]: incidentViewRule,
    [WidgetType.INCIDENT_COHORT_COMPLETION]: incidentViewRule,
    [WidgetType.INCIDENT_CLOSURE_BY_TYPE]: incidentViewRule,
    [WidgetType.INCIDENT_CLOSURE_BY_CRITICALITY]: incidentViewRule,
    [WidgetType.INCIDENT_TRANSFER_RATE]: incidentViewRule,
    [WidgetType.INCIDENT_SLA_COMPLIANCE]: incidentViewRule,
    [WidgetType.INCIDENT_REOPEN_RATE]: incidentViewRule,
    [WidgetType.INCIDENT_AVG_FIRST_RESPONSE]: incidentViewRule,
    [WidgetType.INCIDENT_CREATED_BY_ME]: incidentViewRule,
    [WidgetType.INCIDENT_RESOLVED_BY_ME]: incidentViewRule,
    [WidgetType.INCIDENT_INFLOW_OUTFLOW]: incidentViewRule,
    [WidgetType.INCIDENT_AGING]: incidentViewRule,
    [WidgetType.INCIDENT_COHORT]: incidentViewRule,
    [WidgetType.INCIDENT_SLA_BREACH_NOW]: incidentViewRule,
    [WidgetType.INCIDENT_WORKLOAD]: {
      any: [
        PERMISSIONS.INCIDENT.VIEW_ALL,
        PERMISSIONS.INCIDENT.VIEW_AGENCY,
        PERMISSIONS.INCIDENT.VIEW_SERVICE,
      ],
    },
    [WidgetType.INCIDENT_SCORECARD]: {
      any: [
        PERMISSIONS.INCIDENT.VIEW_ALL,
        PERMISSIONS.INCIDENT.VIEW_AGENCY,
        PERMISSIONS.INCIDENT.VIEW_SERVICE,
      ],
    },
  };

// Indique si un widget est visible pour les permissions donnees.
export const isWidgetVisible = (
  widgetType: WidgetType,
  userPermissions: string[],
): boolean => {
  const rule = WIDGET_PERMISSION_RULES[widgetType];
  const hasAll =
    rule.all?.every((permission) =>
      hasPermissionName(userPermissions, permission),
    ) ?? true;
  const hasAny = rule.any
    ? hasAnyPermissionName(userPermissions, rule.any)
    : true;
  return hasAll && hasAny;
};

// Retourne les widgets accessibles a l'utilisateur courant.
export const getVisibleWidgets = (userPermissions: string[]): WidgetType[] => {
  return (Object.keys(WIDGET_PERMISSION_RULES) as WidgetType[]).filter(
    (widgetType) => isWidgetVisible(widgetType, userPermissions),
  );
};

// Identifie les widgets de statistiques utilisateurs globales.
export const isUserWidget = (widgetType: WidgetType): boolean =>
  widgetType.startsWith("USER_");

// Liste les widgets reserves aux vues personnelles d'incidents.
const PERSONAL_WIDGETS: ReadonlySet<WidgetType> = new Set([
  WidgetType.INCIDENT_ASSIGNED_TO_ME,
  WidgetType.INCIDENT_TRANSFERRED_BY_ME,
  WidgetType.INCIDENT_CLOSED_BY_ME,
  WidgetType.INCIDENT_CREATED_BY_ME,
  WidgetType.INCIDENT_RESOLVED_BY_ME,
]);

// Indique si un widget porte des donnees personnelles.
export const isPersonalWidget = (widgetType: WidgetType): boolean =>
  PERSONAL_WIDGETS.has(widgetType);

// Liste les widgets compatibles avec la vue personnelle.
const OWN_VIEW_ALLOWED: ReadonlySet<WidgetType> = new Set([
  WidgetType.INCIDENT_TOTAL,
  WidgetType.INCIDENT_ACTIVE,
  WidgetType.INCIDENT_BLOCKED,
  WidgetType.INCIDENT_AVG_CLOSURE,
  WidgetType.INCIDENT_AVG_RESOLUTION,
  WidgetType.INCIDENT_ASSIGNED_TO_ME,
  WidgetType.INCIDENT_TRANSFERRED_BY_ME,
  WidgetType.INCIDENT_CLOSED_BY_ME,
  WidgetType.INCIDENT_CREATED_BY_ME,
  WidgetType.INCIDENT_RESOLVED_BY_ME,
  WidgetType.INCIDENT_TYPE_DISTRIBUTION,
  WidgetType.INCIDENT_CRITICALITY_DISTRIBUTION,
  WidgetType.INCIDENT_STATUS_DISTRIBUTION,
  WidgetType.INCIDENT_AGING,
  WidgetType.INCIDENT_COHORT,
  WidgetType.INCIDENT_INFLOW_OUTFLOW,
  WidgetType.INCIDENT_SLA_BREACH_NOW,
  WidgetType.INCIDENT_RECENT_ACTIVITY,
  WidgetType.INCIDENT_MONTHLY_CLOSURES,
  WidgetType.INCIDENT_MONTHLY_AVG_CLOSURE,
  WidgetType.INCIDENT_CLOSURE_BY_TYPE,
  WidgetType.INCIDENT_CLOSURE_BY_CRITICALITY,
  WidgetType.INCIDENT_COHORT_COMPLETION,
  WidgetType.INCIDENT_CANCELLED,
]);

// Indique si un widget peut etre affiche dans une portee donnee.
export const isWidgetAllowedInView = (
  widget: WidgetType,
  view: "own" | "agency" | "service" | "all" | string,
): boolean => {
  if (widget === WidgetType.INCIDENT_SCORECARD) {
    return view === "agency" || view === "service";
  }
  if (view === "own") return OWN_VIEW_ALLOWED.has(widget);
  if (view === "all") {
    return !isPersonalWidget(widget);
  }
  return (
    !isPersonalWidget(widget) &&
    widget !== WidgetType.INCIDENT_TOP_SERVICES &&
    widget !== WidgetType.INCIDENT_TOP_AGENCIES
  );
};
