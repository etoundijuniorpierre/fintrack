// Tests frontend : verifie les clefs i18n construites a partir d'un enum.

import { describe, expect, it } from "vitest";
import {
  ActionType,
  Criticality,
  IncidentCause,
  IncidentStatus,
  IncidentValidatorRole,
  VALIDATOR_ROLES_WITH_TARGET,
} from "../api/incident/enums/enums";

// Complement de i18nIntegrity : celui-ci ignore volontairement les clefs dynamiques,
// car t(variable) n'est pas verifiable statiquement. Mais quand la variable parcourt
// un enum connu, l'ensemble des clefs attendues l'est aussi. Sans ce garde, une valeur
// ajoutee a un enum s'affiche en clef brute (« incidents.status.NOUVEAU_STATUT ») et
// la parite fr/en reste satisfaite puisque la clef manque des deux cotes.

type LocaleTree = Record<string, unknown>;

const localeModules = import.meta.glob("./locales/*/*.json", {
  eager: true,
  import: "default",
}) as Record<string, LocaleTree>;

// Aplatit un arbre de traduction en clefs pointees.
const flattenKeys = (value: LocaleTree, prefix = ""): Set<string> =>
  Object.entries(value).reduce((keys, [key, child]) => {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (child && typeof child === "object" && !Array.isArray(child)) {
      flattenKeys(child as LocaleTree, fullKey).forEach((nested) =>
        keys.add(nested),
      );
    } else {
      keys.add(fullKey);
    }
    return keys;
  }, new Set<string>());

// Rassemble toutes les clefs definies pour une locale.
const keysForLocale = (locale: string): Set<string> => {
  const merged = new Set<string>();
  Object.entries(localeModules)
    .filter(([path]) => path.includes(`/locales/${locale}/`))
    .forEach(([, tree]) => flattenKeys(tree).forEach((key) => merged.add(key)));
  return merged;
};

const LOCALES = { fr: keysForLocale("fr"), en: keysForLocale("en") };

// Familles de clefs dont le suffixe est borne par un enum, et dont l'absence se
// traduirait par un code technique affiche a l'utilisateur.
const ENUM_KEY_FAMILIES: Array<{
  prefix: string;
  values: readonly string[];
}> = [
  { prefix: "incidents.status", values: Object.values(IncidentStatus) },
  { prefix: "incidents.criticality", values: Object.values(Criticality) },
  { prefix: "incidents.cause_values", values: Object.values(IncidentCause) },
  { prefix: "incidents.action_type", values: Object.values(ActionType) },
  {
    prefix: "incidents.validator_roles",
    values: Object.values(IncidentValidatorRole),
  },
  // Declinaison nommee : « Chef du service {{name}} ».
  {
    prefix: "incidents.validator_roles",
    values: VALIDATOR_ROLES_WITH_TARGET.map((role) => `${role}_named`),
  },
];

describe("i18n enum-derived keys", () => {
  it.each(Object.keys(LOCALES))(
    "defines every enum-derived key in %s",
    (locale) => {
      const defined = LOCALES[locale as keyof typeof LOCALES];
      const missing = ENUM_KEY_FAMILIES.flatMap(({ prefix, values }) =>
        values
          .map((value) => `${prefix}.${value}`)
          .filter((key) => !defined.has(key)),
      );

      expect(missing, `clefs absentes de ${locale}`).toEqual([]);
    },
  );
});
