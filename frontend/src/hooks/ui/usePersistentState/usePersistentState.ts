// Hook React pour maintenir un etat persistant en stockage local.

import { useEffect, useState } from "react";

// Persiste une valeur React dans le stockage local.
export const usePersistentState = <T extends string>(
  key: string,
  initialValue: T,
  allowedValues?: readonly T[],
): [T, (value: T) => void] => {
  const [value, setValue] = useState<T>(() => {
    const storedValue = localStorage.getItem(key) as T | null;
    if (
      storedValue &&
      (!allowedValues || allowedValues.includes(storedValue))
    ) {
      return storedValue;
    }
    return initialValue;
  });

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    localStorage.setItem(key, value);
  }, [key, value]);

  return [value, setValue];
};
