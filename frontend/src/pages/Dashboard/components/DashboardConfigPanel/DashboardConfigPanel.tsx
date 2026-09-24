// Panneau lateral de configuration du dashboard : permet de choisir les widgets visibles.
import { memo, useCallback, useMemo, useState } from "react";
import {
  Drawer,
  Checkbox,
  Button,
  Space,
  Typography,
  Divider,
  Tooltip,
} from "antd";
import { QuestionCircleOutlined } from "@ant-design/icons";
import { useTranslation } from "react-i18next";
import { type DashboardConfig, WidgetType } from "../../../../types/dashboard";
import {
  getVisibleWidgets,
  isWidgetAllowedInView,
} from "../../../../utils/dashboard/widgetPermissions/widgetPermissions";
import type { ScopeView } from "../../../../utils/permissions/permissions";
import {
  saveDashboardConfig,
  resetDashboardConfig,
} from "../../../../utils/dashboard/config/dashboardConfig";
import styles from "./DashboardConfigPanel.module.scss";

const { Title, Text } = Typography;

const GRAPH_WIDGETS_SET = new Set<string>([
  WidgetType.INCIDENT_TYPE_DISTRIBUTION,
  WidgetType.INCIDENT_CRITICALITY_DISTRIBUTION,
  WidgetType.INCIDENT_STATUS_DISTRIBUTION,
  WidgetType.INCIDENT_MONTHLY_CLOSURES,
  WidgetType.INCIDENT_MONTHLY_AVG_CLOSURE,
  WidgetType.INCIDENT_CLOSURE_BY_TYPE,
  WidgetType.INCIDENT_CLOSURE_BY_CRITICALITY,
  WidgetType.INCIDENT_AGING,
  WidgetType.INCIDENT_COHORT,
  WidgetType.INCIDENT_WORKLOAD,
  WidgetType.INCIDENT_TOP_SERVICES,
  WidgetType.INCIDENT_TOP_AGENCIES,
  WidgetType.INCIDENT_TOP_RESOLVERS,
]);

// Definit les proprietes attendues par le composant DashboardConfigPanel.
interface DashboardConfigPanelProps {
  userId: string;
  userPermissions: string[];
  currentConfig: DashboardConfig | null;
  // Portee courante : ne propose que les widgets affichables dans cette vue.
  view: ScopeView;
  onClose: () => void;
  onSave: (config: DashboardConfig) => void;
}

// Determine les widgets initialement coches selon la config existante et les permissions.
const getInitialWidgets = (
  currentConfig: DashboardConfig | null,
  permittedWidgets: WidgetType[],
): WidgetType[] => {
  if (currentConfig === null) {
    return permittedWidgets;
  }
  return currentConfig.visibleWidgets.filter((w) =>
    permittedWidgets.includes(w),
  );
};

// Rend le composant DashboardConfigPanel pour l'interface tableau de bord configuration panel.
const DashboardConfigPanel = memo(
  ({
    userId,
    userPermissions,
    currentConfig,
    view,
    onClose,
    onSave,
  }: DashboardConfigPanelProps) => {
    const { t } = useTranslation();

    // Permissions portee courante.
    const permittedWidgets = useMemo(
      () =>
        getVisibleWidgets(userPermissions).filter((w) =>
          isWidgetAllowedInView(w, view),
        ),
      [userPermissions, view],
    );

    const { graphWidgets, otherWidgets } = useMemo(() => {
      const graphs: WidgetType[] = [];
      const others: WidgetType[] = [];
      permittedWidgets.forEach((w) => {
        if (GRAPH_WIDGETS_SET.has(w)) {
          graphs.push(w);
        } else {
          others.push(w);
        }
      });
      return { graphWidgets: graphs, otherWidgets: others };
    }, [permittedWidgets]);

    const [selectedWidgets, setSelectedWidgets] = useState<WidgetType[]>(() =>
      getInitialWidgets(currentConfig, permittedWidgets),
    );

    // Ajoute ou retire un widget de la selection.
    const handleToggle = useCallback((widget: WidgetType, checked: boolean) => {
      setSelectedWidgets((prev) =>
        checked ? [...prev, widget] : prev.filter((w) => w !== widget),
      );
    }, []);

    // Sauvegarde la configuration choisie puis ferme le panneau.
    const handleApply = useCallback(() => {
      const config: DashboardConfig = {
        userId,
        visibleWidgets: selectedWidgets,
        lastUpdated: new Date().toISOString(),
      };
      saveDashboardConfig(config);
      onSave(config);
      onClose();
    }, [userId, selectedWidgets, onSave, onClose]);

    // Reinitialise la configuration aux widgets par defaut autorises.
    const handleReset = useCallback(() => {
      resetDashboardConfig(userId);
      const resetWidgets = permittedWidgets;
      setSelectedWidgets(resetWidgets);
      const resetConfig: DashboardConfig = {
        userId,
        visibleWidgets: resetWidgets,
        lastUpdated: new Date().toISOString(),
      };
      onSave(resetConfig);
      onClose();
    }, [userId, permittedWidgets, onSave, onClose]);

    // Traite l'annulation.
    const handleCancel = useCallback(() => {
      onClose();
    }, [onClose]);

    const footer = (
      <Space className={styles.footer}>
        <Button onClick={handleCancel}>{t("dashboard.widgets.cancel")}</Button>
        <Button type="default" danger onClick={handleReset}>
          {t("dashboard.widgets.reset")}
        </Button>
        <Button type="primary" onClick={handleApply}>
          {t("dashboard.widgets.apply")}
        </Button>
      </Space>
    );

    return (
      <Drawer
        open
        title={
          <Title level={5} className={styles.title}>
            {t("dashboard.widgets.customize")}
          </Title>
        }
        onClose={handleCancel}
        footer={footer}
        size={360}
        destroyOnHidden
      >
        <div className={styles.content}>
          <Text type="secondary" className={styles.description}>
            {t("dashboard.widgets.description")}
          </Text>

          <Divider />

          <div
            className={styles.widgetList}
            role="group"
            aria-label={t("dashboard.widgets.customize")}
          >
            {otherWidgets.length > 0 && (
              <div className={styles.widgetSection}>
                <div className={styles.sectionHeader}>
                  {t("dashboard.widgets.group_others")}
                </div>
                {otherWidgets.map((widget) => (
                  <div key={widget} className={styles.widgetItem}>
                    <Checkbox
                      checked={selectedWidgets.includes(widget)}
                      onChange={(e) => handleToggle(widget, e.target.checked)}
                    >
                      {t(`dashboard.widgets.${widget}`)}
                    </Checkbox>
                    <Tooltip title={t(`dashboard.kpi.help.${widget}`)}>
                      <QuestionCircleOutlined
                        className={styles.helpIcon}
                        aria-label={t(`dashboard.kpi.help.${widget}`)}
                      />
                    </Tooltip>
                  </div>
                ))}
              </div>
            )}

            {otherWidgets.length > 0 && graphWidgets.length > 0 && (
              <Divider style={{ margin: "8px 0" }} />
            )}

            {graphWidgets.length > 0 && (
              <div className={styles.widgetSection}>
                <div className={styles.sectionHeader}>
                  {t("dashboard.widgets.group_graphs")}
                </div>
                {graphWidgets.map((widget) => (
                  <div key={widget} className={styles.widgetItem}>
                    <Checkbox
                      checked={selectedWidgets.includes(widget)}
                      onChange={(e) => handleToggle(widget, e.target.checked)}
                    >
                      {t(`dashboard.widgets.${widget}`)}
                    </Checkbox>
                    <Tooltip title={t(`dashboard.kpi.help.${widget}`)}>
                      <QuestionCircleOutlined
                        className={styles.helpIcon}
                        aria-label={t(`dashboard.kpi.help.${widget}`)}
                      />
                    </Tooltip>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </Drawer>
    );
  },
);

DashboardConfigPanel.displayName = "DashboardConfigPanel";

export default DashboardConfigPanel;
