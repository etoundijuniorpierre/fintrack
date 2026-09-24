// Tests frontend : verifie le comportement de i18n integrity.test.

import { describe, expect, it } from "vitest";

// Garde-fou i18n global :
// 1) parite stricte fr <-> en (memes cles des deux cotes, tous namespaces) ;
// 2) toute reference t('cle.litterale') du code est definie dans les DEUX locales.
// Remplace l'ancien test limite a Settings. Les cles dynamiques (t(variable)) ne
// sont pas verifiables statiquement et sont ignorees ; la pluralisation i18next
// (cle_one / cle_other...) est prise en compte.

type LocaleTree = Record<string, unknown>;

const localeModules = import.meta.glob("./locales/*/*.json", {
  eager: true,
  import: "default",
}) as Record<string, LocaleTree>;

const sourceModules = import.meta.glob("../**/*.{ts,tsx}", {
  eager: true,
  query: "?raw",
  import: "default",
}) as Record<string, string>;

// Definit les donnees de test plural suffixes.
const PLURAL_SUFFIXES = ["_zero", "_one", "_two", "_few", "_many", "_other"];
// Definit les donnees de test translation key pattern.
const TRANSLATION_KEY_PATTERN = /\bt\(\s*(['"])([^'"`]+?)\1/g;

// Prepare flatten keys pour i18n integrity.test.
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

// Prepare keys for locale pour i18n integrity.test.
const keysForLocale = (locale: string): Set<string> => {
  const merged = new Set<string>();
  Object.entries(localeModules)
    .filter(([p]) => p.includes(`/locales/${locale}/`))
    .forEach(([, tree]) => flattenKeys(tree).forEach((k) => merged.add(k)));
  return merged;
};

const resolves = (set: Set<string>, key: string): boolean =>
  set.has(key) || PLURAL_SUFFIXES.some((suffix) => set.has(key + suffix));

const fr = keysForLocale("fr");
const en = keysForLocale("en");

// Prepare literal references pour i18n integrity.test.
const literalReferences = (): string[] => {
  const refs = new Set<string>();
  Object.entries(sourceModules).forEach(([file, source]) => {
    if (file.includes(".test.") || file.includes("/i18n/")) return;
    for (const match of source.matchAll(TRANSLATION_KEY_PATTERN)) {
      const key = match[2];
      if (!key.includes(" ") && /[a-zA-Z]/.test(key) && key.includes("."))
        refs.add(key);
    }
  });
  return [...refs].sort();
};

describe("i18n integrity", () => {
  it("keeps fr and en at strict key parity across all namespaces", () => {
    const missingInEn = [...fr].filter((k) => !en.has(k)).sort();
    const missingInFr = [...en].filter((k) => !fr.has(k)).sort();
    expect({ missingInEn, missingInFr }).toEqual({
      missingInEn: [],
      missingInFr: [],
    });
  });

  it("defines every literal t() reference in both locales", () => {
    const references = literalReferences();
    const missing = references.flatMap((reference) => {
      const out: string[] = [];
      if (!resolves(fr, reference)) out.push(`fr:${reference}`);
      if (!resolves(en, reference)) out.push(`en:${reference}`);
      return out;
    });
    expect(missing).toEqual([]);
  });
});
