// Hook de portee persistante : conserve une vue autorisee selon les permissions disponibles.

import { useEffect, useMemo } from "react";
import { usePersistentState } from "../usePersistentState/usePersistentState";
import {
  getPreferredView,
  type ScopeView,
  type ScopeViewOption,
} from "../../../utils/permissions/permissions";

// Retourne la vue active et son mutateur synchronise avec le stockage local.
export const usePersistentScopeView = (
  storageKey: string,
  availableViews: readonly ScopeViewOption[],
  fallbackOrder: readonly ScopeView[],
): [ScopeView, (value: ScopeView) => void] => {
  const preferredView = useMemo(() => {
    const stored =
      typeof window !== "undefined" ? localStorage.getItem(storageKey) : null;
    return getPreferredView(availableViews, stored, fallbackOrder);
  }, [availableViews, storageKey, fallbackOrder]);

  const allowedValues = useMemo(
    () => availableViews.map((option) => option.value),
    [availableViews],
  );

  const [view, setView] = usePersistentState<ScopeView>(
    storageKey,
    preferredView,
    allowedValues,
  );

  // Restaure une vue valide lorsque les permissions disponibles changent.
  useEffect(() => {
    if (allowedValues.length > 0 && !allowedValues.includes(view)) {
      setView(preferredView);
    }
  }, [allowedValues, preferredView, setView, view]);

  return [view, setView];
};
