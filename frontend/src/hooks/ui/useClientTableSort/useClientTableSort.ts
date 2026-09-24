// Hook frontend : applique les mêmes raccourcis et clics de tri aux tableaux locaux.

import { useCallback, useMemo } from "react";
import { useTableSort } from "../useTableSort/useTableSort";
import {
  controlledTableSorter,
  createTableSortPresets,
  readTableSort,
  sortTableRecords,
  type SortDirection,
  type TableSortMode,
} from "../../../utils/table/sorting/tableSorting";

type TableSortAccessor<T> = (record: T) => unknown;
const EMPTY_SORT_ACCESSORS: Partial<
  Record<string, TableSortAccessor<object>>
> = {};

// Trie toute la collection locale avant pagination et expose les propriétés des colonnes.
export const useClientTableSort = <T extends object>(
  storageKey: string,
  records: readonly T[],
  alphabeticalField: string,
  seniorityField: string,
  accessors?: Partial<Record<string, TableSortAccessor<T>>>,
) => {
  const presets = useMemo(
    () => createTableSortPresets(alphabeticalField, seniorityField),
    [alphabeticalField, seniorityField],
  );
  const {
    field,
    order,
    activePreset,
    selectPreset,
    handleColumnSortChange,
  } = useTableSort<TableSortMode>(storageKey, presets, "alphabetical");
  const effectiveAccessors =
    accessors ??
    (EMPTY_SORT_ACCESSORS as Partial<Record<string, TableSortAccessor<T>>>);
  const data = useMemo(
    () => sortTableRecords(records, field, order, effectiveAccessors),
    [records, field, order, effectiveAccessors],
  );
  const columnSorter = useCallback(
    (columnField: string) =>
      controlledTableSorter(columnField, field, order as SortDirection),
    [field, order],
  );
  const onChange = useCallback(
    (
      _pagination: unknown,
      _filters: unknown,
      sorter: Parameters<typeof readTableSort<T>>[0],
    ) => {
      const next = readTableSort(sorter);
      handleColumnSortChange(next.field, next.order);
    },
    [handleColumnSortChange],
  );

  return {
    data,
    presets,
    activePreset,
    selectPreset,
    columnSorter,
    onChange,
  };
};
