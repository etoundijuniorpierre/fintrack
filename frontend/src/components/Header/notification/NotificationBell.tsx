// Composant React : porte l'interface de notification bell.

import { memo, useMemo, useEffect, useRef } from "react";
import { Dropdown, Badge, Button, Typography, App } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../hooks/auth/useAuth";
import {
  useNotificationsByRecipient,
  useMarkNotificationRead,
  useNotificationWebSocket,
} from "../../../hooks/notification";
import { APP_ROUTES } from "../../../utils/constants";
import { resourceNavigation } from "../../../utils/navigation";
import { PERMISSIONS } from "../../../utils/permissions/permissions";
import { headerIcons } from "../../../utils/icons/appIcons";
import { formatDate } from "../../../utils/formatters/formatters";
import type { NotificationResponse } from "../../../api/notification";
import {
  notificationSubject,
  notificationContent,
} from "../../../utils/notifications/notificationText";
import clsx from "clsx";
import styles from "./NotificationBell.module.scss";

const { Text } = Typography;

// Son court pour les notifications (situe dans le dossier public/sounds/)
const NOTIFICATION_SOUND_URL = "/sounds/notification.mp3";

// Rend le composant NotificationBell pour l'interface notification bell.
const NotificationBell = () => {
  const { hasPermission, user } = useAuth();
  useNotificationWebSocket();
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { mutate: markAsRead } = useMarkNotificationRead();
  const { notification } = App.useApp();
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const previousCountRef = useRef<number>(0);

  const canViewNotifications =
    hasPermission(PERMISSIONS.NOTIFICATION.VIEW_OWN) ||
    hasPermission(PERMISSIONS.NOTIFICATION.VIEW_ALL) ||
    hasPermission(PERMISSIONS.NOTIFICATION.MANAGE);

  // La cloche est strictement personnelle : on recupere les notifications du
  // destinataire courant, jamais le flux global (un VIEW_ALL verrait sinon tout le systeme).
  const { data: myNotificationsData } = useNotificationsByRecipient(
    user?.username,
  );

  const pendingNotifications: NotificationResponse[] = useMemo(() => {
    if (!myNotificationsData) return [];
    const list = Array.isArray(myNotificationsData)
      ? myNotificationsData
      : (myNotificationsData as unknown as { content?: NotificationResponse[] })
          .content || [];
    // Plus recentes en premier : le toast et le dropdown refletent la derniere arrivee.
    return list
      .filter((n) => n.status === "PENDING")
      .sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
      );
  }, [myNotificationsData]);

  const unreadCount = pendingNotifications.length;

  useEffect(() => {
    // Jouer un son si le nombre de notifications non lues a augmente
    if (unreadCount > previousCountRef.current) {
      if (!audioRef.current) {
        audioRef.current = new Audio(NOTIFICATION_SOUND_URL);
      }
      audioRef.current
        .play()
        .catch((e) => console.warn("Erreur de lecture audio:", e));

      // Afficher une alerte visuelle
      const newNotification = pendingNotifications[0];
      if (newNotification) {
        notification.info({
          message:
            notificationSubject(newNotification, i18n.language) ||
            t("notifications.defaultSubject"),
          description: notificationContent(newNotification, i18n.language),
          placement: "topRight",
        });
      }
    }
    previousCountRef.current = unreadCount;
  }, [unreadCount, pendingNotifications, notification, t, i18n.language]);

  // Traite l'ouverture de la notification.
  const handleNotificationClick = (notification: NotificationResponse) => {
    markAsRead(notification.id);
    if (notification.incidentId?.id) {
      resourceNavigation.navigateToResourceDetail(
        navigate,
        "INCIDENT",
        notification.incidentId.id,
      );
    } else {
      navigate(APP_ROUTES.NOTIFICATIONS);
    }
  };

  // Traite l'action utilisateur liee a consultation tous les perimetres.
  const handleViewAll = () => {
    navigate(APP_ROUTES.NOTIFICATIONS);
  };

  const dropdownContent = (
    <div className={styles.dropdownContainer}>
      <div className={styles.header}>
        <h4 className={styles.title}>{t("notifications.pageTitle")}</h4>
      </div>

      {unreadCount > 0 ? (
        <div className={styles.list}>
          {pendingNotifications.slice(0, 5).map((notif) => (
            <div
              key={notif.id}
              className={styles.item}
              onClick={() => handleNotificationClick(notif)}
            >
              <div className={styles.itemHeader}>
                <span className={styles.subject}>
                  {notificationSubject(notif, i18n.language)}
                </span>
                <span className={styles.date}>
                  {formatDate(notif.createdAt)}
                </span>
              </div>
              <div className={styles.content}>
                {notificationContent(notif, i18n.language)}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className={styles.empty}>
          <Text type="secondary">{t("notifications.empty")}</Text>
        </div>
      )}

      <div className={styles.footer}>
        <Button type="link" onClick={handleViewAll}>
          {t("notifications.viewAll")}
        </Button>
      </div>
    </div>
  );

  if (!canViewNotifications) return null;

  return (
    <div className={styles.bellWrapper}>
      <Dropdown
        popupRender={() => dropdownContent}
        placement="bottomRight"
        trigger={["click"]}
      >
        <Badge count={unreadCount} size="small" offset={[-2, 2]}>
          <Button
            type="text"
            icon={headerIcons.bell}
            className={clsx(
              styles.iconBtn,
              unreadCount > 0 && styles.hasUnread,
            )}
            title={t("layout.header.notifications")}
            aria-label={t("layout.header.notifications")}
            data-testid="notification-bell"
          />
        </Badge>
      </Dropdown>
    </div>
  );
};

export default memo(NotificationBell);
