// Page d'administration systeme regroupant tous les onglets de configuration.

import { memo, useMemo } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { Tabs } from "antd";
import { useTranslation } from "react-i18next";
import { useAuth } from "../../hooks/auth/useAuth";
import { APP_ROUTES } from "../../utils/constants";
import { PERMISSIONS } from "../../utils/permissions/permissions";
import { PageHeader } from "../../components/ui";
import { PageContainer } from "../../components/Layout";
import IncidentTypeTab from "./components/IncidentTypeTab/IncidentTypeTab";
import AgencyTab from "./components/AgencyTab/AgencyTab";
import ServiceTab from "./components/ServiceTab/ServiceTab";
import RolePermissionTab from "./components/RolePermissionTab/RolePermissionTab";
import ReportScheduleTab from "./components/ReportScheduleTab/ReportScheduleTab";
import styles from "./Settings.module.scss";

// Rend le composant Settings pour l'interface parametrage.
const Settings = memo(() => {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const { hash } = useLocation();
  const navigate = useNavigate();

  const activeTab = hash.replace("#", "") || "incidentTypes";

  // Traite le changement d'onglet.
  const handleTabChange = (key: string) => {
    navigate(`${APP_ROUTES.SETTINGS}#${key}`, { replace: true });
  };

  const canManageSettings = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);
  const canCreateRole = hasPermission(PERMISSIONS.ROLE.CREATE);
  const canUpdateRole = hasPermission(PERMISSIONS.ROLE.UPDATE);
  const canDeleteRole = hasPermission(PERMISSIONS.ROLE.DELETE);
  const canManageRoles = canCreateRole || canUpdateRole || canDeleteRole;

  const tabItems = useMemo(() => {
    const items = [];
    if (canManageSettings) {
      items.push(
        {
          key: "incidentTypes",
          label: t("settings.tabs.incidentTypes"),
          children: <IncidentTypeTab />,
        },
        {
          key: "agencies",
          label: t("settings.tabs.agencies"),
          children: <AgencyTab />,
        },
        {
          key: "services",
          label: t("settings.tabs.services"),
          children: <ServiceTab />,
        },
        {
          key: "reportSchedules",
          label: t("settings.tabs.reportSchedules"),
          children: <ReportScheduleTab />,
        },
      );
    }
    if (canManageRoles) {
      items.push({
        key: "rolesPermissions",
        label: t("settings.tabs.rolesPermissions"),
        children: <RolePermissionTab />,
      });
    }
    return items;
  }, [canManageSettings, canManageRoles, t]);

  if (!canManageSettings && !canManageRoles) {
    return <Navigate to={APP_ROUTES.DASHBOARD} replace />;
  }

  return (
    <PageContainer>
      <PageHeader
        title={t("settings.pageTitle")}
        subtitle={t("settings.pageSubtitle")}
      />
      <Tabs
        className={styles.settingsTabs}
        activeKey={activeTab}
        items={tabItems}
        onChange={handleTabChange}
        destroyOnHidden
      />
    </PageContainer>
  );
});

Settings.displayName = "Settings";
export default Settings;
