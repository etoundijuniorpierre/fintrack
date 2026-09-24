// En-tete de l'application : bouton de menu, notifications, aide, langue et menu utilisateur.
import {
  Layout,
  Button,
  Dropdown,
  Space,
  Typography,
  type MenuProps,
} from "antd";
import { useTranslation } from "react-i18next";
import { useMemo, memo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth, useLogout } from "../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../utils/permissions/permissions";
import { useMyProfile } from "../../hooks/user/useMyProfile";
import { useUIStore } from "../../store/uiStore/uiStore";
import { useThemeStore } from "../../store/themeStore";
import { APP_ROUTES } from "../../utils/constants";
import { headerIcons, navigationIcons } from "../../utils/icons/appIcons";
import NotificationBell from "./notification/NotificationBell";
import PresenceIndicator from "./presence/PresenceIndicator";
import UserAvatar from "../UserAvatar/UserAvatar";
import styles from "./AppHeader.module.scss";
import { useOnlinePresence } from "../../hooks/user/useOnlinePresence/useOnlinePresence";

const { Header } = Layout;
const { Text } = Typography;

// Centralise la logique d'interface liee a app header props.
export interface AppHeaderProps {
  onMobileMenuClick?: () => void;
}

// Rend le composant AppHeader pour l'interface app en-tete.
const AppHeader = ({ onMobileMenuClick }: AppHeaderProps) => {
  const { user, hasPermission, user: currentUser } = useAuth();
  const { isOnline } = useOnlinePresence();
  const { t, i18n } = useTranslation();
  const { mutate: logout } = useLogout();
  const { sidebarCollapsed, toggleSidebar } = useUIStore();
  const { mode, toggleTheme } = useThemeStore();
  const navigate = useNavigate();
  const { data: profile } = useMyProfile(user?.id ?? "");

  // Ouvre le tiroir mobile ou replie/deplie la barre laterale selon le contexte.
  const handleToggle = useCallback(() => {
    if (onMobileMenuClick) {
      onMobileMenuClick();
    } else {
      toggleSidebar();
    }
  }, [onMobileMenuClick, toggleSidebar]);

  // Traite l'ouverture de l'aide.
  const handleHelpClick = useCallback(() => {
    navigate(APP_ROUTES.HELP);
  }, [navigate]);

  const languageMenuItems: MenuProps = useMemo(
    () => ({
      items: [
        {
          key: "fr",
          label: "Français",
          onClick: () => i18n.changeLanguage("fr"),
          disabled: i18n.language === "fr",
        },
        {
          key: "en",
          label: "English",
          onClick: () => i18n.changeLanguage("en"),
          disabled: i18n.language === "en",
        },
      ],
    }),
    [i18n],
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

  // Menu utilisateur : profil (si autorise) et deconnexion.
  const userMenuItems: MenuProps = useMemo(
    () => ({
      items: [
        ...(hasPermission(PERMISSIONS.USER.MANAGE_PROFILE)
          ? [
            {
              key: "profile",
              label: t("layout.header.profile"),
              icon: headerIcons.user,
              onClick: () => navigate(APP_ROUTES.PROFILE),
            },
          ]
          : []),
        {
          type: "divider" as const,
        },
        {
          key: "logout",
          label: t("layout.header.logout"),
          icon: headerIcons.logout,
          danger: true,
          onClick: () => logout(),
        },
      ],
    }),
    [t, logout, navigate, hasPermission],
  );

  return (
    <Header className={styles.header}>
      <div className={styles.leftSection}>
        <Button
          type="text"
          icon={
            sidebarCollapsed
              ? navigationIcons.menuUnfold
              : navigationIcons.menuFold
          }
          onClick={handleToggle}
          className={styles.toggleBtn}
          aria-label={
            sidebarCollapsed
              ? t("layout.sidebar.expand")
              : t("layout.sidebar.collapse")
          }
        />
      </div>

      <div className={styles.rightSection}>
        <Space size={20}>
          <PresenceIndicator />
          <NotificationBell />

          <Button
            type="text"
            icon={headerIcons.help}
            className={styles.iconBtn}
            onClick={handleHelpClick}
            title={t("layout.header.help")}
            aria-label={t("layout.header.help")}
            data-testid="help-button"
          />

          <Button
            type="text"
            icon={mode === "dark" ? headerIcons.sun : headerIcons.moon}
            className={styles.iconBtn}
            onClick={toggleTheme}
            title={mode === "dark" ? "Mode Clair" : "Mode Sombre"}
            aria-label="Toggle Theme"
          />

          <Dropdown
            menu={languageMenuItems}
            placement="bottomRight"
            trigger={["click"]}
          >
            <Button
              type="text"
              icon={headerIcons.global}
              className={styles.iconBtn}
              title={t("layout.header.change_language")}
              aria-label={t("layout.header.change_language")}
            />
          </Dropdown>

          <Dropdown
            menu={userMenuItems}
            placement="bottomRight"
            trigger={["click"]}
          >
            <div
              className={`${styles.userProfile} ${viewedUserConnected
                  ? styles.profileAvatarOnline
                  : styles.profileAvatarOffline
                }`}
              role="button"
              aria-haspopup="true"
              aria-label={t("layout.header.user_menu")}
            >
              <Space>
                <div className={styles.userInfo}>
                  <Text className={styles.userName}>{user?.username}</Text>
                  <Text className={styles.userRole}>
                    {user?.roles?.[0]?.toUpperCase() ||
                      t("layout.header.default_role")}
                  </Text>
                </div>
                <UserAvatar
                  avatarDocumentId={profile?.avatarDocumentId}
                  className={styles.avatar}
                />
              </Space>
            </div>
          </Dropdown>
        </Space>
      </div>
    </Header>
  );
};

export default memo(AppHeader);
