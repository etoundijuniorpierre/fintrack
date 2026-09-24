// Page de visualisation des caracteristiques detaillees d'un compte.

import { useParams, useNavigate, useLocation } from "react-router-dom";
import {
  Typography,
  Space,
  Tag,
  Button,
  Popconfirm,
  Divider,
  Tooltip,
  Form,
  Skeleton,
} from "antd";
import { useTranslation } from "react-i18next";
import { userNavigation } from "../../../utils/navigation/users/users";
import {
  useUser,
  useDeleteUser,
  useToggleUserStatus,
} from "../../../hooks/user/useUsers";
import { useState, useCallback, useMemo, memo } from "react";
import { UserForm, PasswordManagement } from "../components";
import { PageLoader } from "../../../components";
import { formatDateTime } from "../../../utils/formatters/formatters";
import { useAuth } from "../../../hooks/auth/useAuth";
import {
  PERMISSIONS,
  INCIDENT_VIEW_PERMISSIONS,
  hasAnyPermission,
  hasPermissionName,
  getEffectivePermissionNames,
} from "../../../utils/permissions/permissions";
import { APP_ROUTES } from "../../../utils/constants";
import { canPerformDestructiveAction } from "../../../utils/user";
import { PageContainer, SectionCard } from "../../../components/Layout";
import { PageHeader } from "../../../components/ui";
import UserAvatar from "../../../components/UserAvatar/UserAvatar";
import { actionIcons, detailIcons } from "../../../utils/icons/appIcons";
import styles from "./View.module.scss";
import type { CreateUserRequest } from "../../../api/user/types";
import { useOnlinePresence } from "../../../hooks/user/useOnlinePresence/useOnlinePresence";

const { Text } = Typography;

// Rend le composant ViewUser pour l'interface view utilisateur.
const ViewUser = () => {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasPermission, user: currentUser } = useAuth();
  const { isOnline } = useOnlinePresence();
  const [isEditing, setIsEditing] = useState(
    () => !!(location.state as { edit?: boolean })?.edit,
  );
  const [form] = Form.useForm<CreateUserRequest>();

  const { data: user, isLoading } = useUser(id);
  const { mutate: deleteUser } = useDeleteUser();
  const { mutate: toggleStatus } = useToggleUserStatus();

  const isProtectedUser = useMemo(
    () =>
      user
        ? hasPermissionName(
            getEffectivePermissionNames(user),
            PERMISSIONS.USER.CREATE_ADMIN,
          )
        : false,
    [user],
  );
  const isViewingOwnAccount = useMemo(
    () => user?.id === currentUser?.id,
    [currentUser?.id, user?.id],
  );
  const viewedUserConnected = useMemo(
    () =>
      isViewingOwnAccount ||
      Boolean(user && isOnline(user.username)),
    [isViewingOwnAccount, isOnline, user],
  );
  const isProtectedFromCurrentUser = useMemo(
    () => isProtectedUser && !isViewingOwnAccount,
    [isProtectedUser, isViewingOwnAccount],
  );
  const canUpdate = useMemo(
    () => hasPermission(PERMISSIONS.USER.UPDATE) && !isProtectedFromCurrentUser,
    [hasPermission, isProtectedFromCurrentUser],
  );
  const canDelete = useMemo(
    () => hasPermission(PERMISSIONS.USER.DELETE) && !isProtectedFromCurrentUser,
    [hasPermission, isProtectedFromCurrentUser],
  );

  const canPerformSelfDestructiveAction = useMemo(
    () => canPerformDestructiveAction(user?.id, currentUser?.id),
    [user?.id, currentUser?.id],
  );

  const canViewIncidents = useMemo(
    () => hasAnyPermission(hasPermission, INCIDENT_VIEW_PERMISSIONS),
    [hasPermission],
  );
  const incidentUserLabel = useMemo(
    () =>
      [user?.firstName, user?.lastName].filter(Boolean).join(" ") ||
      user?.username,
    [user?.firstName, user?.lastName, user?.username],
  );

  // Traite le changement de statut.
  const handleToggleStatus = useCallback(() => {
    if (user) toggleStatus(user.id);
  }, [user, toggleStatus]);

  // Gere la suppression d'un element.
  const handleDelete = useCallback(() => {
    if (user)
      deleteUser(user.id, {
        onSuccess: () => userNavigation.navigateToUsers(navigate),
      });
  }, [user, deleteUser, navigate]);

  // Traite la fin d'edition reussie.
  const handleEditSuccess = useCallback(() => setIsEditing(false), []);
  // Traite le retour a l'ecran precedent.
  const handleBack = useCallback(
    () =>
      isEditing
        ? setIsEditing(false)
        : userNavigation.navigateToUsers(navigate),
    [isEditing, navigate],
  );
  // Traite le passage en edition.
  const handleStartEditing = useCallback(() => setIsEditing(true), []);
  // Annule l'action en cours.
  const handleCancelEditing = useCallback(() => setIsEditing(false), []);

  if (isLoading && !user) {
    return (
      <PageContainer>
        <PageHeader
          title={t("users.form.titles.view")}
          subtitle={id}
          onBack={handleBack}
          backLabel={t("common.back")}
        />
        <div className={styles.mainLayout}>
          <div className={styles.contentSection}>
            <SectionCard
              className={styles.contentCard}
              title={t("users.form.user_info")}
            >
              <Skeleton active paragraph={{ rows: 8 }} />
            </SectionCard>
          </div>
          <div className={styles.sidePanel}>
            <SectionCard className={styles.auditCard}>
              <Skeleton active avatar paragraph={{ rows: 3 }} />
            </SectionCard>
          </div>
        </div>
      </PageContainer>
    );
  }

  if (!user) {
    return (
      <PageContainer>
        <PageLoader
          isLoading={false}
          isError={true}
          errorMessage={t("common.unknown_error")}
        >
          <></>
        </PageLoader>
      </PageContainer>
    );
  }

  const headerActions = [
    !isEditing ? (
      <Space key="view-actions">
        {canUpdate && (
          <>
            <Button
              type="primary"
              icon={actionIcons.edit}
              onClick={handleStartEditing}
            >
              {t("common.edit")}
            </Button>
            <Tooltip
              title={
                !canPerformSelfDestructiveAction
                  ? t("users.form.messages.cannot_deactivate_self")
                  : undefined
              }
            >
              <Button
                icon={actionIcons.toggleUser}
                onClick={handleToggleStatus}
                disabled={
                  (!user.isActive && user.isFirstLogin) ||
                  !canPerformSelfDestructiveAction
                }
              >
                {user.isActive
                  ? t("users.form.actions.deactivate")
                  : t("users.form.actions.activate")}
              </Button>
            </Tooltip>
          </>
        )}
        {canDelete && (
          <Tooltip
            title={
              !canPerformSelfDestructiveAction
                ? t("users.form.messages.cannot_delete_self")
                : undefined
            }
          >
            <Popconfirm
              title={t("users.form.messages.delete_confirm")}
              description={t("users.form.messages.delete_ask")}
              onConfirm={handleDelete}
              okText={t("common.yes")}
              cancelText={t("common.no")}
              okButtonProps={{ danger: true }}
              disabled={!canPerformSelfDestructiveAction}
            >
              <Button
                danger
                icon={actionIcons.delete}
                disabled={!canPerformSelfDestructiveAction}
              >
                {t("common.delete")}
              </Button>
            </Popconfirm>
          </Tooltip>
        )}
      </Space>
    ) : null,
  ].filter(Boolean) as React.ReactNode[];

  return (
    <PageContainer>
      <PageHeader
        title={isEditing ? `${t("users.form.titles.edit")}` : user.username}
        subtitle={isEditing ? user.username : t("users.form.titles.view")}
        onBack={handleBack}
        backLabel={t("common.back")}
        titleTag={
          !isEditing && (
            <Tag
              color={user.isActive ? "green" : "red"}
              className={styles.statusTag}
            >
              {user.isActive
                ? t("users.table.status_active")
                : t("users.table.status_inactive")}
            </Tag>
          )
        }
        actions={headerActions}
      />

      <div className={styles.mainLayout}>
        <div className={styles.contentSection}>
          <SectionCard
            className={styles.contentCard}
            title={t("users.form.user_info")}
          >
            <UserForm
              form={form}
              initialValues={user}
              disabled={!isEditing}
              showButtons={isEditing}
              onSuccess={handleEditSuccess}
              onCancel={handleCancelEditing}
              submitText={t("users.form.buttons.submit_edit")}
            />
          </SectionCard>
        </div>

        <div className={styles.sidePanel}>
          <SectionCard className={styles.auditCard}>
            <Space size={16} align="center">
              <Tooltip
                title={
                  viewedUserConnected
                    ? t("users.table.connected")
                    : t("users.table.disconnected")
                }
              >
                <span
                  className={`${styles.profileAvatar} ${
                    viewedUserConnected
                      ? styles.profileAvatarOnline
                      : styles.profileAvatarOffline
                  }`}
                >
                  <UserAvatar
                    avatarDocumentId={user.avatarDocumentId}
                    size={80}
                  />
                  <span
                    role="img"
                    aria-label={
                      viewedUserConnected
                        ? t("users.table.connected")
                        : t("users.table.disconnected")
                    }
                    className={styles.profilePresenceDot}
                  />
                </span>
              </Tooltip>
              <div>
                <Text strong>
                  {[user.firstName, user.lastName].filter(Boolean).join(" ") ||
                    user.username}
                </Text>
                <br />
                <Text type="secondary">{user.email ?? "-"}</Text>
              </div>
            </Space>
          </SectionCard>

          {canViewIncidents && !isEditing && (
            <SectionCard className={styles.auditCard}>
              <div className={styles.auditHeader}>
                {detailIcons.solution} <span>{t("users.incidents.title")}</span>
              </div>
              <Space
                direction="vertical"
                size={8}
                style={{ width: "100%", marginTop: 8 }}
              >
                <Button
                  block
                  icon={actionIcons.view}
                  onClick={() =>
                    navigate(`${APP_ROUTES.INCIDENTS}?createdBy=${user.id}`, {
                      state: { incidentUserLabel },
                    })
                  }
                >
                  {t("users.incidents.created")}
                </Button>
                <Button
                  block
                  icon={actionIcons.view}
                  onClick={() =>
                    navigate(`${APP_ROUTES.INCIDENTS}?assignedTo=${user.id}`, {
                      state: { incidentUserLabel },
                    })
                  }
                >
                  {t("users.incidents.assigned")}
                </Button>
              </Space>
            </SectionCard>
          )}

          <SectionCard className={styles.auditCard}>
            <div className={styles.auditHeader}>
              {detailIcons.history}{" "}
              <span>{t("users.form.sections.traceability")}</span>
            </div>
            <div className={styles.auditItem}>
              <span className={styles.label}>
                {t("users.form.labels.created_at")}
              </span>
              <span className={styles.value}>
                <span className={styles.auditIcon}>{detailIcons.calendar}</span>
                {formatDateTime(user.createdAt)}
              </span>
            </div>
            <div className={styles.auditItem}>
              <span className={styles.label}>
                {t("users.form.labels.updated_at")}
              </span>
              <span className={styles.value}>
                <span className={styles.auditIcon}>{detailIcons.history}</span>
                {formatDateTime(user.updatedAt)}
              </span>
            </div>
            {user.modifiedBy && (
              <div className={styles.auditItem}>
                <span className={styles.label}>
                  {t("users.form.labels.modified_by")}
                </span>
                <span className={styles.value}>
                  <span className={styles.auditIcon}>{detailIcons.user}</span>
                  {user.modifiedBy}
                </span>
              </div>
            )}
            <Divider className={styles.auditDivider} />
            <div className={styles.auditItem}>
              <span className={styles.label}>
                {t("users.form.labels.last_login")}
              </span>
              <span className={styles.value}>
                <span className={styles.auditIcon}>{detailIcons.login}</span>
                {user.lastLogin ? formatDateTime(user.lastLogin) : "-"}
              </span>
            </div>
          </SectionCard>

          {canUpdate && !isEditing && (
            <SectionCard className={styles.passwordCard}>
              <PasswordManagement
                user={user}
                currentUser={currentUser || undefined}
              />
            </SectionCard>
          )}

          <Tooltip title={t("users.form.messages.edit_hint")}>
            <SectionCard className={styles.modeHintCard}>
              <Text type="secondary" className={styles.modeHintText}>
                <span className={styles.modeHintIcon}>
                  {detailIcons.solution}
                </span>
                {isEditing
                  ? t("users.form.messages.editing_mode")
                  : t("users.form.messages.viewing_mode")}
              </Text>
            </SectionCard>
          </Tooltip>
        </div>
      </div>
    </PageContainer>
  );
};

export default memo(ViewUser);
