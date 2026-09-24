// Catalogue des permissions : associe chaque code a un domaine fonctionnel et a ses cles i18n.

import { PERMISSIONS } from "./permissions";

// Decrit les domaines fonctionnels du catalogue de permissions.
export type PermissionGroup =
  | "incident"
  | "user"
  | "role"
  | "report"
  | "dashboard"
  | "notification"
  | "settings"
  | "audit";

// Fixe l'ordre d'affichage des domaines dans le centre d'aide.
export const PERMISSION_GROUPS: readonly PermissionGroup[] = [
  "incident",
  "user",
  "role",
  "report",
  "dashboard",
  "notification",
  "settings",
  "audit",
] as const;

const GROUP_BY_KEY: Record<keyof typeof PERMISSIONS, PermissionGroup> = {
  INCIDENT: "incident",
  REPORT: "report",
  DASHBOARD: "dashboard",
  NOTIFICATION: "notification",
  USER: "user",
  ROLE: "role",
  SETTINGS: "settings",
  AUDIT: "audit",
};

// Associe chaque code de permission a son domaine fonctionnel.
export const PERMISSION_CATALOG: Record<string, PermissionGroup> =
  Object.entries(PERMISSIONS).reduce<Record<string, PermissionGroup>>(
    (acc, [groupKey, codes]) => {
      const group = GROUP_BY_KEY[groupKey as keyof typeof PERMISSIONS];
      Object.values(codes).forEach((code) => {
        acc[code as string] = group;
      });
      return acc;
    },
    {},
  );

// Retourne le domaine d'une permission avec repli sur les parametres.
export const getPermissionGroup = (code: string): PermissionGroup =>
  PERMISSION_CATALOG[code] ?? "settings";

// Retourne la cle i18n de description d'une permission.
export const permissionDescriptionKey = (code: string): string =>
  `help.permissions.${code}`;

// Regroupe des codes de permission par domaine dans l'ordre d'affichage.
export const groupPermissions = (
  codes: readonly string[],
): { group: PermissionGroup; codes: string[] }[] => {
  const byGroup = new Map<PermissionGroup, string[]>();
  codes.forEach((code) => {
    const group = getPermissionGroup(code);
    const list = byGroup.get(group) ?? [];
    list.push(code);
    byGroup.set(group, list);
  });
  return PERMISSION_GROUPS.filter((group) => byGroup.has(group)).map(
    (group) => ({
      group,
      codes: (byGroup.get(group) ?? []).slice().sort(),
    }),
  );
};
