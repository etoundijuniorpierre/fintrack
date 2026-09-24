// Liste de cartes de notifications groupees.

import { memo, useCallback } from "react";
import { useTranslation } from "react-i18next";
import type { NotificationResponse } from "../../../../api/notification";
import { EmptyState } from "../../../../components/ui";
import NotificationCard from "../NotificationCard/NotificationCard";
import styles from "./NotificationCardList.module.scss";

// Centralise la logique d'interface liee a notification card list props.
export interface NotificationCardListProps {
  notifications: NotificationResponse[];
  onCardClick: (notification: NotificationResponse) => void;
  onMarkSent: (id: string) => void;
  onMarkFailed: (id: string) => void;
  canManage: boolean;
}

// Rend le composant NotificationCardList.
const NotificationCardList = memo(
  ({
    notifications,
    onCardClick,
    onMarkSent,
    onMarkFailed,
    canManage,
  }: NotificationCardListProps) => {
    const { t } = useTranslation();

    // Traite le marquage comme envoye.
    const handleMarkSent = useCallback(
      (id: string) => {
        onMarkSent(id);
      },
      [onMarkSent],
    );

    // Traite le marquage en echec.
    const handleMarkFailed = useCallback(
      (id: string) => {
        onMarkFailed(id);
      },
      [onMarkFailed],
    );

    if (notifications.length === 0) {
      return (
        <EmptyState
          title={t("common.noData")}
          description={t("notifications.empty")}
        />
      );
    }

    return (
      <div
        className={styles.cardList}
        role="list"
        aria-label={t("notifications.pageTitle")}
      >
        {notifications.map((notification) => (
          <div key={notification.id} role="listitem">
            <NotificationCard
              notification={notification}
              onClick={onCardClick}
              onMarkSent={handleMarkSent}
              onMarkFailed={handleMarkFailed}
              canManage={canManage}
            />
          </div>
        ))}
      </div>
    );
  },
);

NotificationCardList.displayName = "NotificationCardList";

export default NotificationCardList;
