// Hook frontend : etat de tri serveur partage entre presets et clics de colonne.

import { useCallback, useMemo } from "react";
import { useSessionState } from "../useSessionState/useSessionState";
import {
  resolveSortMode,
  type SortDirection,
  type SortPresetConfig,
} from "../../../utils/table/sorting/tableSorting";

interface TableSortOptions {
  // Appele apres chaque changement de tri (ex. remise a la page 1).
  onChange?: () => void;
}

/**
 * Tri "column-driven" : un clic d'en-tete choisit colonne et direction
 * (asc/desc), le menu de presets applique un couple predefini et ne reste
 * selectionne que si le tri courant lui correspond exactement.
 */
export const useTableSort = <T extends string>(
  storageKey: string,
  presets: Record<T, SortPresetConfig>,
  defaultPreset: T,
  options?: TableSortOptions,
) => {
  const fallback = presets[defaultPreset];
  const [state, setState] = useSessionState<{
    field?: string;
    order?: SortDirection;
  }>(storageKey, { field: fallback.field, order: fallback.order });

  const field =
    typeof state.field === "string" && state.field
      ? state.field
      : fallback.field;
  const order =
    state.order === "asc" || state.order === "desc"
      ? state.order
      : fallback.order;

  const activePreset = useMemo(
    () => resolveSortMode(presets, field),
    [presets, field],
  );

  const selectPreset = useCallback(
    (preset: T) => {
      const config = presets[preset];
      if (!config) return;
      setState({ field: config.field, order: config.order });
      options?.onChange?.();
    },
    [presets, setState, options],
  );

  const handleColumnSortChange = useCallback(
    (nextField?: string, nextOrder?: SortDirection) => {
      if (!nextField || !nextOrder) return;
      if (nextField === field && nextOrder === order) return;
      setState({ field: nextField, order: nextOrder });
      options?.onChange?.();
    },
    [setState, options, field, order],
  );

  return {
    field,
    order,
    sortParam: `${field},${order}`,
    activePreset,
    selectPreset,
    handleColumnSortChange,
  };
};
