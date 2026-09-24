// Magasin d'etat (Zustand) pour l'authentification et la session.

import { create } from "zustand";
import { persist } from "zustand/middleware";
import { tokenManager } from "../../utils/tokenManager/tokenManager";
import { STORAGE_KEYS } from "../../utils/constants";

// Centralise la logique d'interface liee a auth utilisateur.
export interface AuthUser {
  id: string;
  username: string;
  roles: string[];
  permissions: string[];
  serviceId?: string;
  agencyId?: string;
  managedServiceIds?: string[];
  managedAgencyId?: string;
  isActive?: boolean;
  /** Vrai tant que l'utilisateur n'a pas défini son mot de passe définitif. */
  isFirstLogin?: boolean;
}

// Definit l'etat interne du composant Auth.
interface AuthState {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (token: string, user: AuthUser) => void;
  logout: () => void;
  /** Marque la première connexion comme terminée (mot de passe défini). */
  completeFirstLogin: () => void;
  hasRole: (role: string) => boolean;
  hasPermission: (permission: string) => boolean;
}

// Expose le store persistant d'authentification.
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      isAuthenticated: false,

      login: (token: string, user: AuthUser) => {
        tokenManager.setToken(token);
        set({ user, isAuthenticated: true });
      },

      logout: () => {
        tokenManager.removeToken();
        set({ user: null, isAuthenticated: false });
      },

      completeFirstLogin: () => {
        const { user } = get();
        if (typeof window !== "undefined") {
          sessionStorage.removeItem(STORAGE_KEYS.FIRST_LOGIN);
        }
        if (user) {
          set({ user: { ...user, isFirstLogin: false } });
        }
      },

      hasRole: (role: string): boolean => {
        const { user } = get();
        return (
          user?.roles?.some((r) => r.toUpperCase() === role.toUpperCase()) ??
          false
        );
      },

      hasPermission: (permission: string): boolean => {
        const { user } = get();
        return (
          user?.permissions?.some(
            (p) => p.toUpperCase() === permission.toUpperCase(),
          ) ?? false
        );
      },
    }),
    {
      name: STORAGE_KEYS.USER,

      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);
