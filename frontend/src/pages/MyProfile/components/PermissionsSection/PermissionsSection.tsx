// Composant affichant les permissions detenues par un profil.

import React, { useMemo } from "react";
import { Collapse, Tag, Empty } from "antd";
import { useTranslation } from "react-i18next";
import type { Permission, Role } from "../../../../api/user/types";
import { getEffectivePermissions } from "../../../../utils/permissions/permissions";
import styles from "./PermissionsSection.module.scss";

// Definit les proprietes attendues par le composant PermissionsSection.
interface PermissionsSectionProps {
  roles: Role[];
  permissions: Permission[];
  revokedPermissions?: Permission[];
}

// Rend le composant PermissionsSection.
const PermissionsSection: React.FC<PermissionsSectionProps> = ({
  roles,
  permissions,
  revokedPermissions,
}) => {
  const { t } = useTranslation();

  // Permissions effectives : (directes ∪ role) - revoquees, via l'utilitaire partage.
  const effectivePermissions = useMemo(
    () =>
      getEffectivePermissions<Permission>({
        roles,
        permissions,
        revokedPermissions,
      }),
    [roles, permissions, revokedPermissions],
  );

  const grouped = useMemo(() => {
    const groups: Record<string, Permission[]> = {};
    for (const perm of effectivePermissions) {
      const prefix = perm.name.split("_")[0];
      if (!groups[prefix]) groups[prefix] = [];
      groups[prefix].push(perm);
    }
    return groups;
  }, [effectivePermissions]);

  const collapseItems = useMemo(
    () =>
      Object.entries(grouped).map(([prefix, perms]) => ({
        key: prefix,
        label: (
          <span className={styles.groupHeader}>
            <span className={styles.groupTitle}>
              {t(`users.permission_groups.${prefix}`, { defaultValue: prefix })}
            </span>
            <span className={styles.groupCount}>{perms.length}</span>
          </span>
        ),
        children: (
          <div className={styles.tagList} role="list" aria-label={prefix}>
            {perms.map((perm) => (
              <Tag
                key={perm.id}
                className={styles.permissionTag}
                role="listitem"
                title={t(`users.permission_descriptions.${perm.name}`, {
                  defaultValue: perm.description || perm.name,
                })}
              >
                {t(`users.permissions.${perm.name}`, {
                  defaultValue: perm.name,
                })}
              </Tag>
            ))}
          </div>
        ),
      })),
    [grouped, t],
  );

  if (effectivePermissions.length === 0) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description={t("myProfile.messages.no_permissions")}
      />
    );
  }

  return (
    <Collapse
      items={collapseItems}
      className={styles.collapse}
      ghost={false}
      size="small"
      defaultActiveKey={Object.keys(grouped)}
    />
  );
};

export default React.memo(PermissionsSection);
