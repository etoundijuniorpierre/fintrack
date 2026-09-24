// Page presentant les caracteristiques completes d'un role.

import { memo, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Button,
  Space,
  Tag,
  Typography,
  Row,
  Col,
  Tooltip,
  Divider,
  Badge,
} from "antd";
import { useTranslation } from "react-i18next";
import { useRole, useDeleteRole } from "../../../../../hooks/settings";
import { useUsers } from "../../../../../hooks/user";
import { useAuth } from "../../../../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../../../../utils/permissions/permissions";
import { PageHeader, Card, ConfirmDialog } from "../../../../../components/ui";
import { PageContainer } from "../../../../../components/Layout";
import { APP_ROUTES } from "../../../../../utils/constants";
import {
  getRoleTranslationKey,
  formatRoleName,
} from "../../../../../utils/roles/roles";
import type { PermissionResponse } from "../../../../../api/settings/types";
import {
  actionIcons,
  authIcons,
  detailIcons,
} from "../../../../../utils/icons/appIcons";
import styles from "./RoleDetailsPage.module.scss";

const { Title, Text, Paragraph } = Typography;

// Regroupe les donnees de role details page par critere metier.
function groupPermissionsByPrefix(
  permissions: PermissionResponse[] = [],
): Record<string, PermissionResponse[]> {
  return permissions.reduce<Record<string, PermissionResponse[]>>(
    (acc, perm) => {
      const underscoreIndex = perm.name.indexOf("_");
      const prefix =
        underscoreIndex !== -1 ? perm.name.slice(0, underscoreIndex) : "OTHER";
      if (!acc[prefix]) {
        acc[prefix] = [];
      }
      acc[prefix].push(perm);
      return acc;
    },
    {},
  );
}

const PREFIX_COLORS: Record<string, string> = {
  INCIDENT: "red",
  USER: "blue",
  ROLE: "purple",
  AUDIT: "orange",
  SETTINGS: "cyan",
  DASHBOARD: "green",
  REPORT: "geekblue",
  NOTIFICATION: "volcano",
};

// Rend le composant RoleDetailsPage pour l'interface role details page.
const RoleDetailsPage = memo(() => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();

  const { data: role, isLoading } = useRole(id || "");
  const { data: users = [] } = useUsers();
  const { mutate: deleteRole } = useDeleteRole();

  // Compte les utilisateurs associes au role courant.
  const usersWithRoleCount = useMemo(() => {
    if (!role) return 0;
    return users.filter((user) =>
      user.roles?.some((userRole) => userRole.id === role.id),
    ).length;
  }, [role, users]);

  const canUpdate = hasPermission(PERMISSIONS.ROLE.UPDATE);
  const canDelete = hasPermission(PERMISSIONS.ROLE.DELETE);

  const groupedPermissions = useMemo(
    () => (role ? groupPermissionsByPrefix(role.permissions) : {}),
    [role],
  );

  // Traite le retour a l'ecran precedent.
  const handleBack = () => navigate(-1);

  // Traite le passage en edition.
  const handleEdit = () => {
    if (id) navigate(APP_ROUTES.SETTINGS_ROLES_EDIT(id));
  };

  // Gere la suppression d'un element.
  const handleDelete = () => {
    if (id) {
      deleteRole(id, {
        onSuccess: () => navigate(APP_ROUTES.SETTINGS + "#rolesPermissions"),
      });
    }
  };

  if (isLoading)
    return (
      <PageContainer>
        <div />
      </PageContainer>
    );
  if (!role)
    return (
      <PageContainer>{t("settings.roles.messages.not_found")}</PageContainer>
    );

  const roleTranslationKey = getRoleTranslationKey(role.name);
  const roleDisplayName = formatRoleName(role, t);
  const roleDescription = t(`users.role_descriptions.${roleTranslationKey}`, {
    defaultValue: role.description || t("common.noDescription"),
  });

  return (
    <PageContainer className="animate-entry">
      <PageHeader
        title={roleDisplayName}
        onBack={handleBack}
        subtitle={
          role.isSystem
            ? t("settings.roles.system.role_label")
            : t("settings.roles.custom_role_label")
        }
        actions={
          <Space size="middle">
            {canUpdate && (
              <Button
                type="primary"
                icon={actionIcons.edit}
                onClick={handleEdit}
                className="btn-primary"
              >
                {t("settings.buttons.edit")}
              </Button>
            )}
            {canDelete && !role.isSystem && (
              <ConfirmDialog
                title={t("settings.roles.messages.delete_confirm")}
                description={t("settings.roles.messages.delete_ask")}
                onConfirm={handleDelete}
                danger
              >
                <Button
                  danger
                  type="primary"
                  icon={actionIcons.delete}
                  className="btn-danger"
                >
                  {t("settings.buttons.delete")}
                </Button>
              </ConfirmDialog>
            )}
          </Space>
        }
      />

      <div className={styles.content}>
        <Row gutter={[32, 32]}>
          <Col xs={24} lg={16}>
            <Card className={styles.mainCard}>
              <div className={styles.roleHeader}>
                <div className={styles.roleIcon}>{authIcons.certificate}</div>
                <div className={styles.roleTitleInfo}>
                  <div className={styles.titleRow}>
                    <Title level={2} className={styles.roleTitle}>
                      {roleDisplayName}
                    </Title>
                    {role.isSystem && (
                      <Tag color="gold" className={styles.statusTag}>
                        {t("settings.roles.system.yes")}
                      </Tag>
                    )}
                  </div>
                  <Paragraph className={styles.roleDescription}>
                    {roleDescription}
                  </Paragraph>
                </div>
              </div>
            </Card>
          </Col>
          <Col xs={24} lg={8}>
            <Card className={styles.sideCard}>
              <Title level={4} className={styles.sideTitle}>
                {t("common.overview")}
              </Title>
              <div className={styles.infoGrid}>
                <div className={styles.infoItem}>
                  <div className={styles.infoIcon}>{detailIcons.app}</div>
                  <div className={styles.infoContent}>
                    <Text className={styles.infoLabel}>
                      {t("settings.roles.table.permissions")}
                    </Text>
                    <Text className={styles.infoValue}>
                      {role.permissions?.length || 0}
                    </Text>
                  </div>
                </div>

                <Divider />

                <div className={styles.infoItem}>
                  <div className={styles.infoIcon}>{detailIcons.user}</div>
                  <div className={styles.infoContent}>
                    <Text className={styles.infoLabel}>
                      {t("settings.roles.assigned_users")}
                    </Text>
                    <Text className={styles.infoValue}>
                      {usersWithRoleCount}
                    </Text>
                  </div>
                </div>
              </div>
            </Card>
          </Col>
        </Row>

        <div className={styles.permissionsSection}>
          <div className={styles.sectionHeader}>
            <Title level={3} className={styles.sectionTitle}>
              {t("settings.roles.form.labels.permissions")}
            </Title>
            <Text className={styles.sectionSubtitle}>
              {t("settings.roles.permissions_info")}
            </Text>
          </div>

          <Row gutter={[24, 24]} className={styles.permissionsGrid}>
            {Object.entries(groupedPermissions).map(([prefix, perms]) => (
              <Col xs={24} md={12} xl={8} key={prefix}>
                <Card
                  className={styles.permissionCategoryCard}
                  title={
                    <Space>
                      <Badge
                        status="processing"
                        color={PREFIX_COLORS[prefix] || "default"}
                      />
                      <Text strong>
                        {t(`users.permission_groups.${prefix}`, prefix)}
                      </Text>
                      <Tag className={styles.badgeTag}>{perms.length}</Tag>
                    </Space>
                  }
                >
                  <div className={styles.tagsContainer}>
                    {perms.map((perm) => (
                      <Tooltip
                        key={perm.id}
                        title={t(
                          `users.permission_descriptions.${perm.name}`,
                          perm.description ?? perm.name,
                        )}
                      >
                        <Tag className={styles.permissionTag}>
                          {t(
                            `users.permissions.${perm.name}`,
                            perm.name.split("_").slice(1).join(" "),
                          )}
                        </Tag>
                      </Tooltip>
                    ))}
                  </div>
                </Card>
              </Col>
            ))}
          </Row>
        </div>
      </div>
    </PageContainer>
  );
});

RoleDetailsPage.displayName = "RoleDetailsPage";

export default RoleDetailsPage;
