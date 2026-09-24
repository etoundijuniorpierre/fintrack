// Liste des incidents recents du dashboard : gere les etats chargement, erreur et vide.
import { memo, useCallback, useMemo } from "react";
import { Tag, Skeleton, Empty, Typography, Alert, Button } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import type { IncidentHistoryResponse } from "../../../../api/incident/types";
import { ACTION_TYPE_COLORS } from "../../../../api/incident/types";
import { incidentNavigation } from "../../../../utils/navigation/incidents/incidents";
import {
  formatDate,
  formatUserName,
} from "../../../../utils/formatters/formatters";
import styles from "./RecentIncidentsList.module.scss";

const { Text } = Typography;

// Le widget d'activite recente est volontairement borne aux 10 incidents les
// plus recents : au-dela, l'utilisateur se reporte a la page Incidents via
// "Voir tout". Cette limite est appliquee cote frontend pour eviter d'afficher
// une liste qui deborde si l'API renvoie plus que ce que prevu.
const RECENT_ACTIVITY_LIMIT = 10;

// Definit les proprietes attendues par le composant RecentIncidentsList.
interface RecentIncidentsListProps {
  incidents: IncidentHistoryResponse[];
  isLoading: boolean;
  isError: boolean;
  onRetry?: () => void;
}

// Expose la constante SKELETON_COUNT utilisee par recent inincidents liste.
const SKELETON_COUNT = 5;

// Rend le composant SkeletonList pour l'interface recent incidents liste.
const SkeletonList = memo(() => (
  <div className={styles.skeletonList} aria-busy="true">
    {Array.from({ length: SKELETON_COUNT }).map((_, index) => (
      <div key={index} className={styles.skeletonItem}>
        <Skeleton active paragraph={{ rows: 1 }} title={{ width: "60%" }} />
      </div>
    ))}
  </div>
));

SkeletonList.displayName = "SkeletonList";

// Rend le composant RecentIncidentsList pour l'interface recent incidents liste.
const RecentIncidentsList = memo(
  ({ incidents, isLoading, isError, onRetry }: RecentIncidentsListProps) => {
    const { t } = useTranslation();
    const navigate = useNavigate();

    // Permet d'ouvrir le detail d'un incident depuis le widget "Activite
    // recente". Gere au niveau du composant pour ne pas rappeler le hook
    // navigate dans la boucle de rendu, et exposer un onKeyDown clavier.
    const handleOpen = useCallback(
      (id: string) => () =>
        incidentNavigation.navigateToIncidentDetail(navigate, id),
      [navigate],
    );

    // Traite la navigation clavier.
    const handleKeyDown = useCallback(
      (id: string) => (event: React.KeyboardEvent<HTMLDivElement>) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          incidentNavigation.navigateToIncidentDetail(navigate, id);
        }
      },
      [navigate],
    );

    // Cap explicite a 10 entrees : au-dela, l'utilisateur passe par "Voir tout".
    // useMemo est place avant les retours anticipes pour respecter les regles des hooks.
    const visibleIncidents = useMemo(
      () => incidents.slice(0, RECENT_ACTIVITY_LIMIT),
      [incidents],
    );

    if (isLoading) {
      return <SkeletonList />;
    }

    if (isError) {
      return (
        <Alert
          type="error"
          showIcon
          message={t("dashboard.recentActivity.error")}
          action={
            onRetry && (
              <Button size="small" onClick={onRetry}>
                {t("common.retry")}
              </Button>
            )
          }
          className={styles.errorAlert}
        />
      );
    }

    if (incidents.length === 0) {
      return (
        <Empty
          description={t("dashboard.recentActivity.empty")}
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          className={styles.empty}
        />
      );
    }

    return (
      <div className={styles.list} role="list">
        {visibleIncidents.map((incident) => (
          <div
            key={incident.id}
            className={`${styles.listItem} ${styles.listItemInteractive ?? ""}`}
            role="listitem"
            tabIndex={0}
            onClick={
              incident.incidentId ? handleOpen(incident.incidentId) : undefined
            }
            onKeyDown={
              incident.incidentId
                ? handleKeyDown(incident.incidentId)
                : undefined
            }
            aria-label={t("dashboard.recentActivity.open_aria", {
              title: incident.incidentTitle,
            })}
          >
            {/* La grille interne sépare le contenu textuel (titre, badges,
                date) de la flèche : sans cette structure, un titre long
                pouvait écraser les badges et déborder l'item. Tout est
                tronqué proprement avec ellipsis + tooltip natif. */}
            <div className={styles.itemBody}>
              <Text
                strong
                className={styles.title}
                ellipsis={{
                  tooltip: incident.incidentTitle || t("common.unknown"),
                }}
              >
                {incident.incidentTitle || t("common.unknown")}
              </Text>

              <div className={styles.metaRow}>
                <Tag
                  color={ACTION_TYPE_COLORS[incident.action]}
                  className={styles.metaTag}
                >
                  {t(`incidents.action_type.${incident.action}`, {
                    defaultValue: incident.action,
                  })}
                </Tag>
                <Text type="secondary" className={styles.date}>
                  {formatDate(incident.createdAt)}
                </Text>
                <Text type="secondary" className={styles.author}>
                  {formatUserName(incident.user)}
                </Text>
              </div>
            </div>
            <span aria-hidden="true" className={styles.chevron}>
              ›
            </span>
          </div>
        ))}
      </div>
    );
  },
);

RecentIncidentsList.displayName = "RecentIncidentsList";

export default RecentIncidentsList;
