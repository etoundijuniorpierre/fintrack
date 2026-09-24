// Utilitaire de tableau : harmonise les directions, comparateurs et événements de tri.

import type { SortOrder, SorterResult } from "antd/es/table/interface";

export type SortDirection = "asc" | "desc";

export interface SortPresetConfig {
  field: string;
  order: SortDirection;
}

export type TableSortMode = "alphabetical" | "seniority";

// Crée les deux raccourcis d'ordre communs à partir des champs propres au tableau.
export const createTableSortPresets = (
  alphabeticalField: string,
  seniorityField: string,
): Record<TableSortMode, SortPresetConfig> => ({
  alphabetical: { field: alphabeticalField, order: "asc" },
  seniority: { field: seniorityField, order: "desc" },
});

export const TABLE_SORT_DIRECTIONS: SortOrder[] = [
  "ascend",
  "descend",
  "ascend",
];

const tableCollator = new Intl.Collator(undefined, {
  sensitivity: "base",
  numeric: true,
});

// Convertit la direction métier vers la direction attendue par Ant Design.
export const toTableSortOrder = (order?: SortDirection): SortOrder | undefined =>
  order === "asc" ? "ascend" : order === "desc" ? "descend" : undefined;

// Extrait le champ et la direction du dernier clic sur un en-tête de tableau.
export const readTableSort = <T extends object>(
  sorter: SorterResult<T> | SorterResult<T>[],
): { field?: string; order?: SortDirection } => {
  const selected = Array.isArray(sorter) ? sorter[0] : sorter;
  const candidate = selected?.field ?? selected?.columnKey;
  const field = typeof candidate === "string" ? candidate : undefined;
  const order =
    selected?.order === "ascend"
      ? "asc"
      : selected?.order === "descend"
        ? "desc"
        : undefined;
  return { field, order };
};

// Retrouve le mode correspondant au champ actif, indépendamment de sa direction.
export const resolveSortMode = <T extends string>(
  presets: Record<T, SortPresetConfig>,
  field: string,
): T | undefined => {
  for (const [presetKey, config] of Object.entries<SortPresetConfig>(presets)) {
    if (config.field === field) {
      return presetKey as T;
    }
  }
  return undefined;
};

// Compare deux libellés sans tenir compte de la casse ni des accents.
export const compareTableText = (left: unknown, right: unknown): number =>
  tableCollator.compare(String(left ?? ""), String(right ?? ""));

// Compare deux dates en plaçant les valeurs absentes en dernier.
export const compareTableDates = (left: unknown, right: unknown): number => {
  const leftTime = left ? new Date(String(left)).getTime() : Number.NaN;
  const rightTime = right ? new Date(String(right)).getTime() : Number.NaN;
  if (Number.isNaN(leftTime)) return Number.isNaN(rightTime) ? 0 : 1;
  if (Number.isNaN(rightTime)) return -1;
  return leftTime - rightTime;
};

// Compare deux valeurs numériques en plaçant les valeurs absentes en dernier.
export const compareTableNumbers = (left: unknown, right: unknown): number => {
  const leftNumber = Number(left);
  const rightNumber = Number(right);
  if (!Number.isFinite(leftNumber)) return Number.isFinite(rightNumber) ? 1 : 0;
  if (!Number.isFinite(rightNumber)) return -1;
  return leftNumber - rightNumber;
};

type TableSortAccessor<T> = (record: T) => unknown;

// Trie une collection locale complète selon le champ actif et ses éventuels accès imbriqués.
export const sortTableRecords = <T>(
  records: readonly T[],
  field: string,
  order: SortDirection,
  accessors: Partial<Record<string, TableSortAccessor<T>>> = {},
): T[] => {
  const readValue =
    accessors[field] ??
    ((record: T) =>
      field
        .split(".")
        .reduce<unknown>(
          (value, key) =>
            value && typeof value === "object"
              ? (value as Record<string, unknown>)[key]
              : undefined,
          record,
        ));
  const direction = order === "asc" ? 1 : -1;

  return [...records].sort((left, right) => {
    const leftValue = readValue(left);
    const rightValue = readValue(right);
    const comparison =
      typeof leftValue === "number" || typeof rightValue === "number"
        ? compareTableNumbers(leftValue, rightValue)
        : compareTableText(leftValue, rightValue);
    return comparison * direction;
  });
};

// Produit les propriétés contrôlées d'une colonne triable.
export const controlledTableSorter = (
  field: string,
  activeField?: string,
  activeOrder?: SortDirection,
) => ({
  sorter: true as const,
  sortDirections: TABLE_SORT_DIRECTIONS,
  sortOrder:
    activeField === field ? toTableSortOrder(activeOrder) : undefined,
});
