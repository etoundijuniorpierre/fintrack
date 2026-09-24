// Indicateur d'en-tete : nombre d'utilisateurs connectes et liste de presence.

import { memo, useMemo, useState } from "react";
import { Badge, Button, Empty, Popover, Tabs, Typography } from "antd";
import { TeamOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { useOnlinePresence } from "../../../hooks/user/useOnlinePresence/useOnlinePresence";
import { useUsers } from "../../../hooks/user/useUsers/useUsers";
import { formatUserName, formatPresenceStatus } from "../../../utils/formatters/formatters";
import type { PresenceState } from "../../../api/notification/types";
import type { User } from "../../../api/user/types";
import headerStyles from "../AppHeader.module.scss";
import styles from "./PresenceIndicator.module.scss";

const { Text } = Typography;

// Ligne de presence : pastille d'etat, nom complet et agence de rattachement.
const PresenceRow = memo(
  ({
    user,
    online,
    presence,
  }: {
    user: User;
    online: boolean;
    presence?: PresenceState;
  }) => {
    const { t } = useTranslation();
    const changedAt = online
      ? presence?.connectedAt
      : presence?.disconnectedAt;
    return (
      <div className={styles.row}>
        <span
          className={`${styles.dot} ${online ? styles.dotOnline : styles.dotOffline}`}
        />
        <div className={styles.identity}>
          <Text strong>{formatUserName(user)}</Text>
          <Text type="secondary" className={styles.username}>
            {user.agency?.name ?? t("layout.header.presence.no_agency")} -{" "}
            {formatPresenceStatus(changedAt, online, t)}
          </Text>
        </div>
      </div>
    );
  },
);
PresenceRow.displayName = "PresenceRow";

// Contenu du popover : liste du perimetre de l'appelant, separee par etat.
const PresenceLists = ({
  users,
  isLoading,
  online,
  states,
}: {
  users: User[];
  isLoading: boolean;
  online: string[];
  states: Record<string, PresenceState>;
}) => {
  const { t } = useTranslation();

  const [connected, disconnected] = useMemo(() => {
    const onlineSet = new Set(online);
    const isConnected = (user: User) => onlineSet.has(user.username);
    return [
      users.filter(isConnected),
      users.filter((user) => !isConnected(user) && user.isActive),
    ];
  }, [users, online]);

  const renderList = (list: User[], onlineState: boolean) =>
    list.length === 0 ? (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description={t("layout.header.presence.empty")}
      />
    ) : (
      <div className={styles.list}>
        {list.map((user) => (
          <PresenceRow
            key={user.id}
            user={user}
            online={onlineState}
            presence={states[user.username]}
          />
        ))}
      </div>
    );

  return (
    <Tabs
      size="small"
      items={[
        {
          key: "connected",
          label: `${t("layout.header.presence.connected")} (${connected.length})`,
          children: renderList(connected, true),
        },
        {
          key: "disconnected",
          label: `${t("layout.header.presence.disconnected")} (${disconnected.length})`,
          children: isLoading ? null : renderList(disconnected, false),
        },
      ]}
    />
  );
};

// Rend le composant PresenceIndicator pour la barre superieure.
const PresenceIndicator = memo(() => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const { online, states, canViewPresence } = useOnlinePresence();
  // La liste (scopee cote serveur) borne compteur et popover au perimetre
  // de l'appelant et la presence provient uniquement du registre WebSocket.
  const { data: users, isLoading } = useUsers({ enabled: canViewPresence });

  const totalOnline = useMemo(() => {
    const onlineSet = new Set(online);
    return (users ?? []).filter((item) =>
      onlineSet.has(item.username),
    ).length;
  }, [users, online]);

  if (!canViewPresence) return null;

  const hasOnline = totalOnline > 0;

  return (
    <Popover
      open={open}
      onOpenChange={setOpen}
      trigger="click"
      placement="bottomRight"
      title={t("layout.header.presence.title")}
      content={
        open ? (
          <PresenceLists
            users={users ?? []}
            isLoading={isLoading}
            online={online}
            states={states}
          />
        ) : null
      }
    >
      <Badge
        count={totalOnline}
        size="small"
        offset={[-2, 2]}
        className={hasOnline ? styles.badgePulse : undefined}
      >
        <Button
          type="text"
          shape="circle"
          className={`${headerStyles.iconBtn} ${hasOnline ? styles.iconAlive : ""}`}
          icon={<TeamOutlined aria-hidden="true" />}
          title={t("layout.header.presence.title")}
          aria-label={t("layout.header.presence.title")}
        />
      </Badge>
    </Popover>
  );
});
PresenceIndicator.displayName = "PresenceIndicator";

export default PresenceIndicator;
