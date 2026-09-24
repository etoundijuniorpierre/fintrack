// Composant React : porte l'interface de notification detail modale.

import { memo } from "react";
import { Descriptions, Tag, Button } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import Modal from "../../../../components/ui/Modal/Modal";
import type { NotificationResponse } from "../../../../api/notification";
import {
  NOTIFICATION_TYPE_COLORS,
  NOTIFICATION_STATUS_COLORS,
} from "../../../../api/notification/types";
import { formatDateTime } from "../../../../utils/formatters/formatters";
import {
  notificationSubject,
  notificationContent,
} from "../../../../utils/notifications/notificationText";
import { resourceNavigation } from "../../../../utils/navigation";
import { incidentPathIdentifier } from "../../../../utils/navigation/incidents/incidents";
import styles from "./NotificationDetailModal.module.scss";

// Definit les informations utilisateur affichees dans le detail d'une notification.
interface ConcernedUser {
  role: string;
  username: string;
  fullName: string;
  email: string;
}

// Definit les proprietes attendues par le composant NotificationDetailModal.
interface NotificationDetailModalProps {
  open: boolean;
  notification: NotificationResponse | null;
  onClose: () => void;
}

// Recupere un parametre texte depuis le payload enrichi de notification.
const readTextParam = (
  params: Record<string, unknown> | undefined,
  key: string,
): string => {
  const value = params?.[key];
  return typeof value === "string" ? value : "";
};

// Normalise les utilisateurs concernes recus depuis le backend.
const getConcernedUsers = (
  notification: NotificationResponse,
): ConcernedUser[] => {
  const params = notification.templateParams;
  const rawUsers = params?.concerned_users;
  if (Array.isArray(rawUsers)) {
    return rawUsers
      .filter(
        (item): item is Record<string, unknown> =>
          Boolean(item) && typeof item === "object",
      )
      .map((item) => ({
        role: typeof item.role === "string" ? item.role : "user",
        username: typeof item.username === "string" ? item.username : "",
        fullName: typeof item.fullName === "string" ? item.fullName : "",
        email: typeof item.email === "string" ? item.email : "",
      }))
      .filter((user) => user.username || user.fullName || user.email);
  }

  return ["recipient", "creator", "assignee", "sender"]
    .map((role) => ({
      role,
      username: readTextParam(params, `${role}_username`),
      fullName: readTextParam(
        params,
        role === "sender" ? "sender_name" : `${role}_full_name`,
      ),
      email: readTextParam(params, `${role}_email`),
    }))
    .filter((user) => user.username || user.fullName || user.email);
};

// Formate un utilisateur concerne pour une lecture rapide dans la modale.
const formatConcernedUser = (user: ConcernedUser): string => {
  return user.fullName || user.username || user.email;
};

// Rend le composant NotificationDetailModal pour l'interface notification detail modale.
const NotificationDetailModal = memo(
  ({ open, notification, onClose }: NotificationDetailModalProps) => {
    const { t, i18n } = useTranslation();
    const navigate = useNavigate();

    if (!notification) return null;

    const noValue = t("notifications.noValue");
    // Formate un horodatage facultatif pour l'interface.
    const formatOptionalDateTime = (value?: string) =>
      value ? formatDateTime(value) : noValue;
    const concernedUsers = getConcernedUsers(notification);
    const recipientName =
      concernedUsers.find((user) => user.role === "recipient")?.fullName ||
      concernedUsers.find((user) => user.role === "recipient")?.username ||
      notification.recipient;
    const agencyName =
      (i18n.language.startsWith("en")
        ? readTextParam(notification.templateParams, "agency_name_en")
        : readTextParam(notification.templateParams, "agency_name")) ||
      readTextParam(notification.templateParams, "agency_name") ||
      noValue;

    // Ouvre l'incident associe a la notification courante.
    const handleViewIncident = () => {
      if (notification.incidentId?.id) {
        resourceNavigation.navigateToResourceDetail(
          navigate,
          "INCIDENT",
          incidentPathIdentifier(notification.incidentId),
          { onClose },
        );
      }
    };

    const getStatusColor = (status: string) => {
      switch (status) {
        case "PENDING":
          return NOTIFICATION_STATUS_COLORS.PENDING;
        case "SENT":
          return NOTIFICATION_STATUS_COLORS.SENT;
        case "FAILED":
          return NOTIFICATION_STATUS_COLORS.FAILED;
        case "READ":
          return NOTIFICATION_STATUS_COLORS.READ;
        default:
          return "default";
      }
    };

    const getTypeColor = (type: string) => {
      switch (type) {
        case "EMAIL":
          return NOTIFICATION_TYPE_COLORS.EMAIL;
        case "INTERNAL":
          return NOTIFICATION_TYPE_COLORS.INTERNAL;
        default:
          return "default";
      }
    };

    return (
      <Modal
        open={open}
        title={t("notifications.detail.title")}
        onClose={onClose}
        cancelText={t("notifications.close")}
        footer={true}
        width={600}
        destroyOnHidden
      >
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label={t("notifications.detail.recipient")}>
            {recipientName}
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.subject")}>
            <strong>
              {notificationSubject(notification, i18n.language) || noValue}
            </strong>
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.status")}>
            <Tag color={getStatusColor(notification.status)}>
              {t(`notifications.status.${notification.status}`)}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.type")}>
            <Tag color={getTypeColor(notification.type)}>
              {t(`notifications.type.${notification.type}`)}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.incident")}>
            {notification.incidentId?.reference && (
              <Tag>{notification.incidentId.reference}</Tag>
            )}
            {notification.incidentId?.title || noValue}
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.agency")}>
            {agencyName}
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.message")}>
            <div className={styles.messageContent}>
              {notificationContent(notification, i18n.language) || noValue}
            </div>
          </Descriptions.Item>
          {concernedUsers.length > 0 && (
            <Descriptions.Item label={t("notifications.detail.concernedUsers")}>
              <div className={styles.concernedUsers}>
                {concernedUsers.map((user) => (
                  <div
                    key={`${user.role}-${user.username || user.email}`}
                    className={styles.concernedUser}
                  >
                    <span className={styles.concernedUserRole}>
                      {t(`notifications.detail.userRoles.${user.role}`, {
                        defaultValue: user.role,
                      })}
                    </span>
                    <span>{formatConcernedUser(user)}</span>
                  </div>
                ))}
              </div>
            </Descriptions.Item>
          )}
          <Descriptions.Item label={t("notifications.detail.sentAt")}>
            {formatOptionalDateTime(notification.sentAt)}
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.retryCount")}>
            {notification.retryCount ?? 0}
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.retryAt")}>
            {formatOptionalDateTime(
              notification.nextRetry ?? notification.retryAt,
            )}
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.createdAt")}>
            {formatDateTime(notification.createdAt)}
          </Descriptions.Item>
          <Descriptions.Item label={t("notifications.detail.updatedAt")}>
            {formatDateTime(notification.updatedAt)}
          </Descriptions.Item>
          {notification.modifiedBy && (
            <Descriptions.Item label={t("notifications.detail.modifiedBy")}>
              {notification.modifiedBy}
            </Descriptions.Item>
          )}
        </Descriptions>

        {notification.incidentId?.id && (
          <div className={styles.actions}>
            <Button type="primary" onClick={handleViewIncident}>
              {t("notifications.detail.viewIncident")}
            </Button>
          </div>
        )}
      </Modal>
    );
  },
);

NotificationDetailModal.displayName = "NotificationDetailModal";

export default NotificationDetailModal;
