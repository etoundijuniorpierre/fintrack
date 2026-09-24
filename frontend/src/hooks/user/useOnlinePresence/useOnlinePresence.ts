// Hook frontend : expose la liste temps reel des utilisateurs connectes.

import { useCallback, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { notificationApi } from "../../../api/notification/notificationApi/notificationApi";
import { useAuth } from "../../auth/useAuth";
import { QUERY_KEYS } from "../../../utils/constants";
import {
  PERMISSIONS,
  hasAnyPermission,
} from "../../../utils/permissions/permissions";

// Droits ouvrant la consultation de la presence (memes que le backend).
export const PRESENCE_VIEW_PERMISSIONS = [
  PERMISSIONS.USER.VIEW_ALL,
  PERMISSIONS.USER.VIEW_AGENCY,
  PERMISSIONS.USER.VIEW_SERVICE,
];

// Charge l'etat initial ; le WebSocket alimente ensuite ce cache en temps reel.
export const useOnlinePresence = () => {
  const { hasPermission } = useAuth();
  const canViewPresence = hasAnyPermission(
    hasPermission,
    PRESENCE_VIEW_PERMISSIONS,
  );

  const { data, isLoading } = useQuery({
    queryKey: QUERY_KEYS.PRESENCE.ONLINE,
    queryFn: ({ signal }) => notificationApi.getPresence(signal),
    enabled: canViewPresence,
    // Le WebSocket pousse les changements : pas d'expiration locale.
    staleTime: Infinity,
    // Reconciliation ponctuelle au retour sur l'onglet, sans polling temporel.
    refetchOnWindowFocus: "always",
  });

  const online = useMemo(() => data?.online ?? [], [data?.online]);
  const states = useMemo(() => data?.states ?? {}, [data?.states]);

  const isOnline = useCallback(
    (username?: string) => Boolean(username && online.includes(username)),
    [online],
  );

  const getPresence = useCallback(
    (username?: string) => (username ? states[username] : undefined),
    [states],
  );

  return {
    online,
    states,
    isOnline,
    getPresence,
    canViewPresence,
    isLoading,
  };
};
