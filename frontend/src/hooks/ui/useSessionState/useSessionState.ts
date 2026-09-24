import { useState, useCallback } from "react";

export const useSessionState = <T>(
  key: string,
  initialValue: T | (() => T),
): [T, (value: React.SetStateAction<T>) => void] => {
  const [value, setValue] = useState<T>(() => {
    try {
      const storedValue = sessionStorage.getItem(key);
      if (storedValue !== null) {
        return JSON.parse(storedValue) as T;
      }
    } catch (e) {
      console.warn(`Failed to parse session storage for key ${key}`, e);
    }
    return initialValue instanceof Function
      ? (initialValue as () => T)()
      : initialValue;
  });

  const setStoredValue = useCallback(
    (newValue: React.SetStateAction<T>) => {
      setValue((prev) => {
        const nextValue =
          newValue instanceof Function
            ? (newValue as (prev: T) => T)(prev)
            : newValue;
        try {
          sessionStorage.setItem(key, JSON.stringify(nextValue));
        } catch (e) {
          console.warn(`Failed to set session storage for key ${key}`, e);
        }
        return nextValue;
      });
    },
    [key],
  );

  return [value, setStoredValue];
};
