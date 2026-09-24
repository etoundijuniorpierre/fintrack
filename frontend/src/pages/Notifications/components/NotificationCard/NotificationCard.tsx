// Carte individuelle presentant les details d'une notification.

import { memo, useCallback } from "react";
import { Button } from "antd";
import { useTranslation } from "react-i18next";
import type { NotificationResponse } from "../../../../api/notification";
import {
  NOTIFICATION_TYPE_COLORS,
  NOTIFICATION_STATUS_COLORS,
} from "../../../../api/notification";
import { Card, StatusTag } from "../../../../components/ui";
import { formatDate } from "../../../../utils/formatters/formatters";
import { notificationSubject } from "../../../../utils/notifications/notificationText";
import styles from "./NotificationCard.module.scss";

// Centralise la logique d'interface liee a notification card props.
export interface NotificationCardProps {
  notification: NotificationResponse;
  onClick: (notification: NotificationResponse) => void;
  onMarkSent: (id: string) => void;
  onMarkFailed: (id: string) => void;
  canManage: boolean;
}

// Rend le composant NotificationCard.
const NotificationCard = memo(
  ({
    notification,
    onClick,
    onMarkSent,
    onMarkFailed,
    canManage,
  }: NotificationCardProps) => {
    const { t, i18n } = useTranslation();

    // Traite l'ouverture de la carte.
    const handleCardClick = useCallback(() => {
      onClick(notification);
    }, [onClick, notification]);

    // Traite le marquage comme envoye.
    const handleMarkSent = useCallback(
      (e: React.MouseEvent) => {
        e.stopPropagation();
        onMarkSent(notification.id);
      },
      [onMarkSent, notification.id],
    );

    // Traite le marquage en echec.
    const handleMarkFailed = useCallback(
      (e: React.MouseEvent) => {
        e.stopPropagation();
        onMarkFailed(notification.id);
      },
      [onMarkFailed, notification.id],
    );

    const typeBadge = (
      <StatusTag
        status={notification.type}
        colorMap={NOTIFICATION_TYPE_COLORS}
        label={t(`notifications.type.${notification.type}`)}
      />
    );

    const statusBadge = (
      <StatusTag
        status={notification.status}
        colorMap={NOTIFICATION_STATUS_COLORS}
        label={t(`notifications.status.${notification.status}`)}
      />
    );

    return (
      <Card
        className={styles.notificationCard}
        extra={typeBadge}
        onClick={handleCardClick}
      >
        <div className={styles.cardBody}>
          <div className={styles.field}>
            <span className={styles.fieldLabel}>
              {t("notifications.table.recipient")}
            </span>
            <span className={styles.fieldValue}>{notification.recipient}</span>
          </div>

          <div className={styles.field}>
            <span className={styles.fieldLabel}>
              {t("notifications.table.subject")}
            </span>
            <span className={styles.fieldValue}>
              {notificationSubject(notification, i18n.language) || "-"}
            </span>
          </div>

          <div className={styles.field}>
            <span className={styles.fieldLabel}>
              {t("notifications.table.incident")}
            </span>
            <span className={styles.fieldValue}>
              {notification.incidentId?.title ?? "-"}
            </span>
          </div>

          <div className={styles.field}>
            <span className={styles.fieldLabel}>
              {t("notifications.table.status")}
            </span>
            <span className={styles.fieldValue}>{statusBadge}</span>
          </div>

          <div className={styles.field}>
            <span className={styles.fieldLabel}>
              {t("notifications.table.sentAt")}
            </span>
            <span className={styles.fieldValue}>
              {notification.sentAt ? formatDate(notification.sentAt) : "-"}
            </span>
          </div>

          <div className={styles.field}>
            <span className={styles.fieldLabel}>
              {t("notifications.table.retryCount")}
            </span>
            <span className={styles.fieldValue}>
              {notification.retryCount ?? 0}
            </span>
          </div>

          {canManage && notification.status === "PENDING" && (
            <div className={styles.actions}>
              <Button
                size="small"
                type="primary"
                aria-label={t("notifications.actions.markSent")}
                onClick={handleMarkSent}
              >
                {t("notifications.actions.markSent")}
              </Button>
              <Button
                size="small"
                danger
                aria-label={t("notifications.actions.markFailed")}
                onClick={handleMarkFailed}
              >
                {t("notifications.actions.markFailed")}
              </Button>
            </div>
          )}
        </div>
      </Card>
    );
  },
);

NotificationCard.displayName = "NotificationCard";

export default NotificationCard;
