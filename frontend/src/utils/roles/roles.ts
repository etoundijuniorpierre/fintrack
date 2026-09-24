// Helpers de roles : normalise les noms techniques et produit les libelles affichables.

export const ROLE_NAMES = {
  AGENT: "AGENT",
  AGENCY_MANAGER: "CHEF_AGENCE",
  SERVICE_MANAGER: "CHEF_SERVICE",
  ADMIN: "ADMIN",
  SUPER_ADMIN: "SUPER_ADMIN",
} as const;

// Retire le prefixe technique et harmonise la casse d'un role.
export const normalizeRoleName = (roleName?: string): string =>
  roleName
    ?.trim()
    .replace(/^ROLE_/i, "")
    .toUpperCase() ?? "";

// Retourne la cle i18n associee au role normalise.
export const getRoleTranslationKey = (roleName?: string): string =>
  normalizeRoleName(roleName);

// Produit le libelle utilisateur d'un role avec repli sur son nom technique.
export const formatRoleName = (
  role:
    | { displayName?: string | null; name?: string | null }
    | null
    | undefined,
  t: (key: string, opts?: { defaultValue?: string }) => string,
): string => {
  const display = role?.displayName?.trim();
  if (display) return display;
  const name = role?.name?.trim() ?? "";
  return t(`users.roles.${getRoleTranslationKey(name)}`, {
    defaultValue: name,
  });
};

// Indique si une liste contient le role demande.
export const hasRoleName = (
  roles: readonly { name?: string }[] | undefined,
  roleName: string,
): boolean =>
  roles?.some(
    (role) => normalizeRoleName(role.name) === normalizeRoleName(roleName),
  ) ?? false;
