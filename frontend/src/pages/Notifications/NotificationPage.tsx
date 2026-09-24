// Page de consultation et de gestion des notifications.

import { memo, useMemo, useState, useCallback } from "react";
import { Button, Tag, Space, Checkbox, Pagination, Spin, Empty } from "antd";
import { useTranslation } from "react-i18next";
import {
  useNotificationsByRecipientPage,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
  useBulkDeleteNotifications,
} from "../../hooks/notification";
import { useAuth } from "../../hooks/auth/useAuth";
import type { NotificationResponse } from "../../api/notification";
import {
  NOTIFICATION_TYPE_COLORS,
  NOTIFICATION_STATUS_COLORS,
} from "../../api/notification/types";
import { formatDateTime } from "../../utils/formatters/formatters";
import {
  notificationSubject,
  notificationContent,
} from "../../utils/notifications/notificationText";
import {
  DEFAULT_PAGE_SIZE
} from "../../utils/table/pagination/paginationConfig";
import { PageLoader } from "../../components";
import { PageHeader } from "../../components/ui";
import { PageContainer, SectionCard } from "../../components/Layout";
import { PERMISSIONS } from "../../utils/permissions/permissions";
import NotificationDetailModal from "./components/NotificationDetailModal/NotificationDetailModal";
import styles from "./NotificationPage.module.scss";

// Determine statut color e partir du contexte fourni.
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

// Determine type color e partir du contexte fourni.
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

// Rend le composant NotificationPage pour l'interface notification page.
const NotificationPage = memo(() => {
  const { t, i18n } = useTranslation();
  const { hasPermission, user } = useAuth();
  const canManageNotifications = hasPermission(PERMISSIONS.NOTIFICATION.MANAGE);

  const [paginationState, setPaginationState] = useState({
    current: 1,
    pageSize: DEFAULT_PAGE_SIZE,
  });

  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [selectedNotification, setSelectedNotification] =
    useState<NotificationResponse | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const queryParams = useMemo(
    () => ({
      page: paginationState.current - 1,
      size: paginationState.pageSize,
      sort: "createdAt,desc",
    }),
    [paginationState],
  );

  // Page strictement personnelle : le serveur limite le payload au destinataire courant.
  const {
    data,
    isLoading,
    isFetching,
    isError,
  } = useNotificationsByRecipientPage(user?.username, queryParams);
  const { mutate: markAsRead } = useMarkNotificationRead();
  const { mutate: markAllAsRead, isPending: isMarkingAllRead } =
    useMarkAllNotificationsRead();
  const { mutate: bulkDelete } = useBulkDeleteNotifications();

  const dataSource = data?.content ?? [];
  const totalNotifications = data?.totalElements ?? 0;
  const isPageLoading = isLoading || isFetching;

  // Bornage direct en cours de rendu (evite les cascading renders et l'avertissement React 19).
  // Traite l'ouverture de la notification.
  const handleNotificationClick = (notification: NotificationResponse) => {
    if (notification.status !== "READ") {
      markAsRead(notification.id);
    }
    setSelectedNotification(notification);
    setIsModalOpen(true);
  };

  // Traite la fermeture de la modale.
  const handleCloseModal = useCallback(() => {
    setIsModalOpen(false);
    setSelectedNotification(null);
  }, []);

  // Traite la selection de tous les perimetres.
  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedIds(dataSource.map((n) => n.id));
    } else {
      setSelectedIds([]);
    }
  };

  // Traite la selection.
  const handleSelect = (id: string, checked: boolean) => {
    if (checked) {
      setSelectedIds((prev) => [...prev, id]);
    } else {
      setSelectedIds((prev) => prev.filter((i) => i !== id));
    }
  };

  // Traite la suppression de la selection.
  const handleDeleteSelected = () => {
    if (selectedIds.length > 0) {
      bulkDelete(selectedIds, {
        onSuccess: () => setSelectedIds([]),
      });
    }
  };

  // Charge la page demandee et abandonne la selection propre a la page precedente.
  const handlePageChange = useCallback((page: number, pageSize: number) => {
    setSelectedIds([]);
    setPaginationState((previous) => ({
      current: pageSize !== previous.pageSize ? 1 : page,
      pageSize,
    }));
  }, []);

  return (
    <PageContainer>
      <PageHeader
        title={t("notifications.pageTitle")}
        subtitle={t("notifications.pageSubtitle")}
      />

      {isError ? (
        <PageLoader isLoading={false} isError={true}>
          <span />
        </PageLoader>
      ) : (
        <SectionCard noPadding>
          <div className={styles.toolbar}>
            {canManageNotifications && (
              <Space>
                <Checkbox
                  checked={
                    selectedIds.length > 0 &&
                    selectedIds.length === dataSource.length
                  }
                  indeterminate={
                    selectedIds.length > 0 &&
                    selectedIds.length < dataSource.length
                  }
                  onChange={(e) => handleSelectAll(e.target.checked)}
                >
                  {t("notifications.selectAll")}
                </Checkbox>
                {selectedIds.length > 0 && (
                  <Button danger type="primary" onClick={handleDeleteSelected}>
                    {t("notifications.deleteSelected")} ({selectedIds.length})
                  </Button>
                )}
              </Space>
            )}
            <Button
              onClick={() => markAllAsRead()}
              disabled={
                !dataSource.some(
                  (notification) => notification.status !== "READ",
                )
              }
              loading={isMarkingAllRead}
            >
              {t("notifications.markAllRead")}
            </Button>
          </div>

          <Spin spinning={isPageLoading}>
            <div
              className={styles.listContainer}
              role="list"
              aria-busy={isPageLoading}
              aria-live="polite"
            >
              {dataSource.map((item) => (
                <div key={item.id} role="listitem" className={styles.listItem}>
                  <div className={styles.itemWrapper}>
                    {canManageNotifications && (
                      <div className={styles.checkboxWrapper}>
                        <Checkbox
                          checked={selectedIds.includes(item.id)}
                          onChange={(e) =>
                            handleSelect(item.id, e.target.checked)
                          }
                          onClick={(e) => e.stopPropagation()}
                        />
                      </div>
                    )}
                    <div
                      className={styles.itemContent}
                      onClick={() => handleNotificationClick(item)}
                    >
                      <div className={styles.itemTitle}>
                        <Space>
                          <span
                            className={
                              item.status === "PENDING"
                                ? styles.subjectPending
                                : styles.subjectNormal
                            }
                          >
                            {notificationSubject(item, i18n.language) ||
                              t("notifications.defaultSubject")}
                          </span>
                          <Tag color={getStatusColor(item.status)}>
                            {t(`notifications.status.${item.status}`)}
                          </Tag>
                          <Tag color={getTypeColor(item.type)}>
                            {t(`notifications.type.${item.type}`)}
                          </Tag>
                        </Space>
                      </div>
                      <div className={styles.itemDescription}>
                        {notificationContent(item, i18n.language)}
                      </div>
                    </div>
                    <span className={styles.date}>
                      {formatDateTime(item.createdAt)}
                    </span>
                  </div>
                </div>
              ))}
              {!isPageLoading && dataSource.length === 0 && (
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={t("notifications.empty")}
                />
              )}
            </div>
          </Spin>
          <div className={styles.paginationWrapper}>
            <Pagination
              current={paginationState.current}
              pageSize={paginationState.pageSize}
              total={totalNotifications}
              pageSizeOptions={[10, 20, 50, 100]}
              showSizeChanger
              showTotal={(total, range) => `${range[0]}-${range[1]} / ${total}`}
              hideOnSinglePage={false}
              align="center"
              onChange={handlePageChange}
            />
          </div>
        </SectionCard>
      )}

      <NotificationDetailModal
        open={isModalOpen}
        notification={selectedNotification}
        onClose={handleCloseModal}
      />
    </PageContainer>
  );
});

NotificationPage.displayName = "NotificationPage";

export default NotificationPage;
