// Barre laterale de navigation : affiche le menu principal filtre selon les permissions de l'utilisateur.
import { Layout, Menu, Tooltip, type MenuProps } from "antd";
import { useNavigate, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useMemo, memo, useCallback, useEffect } from "react";
import { useUIStore } from "../../store/uiStore/uiStore";
import { useAuth } from "../../hooks/auth/useAuth";
import { APP_ROUTES } from "../../utils/constants";
import {
  preloadRouteChunk,
  preloadRouteChunks,
} from "../../routes/routePreloading";
import {
  INCIDENT_VIEW_PERMISSIONS,
  USER_VIEW_PERMISSIONS,
  PERMISSIONS,
  hasAnyPermission,
} from "../../utils/permissions/permissions";
import { navigationIcons } from "../../utils/icons/appIcons";
import styles from "./Sidebar.module.scss";

const { Sider } = Layout;

type IdleScheduler = {
  requestIdleCallback?: (callback: () => void) => number;
  cancelIdleCallback?: (handle: number) => void;
};

// Icone de menu : ajoute une infobulle quand la barre laterale est repliee.
const CollapsibleIcon = ({
  icon,
  label,
  collapsed,
}: {
  icon: React.ReactNode;
  label: string;
  collapsed: boolean;
}) => {
  if (!collapsed) return <>{icon}</>;
  return (
    <Tooltip title={label} placement="right">
      {icon}
    </Tooltip>
  );
};

// Rend le composant Sidebar pour l'interface navigation.
const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const { sidebarCollapsed } = useUIStore();
  const { hasPermission } = useAuth();

  // Navigue vers la route correspondant a l'element de menu clique.
  const handleMenuClick = useCallback(
    ({ key }: { key: string }) => {
      void preloadRouteChunk(key);
      navigate(key);
    },
    [navigate],
  );

  // Construit les elements du menu et ne garde que ceux autorises par les permissions.
  const menuItems: MenuProps["items"] = useMemo(() => {
    const items = [
      {
        key: APP_ROUTES.SUPER_ADMIN,
        icon: (
          <CollapsibleIcon
            icon={navigationIcons.superAdmin}
            label={t("layout.sidebar.superAdmin")}
            collapsed={sidebarCollapsed}
          />
        ),
        label: t("layout.sidebar.superAdmin"),
        visible: hasPermission(PERMISSIONS.USER.CREATE_ADMIN),
      },
      {
        key: APP_ROUTES.DASHBOARD,
        icon: (
          <CollapsibleIcon
            icon={navigationIcons.dashboard}
            label={t("layout.sidebar.dashboard")}
            collapsed={sidebarCollapsed}
          />
        ),
        label: t("layout.sidebar.dashboard"),
        visible: hasPermission(PERMISSIONS.DASHBOARD.CONFIGURE),
      },
      {
        key: APP_ROUTES.INCIDENTS,
        icon: (
          <CollapsibleIcon
            icon={navigationIcons.incidents}
            label={t("layout.sidebar.incidents")}
            collapsed={sidebarCollapsed}
          />
        ),
        label: t("layout.sidebar.incidents"),
        visible: hasAnyPermission(hasPermission, INCIDENT_VIEW_PERMISSIONS),
      },
      {
        key: APP_ROUTES.USERS,
        icon: (
          <CollapsibleIcon
            icon={navigationIcons.users}
            label={t("layout.sidebar.users")}
            collapsed={sidebarCollapsed}
          />
        ),
        label: t("layout.sidebar.users"),
        visible: hasAnyPermission(hasPermission, USER_VIEW_PERMISSIONS),
      },
      {
        key: APP_ROUTES.REPORTS,
        icon: (
          <CollapsibleIcon
            icon={navigationIcons.reports}
            label={t("layout.sidebar.reports")}
            collapsed={sidebarCollapsed}
          />
        ),
        label: t("layout.sidebar.reports"),
        visible: hasAnyPermission(hasPermission, [
          PERMISSIONS.REPORT.VIEW_OWN,
          PERMISSIONS.REPORT.VIEW_AGENCY,
          PERMISSIONS.REPORT.VIEW_SERVICE,
          PERMISSIONS.REPORT.VIEW_ALL,
        ]),
      },
      {
        key: APP_ROUTES.AUDIT,
        icon: (
          <CollapsibleIcon
            icon={navigationIcons.audit}
            label={t("layout.sidebar.audit")}
            collapsed={sidebarCollapsed}
          />
        ),
        label: t("layout.sidebar.audit"),
        visible: hasPermission(PERMISSIONS.AUDIT.VIEW),
      },
      {
        key: APP_ROUTES.SETTINGS,
        icon: (
          <CollapsibleIcon
            icon={navigationIcons.settings}
            label={t("layout.sidebar.settings")}
            collapsed={sidebarCollapsed}
          />
        ),
        label: t("layout.sidebar.settings"),
        visible: hasAnyPermission(hasPermission, [
          PERMISSIONS.SETTINGS.INCIDENT_TYPES,
          PERMISSIONS.SETTINGS.SYSTEM,
        ]),
      },
    ];

    return items
      .filter((item) => item.visible)
      .map(({ visible: _visible, ...rest }) => rest);
  }, [t, hasPermission, sidebarCollapsed]);

  const visibleRouteKeys = useMemo(
    () =>
      (menuItems ?? [])
        .map((item) => String((item as { key?: string }).key ?? ""))
        .filter(Boolean),
    [menuItems],
  );

  // Precharge les pages visibles quand le navigateur est disponible pour accelerer les transitions.
  useEffect(() => {
    if (visibleRouteKeys.length === 0) return;
    const idleScheduler = window as unknown as IdleScheduler;
    const preload = () => preloadRouteChunks(visibleRouteKeys);

    if (idleScheduler.requestIdleCallback) {
      const handle = idleScheduler.requestIdleCallback(preload);
      return () => idleScheduler.cancelIdleCallback?.(handle);
    }

    const handle = window.setTimeout(preload, 250);

    return () => {
      window.clearTimeout(handle);
    };
  }, [visibleRouteKeys]);

  const handleNavigationHover = useCallback(() => {
    preloadRouteChunks(visibleRouteKeys);
  }, [visibleRouteKeys]);

  const activeKey = useMemo(() => {
    if (visibleRouteKeys.includes(location.pathname)) {
      return location.pathname;
    }
    const matchingKey = visibleRouteKeys
      .filter((key) => location.pathname.startsWith(key))
      .sort((a, b) => b.length - a.length)[0];
    return matchingKey || location.pathname;
  }, [location.pathname, visibleRouteKeys]);

  return (
    <Sider
      trigger={null}
      collapsible
      collapsed={sidebarCollapsed}
      className={styles.sidebar}
      width={250}
      onMouseEnter={handleNavigationHover}
      onFocus={handleNavigationHover}
    >
      <div className={styles.logoContainer}>
        <img
          src={
            sidebarCollapsed ? "/Img/logo-mark-dark.svg" : "/Img/logo-dark.svg"
          }
          alt={t("common.app_name")}
          className={styles.logo}
        />
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[activeKey]}
        items={menuItems}
        onClick={handleMenuClick}
        className={styles.menu}
      />
    </Sider>
  );
};

export default memo(Sidebar);
