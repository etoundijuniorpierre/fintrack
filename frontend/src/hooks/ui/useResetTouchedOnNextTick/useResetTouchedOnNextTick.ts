// Hook React utilitaire pour reinitialiser les etats d'interaction au tick suivant.

import { useEffect } from "react";

// Repousse la remise a zero d'un etat touched au prochain cycle React.
export const useResetTouchedOnNextTick = (
  setIsTouched: (isTouched: boolean) => void,
  dependencies: readonly unknown[],
): void => {
  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    const resetTouchedId = window.setTimeout(() => setIsTouched(false), 0);
    return () => window.clearTimeout(resetTouchedId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);
};
