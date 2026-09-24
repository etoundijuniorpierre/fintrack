// Carte KPI : affiche un indicateur (titre, valeur, icone) avec un ton de couleur et tendance optionnelle.
import { Card, Statistic, Tooltip, Typography } from "antd";
import { QuestionCircleOutlined } from "@ant-design/icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import styles from "./KpiCard.module.scss";

const { Text } = Typography;

// Centralise la logique d'interface liee a kpi card props.
export interface KpiCardProps {
  title: string;
  value: number | string;
  icon: React.ReactNode;
  tone?: "danger" | "warning" | "success" | "info" | "neutral";
  subtitle?: string;
  details?: string[];
  /** Texte d'aide affiche via une infobulle (ⓘ) pour expliquer la metrique a un non-specialiste. */
  help?: string;
  trend?: {
    value: number;
    isUp: boolean;
  };
}

// Rend le composant KpiCard pour l'interface kpi carte.
const KpiCard = memo(
  ({
    title,
    value,
    icon,
    tone = "info",
    subtitle,
    details,
    help,
    trend,
  }: KpiCardProps) => {
    const { t } = useTranslation();

    return (
      <Card
        className={`${styles.card} ${styles[tone]}`}
        aria-label={`${title}: ${value}`}
      >
        <div className={styles.content}>
          <div className={styles.iconWrapper}>
            <div className={styles.icon} aria-hidden="true">
              {icon}
            </div>
          </div>
          <div className={styles.details}>
            <Text className={styles.title}>
              {title}
              {help && (
                <Tooltip title={help}>
                  <QuestionCircleOutlined
                    className={styles.helpIcon}
                    aria-label={help}
                  />
                </Tooltip>
              )}
            </Text>
            <Statistic value={value} className={styles.valueText} />
            {subtitle && (
              <Text className={styles.subtitle} type="secondary">
                {subtitle}
              </Text>
            )}
            {details?.map((detail) => (
              <Text key={detail} className={styles.subtitle} type="secondary">
                {detail}
              </Text>
            ))}
            {trend && (
              <div className={styles.trend}>
                <Text className={styles.trendValue}>
                  {trend.isUp ? "+" : "-"} {trend.value}%
                </Text>
                <Text className={styles.trendLabel}>
                  {t("dashboard.kpi.comparison")}
                </Text>
              </div>
            )}
          </div>
        </div>
      </Card>
    );
  },
);

KpiCard.displayName = "KpiCard";

export default KpiCard;
