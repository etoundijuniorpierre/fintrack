// Helpers pour valider et extraire les permissions d'un utilisateur.

import type { TFunction } from "i18next";

// Liste les codes de permissions connus par le frontend.
export const PERMISSIONS = {
  INCIDENT: {
    CREATE: "INCIDENT_CREATE",
    UPDATE: "INCIDENT_UPDATE",
    REOPEN: "INCIDENT_REOPEN",
    VIEW_OWN: "INCIDENT_VIEW_OWN",
    VIEW_AGENCY: "INCIDENT_VIEW_AGENCY",
    VIEW_SERVICE: "INCIDENT_VIEW_SERVICE",
    VIEW_ALL: "INCIDENT_VIEW_ALL",
    VALIDATE: "INCIDENT_VALIDATE",
    TRANSFER: "INCIDENT_TRANSFER",
    AUTO_TRANSFER: "INCIDENT_AUTO_TRANSFER",
    TRANSFER_WITH_REASON: "INCIDENT_TRANSFER_WITH_REASON",
    ASSIGN: "INCIDENT_ASSIGN",
    TREAT: "INCIDENT_TREAT",
    RESOLVE: "INCIDENT_RESOLVE",
    CLOSE: "INCIDENT_CLOSE",
    CANCEL: "INCIDENT_CANCEL",
    REJECT: "INCIDENT_REJECT",
    DELETE: "INCIDENT_DELETE",
    VALIDATION_DIRECTION: "VALIDATION_DIRECTION",
  },
  REPORT: {
    VIEW_OWN: "REPORT_VIEW_OWN",
    VIEW_AGENCY: "REPORT_VIEW_AGENCY",
    VIEW_SERVICE: "REPORT_VIEW_SERVICE",
    VIEW_ALL: "REPORT_VIEW_ALL",
    EXPORT: "REPORT_EXPORT",
    GENERATE: "REPORT_GENERATE",
    GENERATE_ALL_SCOPES: "REPORT_GENERATE_ALL_SCOPES",
    SEND_EMAIL: "REPORT_SEND_EMAIL",
    DELETE: "REPORT_DELETE",
  },
  DASHBOARD: {
    CONFIGURE: "DASHBOARD_CONFIGURE",
  },
  NOTIFICATION: {
    VIEW_OWN: "NOTIFICATION_VIEW_OWN",
    VIEW_ALL: "NOTIFICATION_VIEW_ALL",
    MANAGE: "NOTIFICATION_MANAGE",
  },
  USER: {
    CREATE_ALL_AGENT: "USER_CREATE_ALL_AGENT",
    CREATE_AGENT_AGENCY: "USER_CREATE_AGENT_AGENCY",
    CREATE_AGENT_SERVICE: "USER_CREATE_AGENT_SERVICE",
    CREATE_AGENCY_MANAGER: "USER_CREATE_CHEF_AGENCE",
    CREATE_SERVICE_MANAGER: "USER_CREATE_CHEF_SERVICE",
    CREATE_ADMIN: "USER_CREATE_ADMIN",
    UPDATE: "USER_UPDATE",
    UPDATE_AGENT: "USER_UPDATE_AGENT",
    DELETE: "USER_DELETE",
    VIEW_AGENCY: "USER_VIEW_AGENCY",
    VIEW_SERVICE: "USER_VIEW_SERVICE",
    VIEW_ALL: "USER_VIEW_ALL",
    MANAGE_PROFILE: "USER_MANAGE_PROFILE",
  },
  ROLE: {
    CREATE: "ROLE_CREATE",
    UPDATE: "ROLE_UPDATE",
    DELETE: "ROLE_DELETE",
    ASSIGN: "ROLE_ASSIGN",
  },
  SETTINGS: {
    INCIDENT_TYPES: "SETTINGS_INCIDENT_TYPES",
    SYSTEM: "SETTINGS_SYSTEM",
  },
  AUDIT: {
    VIEW: "AUDIT_VIEW",
  },
} as const;

// Extrait les valeurs d'un dictionnaire de constantes.
type Values<T> = T[keyof T];

// Type l'ensemble des codes de permission utilises par l'interface.
export type Permission =
  | Values<typeof PERMISSIONS.INCIDENT>
  | Values<typeof PERMISSIONS.REPORT>
  | Values<typeof PERMISSIONS.DASHBOARD>
  | Values<typeof PERMISSIONS.NOTIFICATION>
  | Values<typeof PERMISSIONS.USER>
  | Values<typeof PERMISSIONS.ROLE>
  | Values<typeof PERMISSIONS.SETTINGS>
  | Values<typeof PERMISSIONS.AUDIT>;

// Type une fonction de verification de permission.
export type PermissionChecker = (permission: string) => boolean;

// Decrit les portees de donnees selectionnables.
export type ScopeView =
  | "own"
  | "assigned"
  | "agency"
  | "service"
  | "byAgency"
  | "byService"
  | "all"
  | "user";

// Decrit une option de portee affichee dans un selecteur.
export interface ScopeViewOption {
  label: string;
  value: ScopeView;
}

// Liste les permissions donnant acces aux vues incidents.
export const INCIDENT_VIEW_PERMISSIONS = [
  PERMISSIONS.INCIDENT.VIEW_OWN,
  PERMISSIONS.INCIDENT.VIEW_AGENCY,
  PERMISSIONS.INCIDENT.VIEW_SERVICE,
  PERMISSIONS.INCIDENT.VIEW_ALL,
] as const;

// Acces a la page des incidents : aligne sur le @PreAuthorize du backend
// (getAllIncidents / getIncidentById), TREAT et RESOLVE inclus pour la vue
// assignes .
export const INCIDENT_ACCESS_PERMISSIONS = [
  ...INCIDENT_VIEW_PERMISSIONS,
  PERMISSIONS.INCIDENT.TREAT,
  PERMISSIONS.INCIDENT.RESOLVE,
  PERMISSIONS.INCIDENT.ASSIGN,
] as const;

// Liste les permissions de consultation des utilisateurs.
export const USER_VIEW_PERMISSIONS = [
  PERMISSIONS.USER.VIEW_AGENCY,
  PERMISSIONS.USER.VIEW_SERVICE,
  PERMISSIONS.USER.VIEW_ALL,
] as const;

// Liste les permissions de creation des utilisateurs.
export const USER_CREATE_PERMISSIONS = [
  PERMISSIONS.USER.CREATE_ALL_AGENT,
  PERMISSIONS.USER.CREATE_AGENT_AGENCY,
  PERMISSIONS.USER.CREATE_AGENT_SERVICE,
  PERMISSIONS.USER.CREATE_AGENCY_MANAGER,
  PERMISSIONS.USER.CREATE_SERVICE_MANAGER,
  PERMISSIONS.USER.CREATE_ADMIN,
] as const;

// Liste les permissions de mise a jour des utilisateurs.
export const USER_UPDATE_PERMISSIONS = [
  PERMISSIONS.USER.UPDATE,
  PERMISSIONS.USER.UPDATE_AGENT,
] as const;

// Indique si au moins une permission demandee est accordee par un checker.
export const hasAnyPermission = (
  hasPermission: PermissionChecker,
  permissions: readonly string[],
): boolean => permissions.some((permission) => hasPermission(permission));

// Compare une permission par nom sans tenir compte de la casse.
export const hasPermissionName = (
  permissions: readonly string[],
  permission: string,
): boolean =>
  permissions.some(
    (candidate) => candidate.toUpperCase() === permission.toUpperCase(),
  );

// Indique si une liste contient au moins une permission requise.
export const hasAnyPermissionName = (
  permissions: readonly string[],
  requiredPermissions: readonly string[],
): boolean =>
  requiredPermissions.some((permission) =>
    hasPermissionName(permissions, permission),
  );

// Permissions effectives d'un utilisateur : (directes ∪ heritees des roles) - revoquees.
export const getEffectivePermissions = <T extends { id: string }>(user: {
  permissions?: T[] | null;
  roles?: { permissions?: T[] | null }[] | null;
  revokedPermissions?: T[] | null;
}): T[] => {
  const rolePerms = (user.roles ?? []).flatMap((role) => role.permissions ?? []);
  const revokedIds = new Set(
    (user.revokedPermissions ?? []).map((permission) => permission.id),
  );
  const seen = new Set<string>();
  return [...rolePerms, ...(user.permissions ?? [])].filter((permission) => {
    if (!permission?.id || seen.has(permission.id)) return false;
    // Soustraction des permissions revoquees : elles ne sont plus effectives.
    if (revokedIds.has(permission.id)) return false;
    seen.add(permission.id);
    return true;
  });
};

// Noms des permissions effectives (directes + heritees des roles).
export const getEffectivePermissionNames = (user: {
  permissions?: { id: string; name: string }[] | null;
  roles?: { permissions?: { id: string; name: string }[] | null }[] | null;
  revokedPermissions?: { id: string; name: string }[] | null;
}): string[] => getEffectivePermissions(user).map((permission) => permission.name);

// Construit les options de portee disponibles pour le tableau de bord.
export const getDashboardViewOptions = (
  hasPermission: PermissionChecker,
  t: TFunction,
  membership?: { hasAgency?: boolean; hasService?: boolean },
): ScopeViewOption[] => {
  const canViewAll = hasPermission(PERMISSIONS.INCIDENT.VIEW_ALL);
  const hasAgency = membership?.hasAgency ?? true;
  const hasService = membership?.hasService ?? true;
  const options: ScopeViewOption[] = [];

  if (hasPermission(PERMISSIONS.INCIDENT.VIEW_OWN)) {
    options.push({ label: t("dashboard.views.own"), value: "own" });
  }

  // Les vues personnelles restent disponibles meme quand l'utilisateur a aussi VIEW_ALL.
  if (hasPermission(PERMISSIONS.INCIDENT.VIEW_AGENCY) && hasAgency) {
    options.push({ label: t("dashboard.views.agency"), value: "agency" });
  }

  if (hasPermission(PERMISSIONS.INCIDENT.VIEW_SERVICE) && hasService) {
    options.push({ label: t("dashboard.views.service"), value: "service" });
  }

  if (canViewAll) {
    options.push({ label: t("dashboard.views.all"), value: "all" });
  }

  return options;
};

// Construit les options de portee disponibles pour les incidents.
export const getIncidentViewOptions = (
  hasPermission: PermissionChecker,
  t: TFunction,
  membership?: { hasAgency?: boolean; hasService?: boolean },
): ScopeViewOption[] => {
  const canViewAll = hasPermission(PERMISSIONS.INCIDENT.VIEW_ALL);
  const hasAgency = membership?.hasAgency ?? true;
  const hasService = membership?.hasService ?? true;
  const options: ScopeViewOption[] = [];

  if (
    hasAnyPermission(hasPermission, [
      PERMISSIONS.INCIDENT.TREAT,
      PERMISSIONS.INCIDENT.RESOLVE,
    ])
  ) {
    options.push({ label: t("incidents.views.assigned"), value: "assigned" });
  }

  if (hasPermission(PERMISSIONS.INCIDENT.VIEW_OWN)) {
    options.push({ label: t("incidents.views.own"), value: "own" });
  }

  if (hasPermission(PERMISSIONS.INCIDENT.VIEW_AGENCY)) {
    if (hasAgency) {
      options.push({ label: t("incidents.views.agency"), value: "agency" });
    } else if (canViewAll) {
      options.push({ label: t("incidents.views.byAgency"), value: "byAgency" });
    }
  }

  if (hasPermission(PERMISSIONS.INCIDENT.VIEW_SERVICE)) {
    if (hasService) {
      options.push({ label: t("incidents.views.service"), value: "service" });
    } else if (canViewAll) {
      options.push({
        label: t("incidents.views.byService"),
        value: "byService",
      });
    }
  }

  if (canViewAll) {
    options.push({ label: t("incidents.views.all"), value: "all" });
  }

  return options;
};

// Construit les options de portee disponibles pour les rapports.
export const getReportScopeOptions = (
  hasPermission: PermissionChecker,
  t: TFunction,
): ScopeViewOption[] => {
  const canGenerateAllScopes = hasPermission(
    PERMISSIONS.REPORT.GENERATE_ALL_SCOPES,
  );
  const options: ScopeViewOption[] = [];

  if (hasPermission(PERMISSIONS.REPORT.VIEW_OWN)) {
    options.push({ label: t("reports.scope.own"), value: "own" });
  }

  if (
    canGenerateAllScopes ||
    hasPermission(PERMISSIONS.REPORT.VIEW_AGENCY)
  ) {
    options.push({
      label: t(
        canGenerateAllScopes
          ? "reports.scope.byAgency"
          : "reports.scope.agency",
      ),
      value: "agency",
    });
  }

  if (
    canGenerateAllScopes ||
    hasPermission(PERMISSIONS.REPORT.VIEW_SERVICE)
  ) {
    options.push({
      label: t(
        canGenerateAllScopes
          ? "reports.scope.byService"
          : "reports.scope.service",
      ),
      value: "service",
    });
  }

  if (
    canGenerateAllScopes ||
    hasPermission(PERMISSIONS.REPORT.VIEW_AGENCY) ||
    hasPermission(PERMISSIONS.REPORT.VIEW_SERVICE)
  ) {
    options.push({ label: t("reports.scope.byUser"), value: "user" });
  }

  if (canGenerateAllScopes) {
    options.push({ label: t("reports.scope.all"), value: "all" });
  }

  return options;
};

// Determine la vue preferee selon les options disponibles.
export const getPreferredView = (
  options: readonly ScopeViewOption[],
  preferred: string | null | undefined,
  fallbackOrder: readonly ScopeView[],
): ScopeView => {
  const preferredOption = options.find((option) => option.value === preferred);
  if (preferredOption) {
    return preferredOption.value;
  }

  return (
    fallbackOrder.find((view) =>
      options.some((option) => option.value === view),
    ) ??
    options[0]?.value ??
    "own"
  );
};
