// Hooks d'authentification : connexion, deconnexion, reauthentification et restauration de session.

import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { authApi } from "../../api/user/auth/authApi";
import { useAuthStore } from "../../store/authStore/authStore";
import { tokenManager } from "../../utils/tokenManager/tokenManager";
import { APP_ROUTES, STORAGE_KEYS } from "../../utils/constants";
import type { LoginCredentials } from "../../api/user/types";

// Authentifie l'utilisateur et initialise l'etat de session applicatif.
export const useLogin = () => {
  const { login } = useAuthStore();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (credentials: LoginCredentials) => authApi.login(credentials),
    onSuccess: (data) => {
      // Toute premiere connexion reste limitee au changement de mot de passe.
      const firstLogin = Boolean(data.isFirstLogin);

      // On positionne le drapeau sessionStorage uniquement dans ce cas, sinon on s'assure
      // qu'il n'en reste pas de residuel d'une session precedente.
      if (firstLogin) {
        sessionStorage.setItem(STORAGE_KEYS.FIRST_LOGIN, "true");
      } else {
        sessionStorage.removeItem(STORAGE_KEYS.FIRST_LOGIN);
      }

      login(data.token, {
        id: data.id,
        username: data.username,
        roles: Array.from(data.roles),
        permissions: Array.from(data.permissions),
        serviceId: data.serviceId,
        agencyId: data.agencyId,
        managedServiceIds: data.managedServiceIds,
        managedAgencyId: data.managedAgencyId,
        isFirstLogin: firstLogin,
      });

      navigate(APP_ROUTES.DASHBOARD);
    },
  });
};

// Reauthentifie l'utilisateur pour confirmer une action sensible.
export const useReauth = () => {
  return useMutation({
    mutationFn: (credentials: LoginCredentials) => authApi.login(credentials),
  });
};

// Deconnecte l'utilisateur et nettoie l'etat local de session.
export const useLogout = () => {
  const { logout } = useAuthStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const navigateToLogin = () => {
    sessionStorage.removeItem(STORAGE_KEYS.FIRST_LOGIN);
    logout();
    // La deconnexion navigue cote client : sans purge, le cache TanStack Query
    // survit et la session suivante repart avec les donnees et les permissions
    // de la precedente (listes, tableaux de bord, boutons d'action perimes).
    queryClient.clear();
    navigate(APP_ROUTES.LOGIN);
  };

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSuccess: navigateToLogin,
    onError: navigateToLogin,
  });
};

// Expose l'etat d'authentification courant et les helpers d'autorisation.
export const useAuth = () => {
  const { user, isAuthenticated, hasRole, hasPermission } = useAuthStore();
  return { user, isAuthenticated, hasRole, hasPermission };
};

// Restaure le token d'acces en memoire avant le montage des pages protegees.
export const useSessionBootstrap = () => {
  const { isAuthenticated, login, logout } = useAuthStore();
  const queryClient = useQueryClient();
  const [ready, setReady] = useState(
    () => !isAuthenticated || tokenManager.hasToken(),
  );

  useEffect(() => {
    if (ready) return;

    let cancelled = false;
    (async () => {
      try {
        const data = await authApi.refreshToken();
        if (cancelled) return;
        login(data.token, {
          id: data.id,
          username: data.username,
          roles: Array.from(data.roles),
          permissions: Array.from(data.permissions),
          serviceId: data.serviceId,
          agencyId: data.agencyId,
          managedServiceIds: data.managedServiceIds,
          managedAgencyId: data.managedAgencyId,
          isFirstLogin: Boolean(data.isFirstLogin),
        });
      } catch {
        // Session non restaurable : on repart d'un cache vide, comme a la deconnexion.
        if (!cancelled) {
          logout();
          queryClient.clear();
        }
      } finally {
        if (!cancelled) setReady(true);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [ready, login, logout, queryClient]);

  return ready;
};

// Permet a l'utilisateur de contacter les administrateurs.
export const useContactAdmin = () => {
  return useMutation({
    mutationFn: (request: {
      username: string;
      subject: string;
      message: string;
    }) => authApi.contactAdmin(request),
  });
};
