// Hook frontend : synchronise les notifications WebSocket et les caches metier.

import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { tokenManager } from "../../../utils/tokenManager/tokenManager";
import { useAuth } from "../../auth/useAuth";
import { PRESENCE_VIEW_PERMISSIONS } from "../../user/useOnlinePresence/useOnlinePresence";
import { hasAnyPermission } from "../../../utils/permissions/permissions";
import { APP_ROUTES, QUERY_KEYS } from "../../../utils/constants";
import { notificationSubject } from "../../../utils/notifications/notificationText";
import type {
  IncidentResponse,
  IncidentSummaryResponse,
  PagedResponse,
} from "../../../api/incident";
import {
  IncidentStatus,
  type IncidentStatus as IncidentStatusValue,
} from "../../../api/incident";
import type { PresenceResponse } from "../../../api/notification/types";

type WebSocketNotificationPayload = {
  incidentId?:
    | {
        id?: string;
        status?: string;
        title?: string;
      }
    | string
    | null;
  templateParams?: Record<string, unknown>;
  subject?: string;
  subjectEn?: string;
};

const WS_URL = `${import.meta.env.VITE_API_BASE_URL || ""}/api/v1/notificationService/ws`;

// Extrait l'identifiant incident depuis les formats emis par le backend.
const resolveIncidentId = (
  incidentId: WebSocketNotificationPayload["incidentId"],
) => {
  if (!incidentId) return undefined;
  return typeof incidentId === "string" ? incidentId : incidentId.id;
};

// Extrait le statut incident depuis le resume ou les parametres de notification.
const resolveIncidentStatus = (payload: WebSocketNotificationPayload) => {
  const summaryStatus =
    typeof payload.incidentId === "object" && payload.incidentId
      ? payload.incidentId.status
      : undefined;
  const templateStatus = payload.templateParams?.incident_status_code;
  const validStatuses = Object.values(IncidentStatus);
  return [summaryStatus, templateStatus].find(
    (status): status is IncidentStatusValue =>
      typeof status === "string" &&
      validStatuses.includes(status as IncidentStatusValue),
  );
};

// Met a jour le detail incident en cache avant le refetch reseau.
const updateIncidentDetailCache = (
  current: IncidentResponse | undefined,
  incidentId: string,
  status: IncidentStatusValue,
) => {
  if (!current || current.id !== incidentId) return current;
  return { ...current, status };
};

// Met a jour les listes d'incidents en cache avant le refetch reseau.
const updateIncidentListCache = (
  current: PagedResponse<IncidentSummaryResponse> | undefined,
  incidentId: string,
  status: IncidentStatusValue,
) => {
  if (!current?.content) return current;
  return {
    ...current,
    content: current.content.map((incident) =>
      incident.id === incidentId ? { ...incident, status } : incident,
    ),
  };
};

// Indique si le navigateur peut afficher ou demander les notifications bureau.
const desktopNotificationsAreAvailable = () =>
  "Notification" in window &&
  (Notification.permission === "default" ||
    Notification.permission === "granted");

// Affiche une alerte bureau apres validation explicite de la permission.
const showDesktopNotification = async ({
  title,
  body,
  onClick,
}: {
  title: string;
  body: string;
  onClick: () => void;
}) => {
  if (!desktopNotificationsAreAvailable()) return;

  const permission =
    Notification.permission === "default"
      ? await Notification.requestPermission().catch((error) => {
          console.debug(
            "Demande de notification bureau refusee par le navigateur",
            error,
          );
          return "denied" as NotificationPermission;
        })
      : Notification.permission;

  if (permission !== "granted") return;

  const notification = new Notification(title, {
    body,
    icon: "/Img/FinstarLogo.png",
    // Reste affichee jusqu'a interaction.
    requireInteraction: true,
  });
  notification.onclick = () => {
    window.focus();
    onClick();
    notification.close();
  };
};

// Maintient la connexion WebSocket des notifications utilisateur.
export const useNotificationWebSocket = () => {
  const clientRef = useRef<Client | null>(null);
  const queryClient = useQueryClient();
  const { user, hasPermission } = useAuth();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const canViewPresence = hasAnyPermission(
    hasPermission,
    PRESENCE_VIEW_PERMISSIONS,
  );

  useEffect(() => {
    if ("Notification" in window && Notification.permission === "default") {
      Notification.requestPermission();
    }

    if (!user?.username) return;

    const token = tokenManager.getToken();
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      // Les battements permettent de detecter une connexion morte (proxy,
      // veille) et de declencher la reconnexion automatique.
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      // Chaque (re)connexion repart avec le token courant, pas celui du montage.
      beforeConnect: () => {
        const freshToken = tokenManager.getToken();
        if (freshToken) {
          client.connectHeaders = { Authorization: `Bearer ${freshToken}` };
        }
      },
      onConnect: () => {
        console.debug("WebSocket connecte");

        // Rattrape les notifications emises pendant une eventuelle deconnexion.
        queryClient.invalidateQueries({
          queryKey: QUERY_KEYS.NOTIFICATIONS.ALL,
        });

        // Presence temps reel : le backend diffuse la liste des connectes.
        if (canViewPresence) {
          client.subscribe("/topic/presence", (message) => {
            try {
              const payload = JSON.parse(
                message.body,
              ) as Partial<PresenceResponse>;
              if (Array.isArray(payload.online)) {
                queryClient.setQueryData(
                  QUERY_KEYS.PRESENCE.ONLINE,
                  {
                    online: payload.online,
                    states: payload.states ?? {},
                  },
                );
                queryClient.invalidateQueries({
                  queryKey: ["dashboard", "user-stats"],
                });
              }
            } catch (error) {
              console.error("Erreur parsing presence WebSocket", error);
            }
          });
          queryClient.invalidateQueries({
            queryKey: QUERY_KEYS.PRESENCE.ONLINE,
          });
        }

        client.subscribe(`/topic/notifications/${user.username}`, (message) => {
          console.debug("Notification WebSocket recue :", message.body);

          try {
            const notifData = JSON.parse(
              message.body,
            ) as WebSocketNotificationPayload;
            const incidentId = resolveIncidentId(notifData.incidentId);
            const incidentStatus = resolveIncidentStatus(notifData);

            if (incidentId) {
              if (incidentStatus) {
                queryClient.setQueryData<IncidentResponse | undefined>(
                  QUERY_KEYS.INCIDENTS.DETAIL(incidentId),
                  (current) =>
                    updateIncidentDetailCache(
                      current,
                      incidentId,
                      incidentStatus,
                    ),
                );
                queryClient.setQueriesData<
                  PagedResponse<IncidentSummaryResponse> | undefined
                >({ queryKey: QUERY_KEYS.INCIDENTS.ALL }, (current) =>
                  updateIncidentListCache(current, incidentId, incidentStatus),
                );
              }
              queryClient.invalidateQueries({
                queryKey: QUERY_KEYS.INCIDENTS.ALL,
              });
              queryClient.invalidateQueries({
                queryKey: QUERY_KEYS.INCIDENTS.DETAIL(incidentId),
              });
              queryClient.invalidateQueries({
                queryKey: QUERY_KEYS.INCIDENTS.HISTORY(incidentId),
              });
              queryClient.invalidateQueries({
                queryKey: QUERY_KEYS.DOCUMENTS.BY_INCIDENT(incidentId),
              });
              queryClient.invalidateQueries({ queryKey: ["dashboard"] });
            }

            const audio = new Audio("/sounds/notification.mp3");
            audio.play().catch((error) => {
              console.debug(
                "Son de notification bloque par les regles d'autoplay du navigateur",
                error,
              );
            });

            if (
              document.hidden &&
              desktopNotificationsAreAvailable()
            ) {
              void showDesktopNotification({
                title: t("notifications.desktop.title"),
                body:
                  notificationSubject(notifData, i18n.language) ||
                  t("notifications.desktop.defaultBody"),
                onClick: () =>
                  navigate(
                    incidentId
                      ? APP_ROUTES.INCIDENTS_DETAIL(incidentId)
                      : APP_ROUTES.NOTIFICATIONS,
                  ),
              });
            }
          } catch (error) {
            console.error("Erreur parsing notification WebSocket", error);
          }

          queryClient.invalidateQueries({
            queryKey: QUERY_KEYS.NOTIFICATIONS.ALL,
          });
          queryClient.invalidateQueries({
            queryKey: QUERY_KEYS.NOTIFICATIONS.BY_STATUS("PENDING"),
          });
          queryClient.invalidateQueries({
            queryKey: QUERY_KEYS.NOTIFICATIONS.BY_RECIPIENT(user.username),
          });
        });
      },
      onStompError: (frame) => {
        console.error("Erreur STOMP WebSocket :", frame.headers["message"]);
        console.error("Details WebSocket :", frame.body);
      },
      onWebSocketError: (event) => {
        console.error("Erreur de connexion WebSocket :", event);
      },
    });

    client.activate();
    clientRef.current = client;

    // Au retour sur l'onglet, force la reconnexion si le lien est mort et
    // recharge les notifications manquees.
    const handleVisibilityChange = () => {
      if (document.visibilityState !== "visible") return;
      if (!client.connected) {
        client.deactivate().then(() => client.activate());
      }
      queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.NOTIFICATIONS.ALL,
      });
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      client.deactivate();
      console.debug("WebSocket deconnecte");
    };
  }, [
    user?.username,
    canViewPresence,
    queryClient,
    navigate,
    t,
    i18n.language,
  ]);
};
