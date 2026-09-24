// Store frontend : centralise l'etat de theme store.

import { create } from "zustand";
import { persist } from "zustand/middleware";

// Centralise la logique d'interface liee a theme mode.
export type ThemeMode = "light" | "dark";

// Definit l'etat interne du composant Theme.
interface ThemeState {
  mode: ThemeMode;
  toggleTheme: () => void;
  setTheme: (mode: ThemeMode) => void;
}

// Centralise le theme visuel choisi par l'utilisateur.
export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      mode: "dark",
      toggleTheme: () =>
        set((state) => ({ mode: state.mode === "light" ? "dark" : "light" })),
      setTheme: (mode) => set({ mode }),
    }),
    {
      name: "fintrack-theme-storage",
    },
  ),
);
