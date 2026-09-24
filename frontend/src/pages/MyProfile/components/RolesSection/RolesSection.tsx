// Composant listant et detaillant les roles affectes.

import React, { memo } from "react";
import { List, Empty, Typography } from "antd";
import { useTranslation } from "react-i18next";
import type { Role } from "../../../../api/user/types";
import { formatRoleName } from "../../../../utils/roles/roles";
import styles from "./RolesSection.module.scss";

const { Text } = Typography;

// Definit les proprietes attendues par le composant RolesSection.
interface RolesSectionProps {
  roles: Role[];
}

// Rend le composant RolesSection.
const RolesSection: React.FC<RolesSectionProps> = memo(({ roles }) => {
  const { t } = useTranslation();

  if (!roles || roles.length === 0) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description={t("myProfile.messages.no_roles")}
      />
    );
  }

  return (
    <List
      className={styles.rolesList}
      dataSource={roles}
      rowKey={(role) => role.id}
      renderItem={(role) => (
        <List.Item>
          <List.Item.Meta
            title={formatRoleName(role, t)}
            description={
              <Text type="secondary">
                {t(`users.role_descriptions.${role.name}`, {
                  defaultValue: role.description || "",
                })}
              </Text>
            }
          />
        </List.Item>
      )}
    />
  );
});

RolesSection.displayName = "RolesSection";

export default RolesSection;
