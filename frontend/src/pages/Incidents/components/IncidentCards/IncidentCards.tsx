// Composant React : porte l'interface de incident cards.

import { Col, Empty, Row, Tag, Typography } from "antd";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import type { IncidentSummaryResponse } from "../../../../api/incident/types";
import { STATUS_COLORS } from "../../../../api/incident/types";
import { Card, Button, StatusTag } from "../../../../components/ui";
import { useAuth } from "../../../../hooks/auth/useAuth";
import { incidentIcons } from "../../../../utils/icons/appIcons";
import { getIncidentSlaInfo } from "../../../../utils/incidents/incidentTiming";
import {
  formatDateTime,
  formatUserName,
} from "../../../../utils/formatters/formatters";
import {
  incidentNavigation,
  incidentPathIdentifier,
} from "../../../../utils/navigation/incidents/incidents";
import styles from "./IncidentCards.module.scss";

const { Text } = Typography;

// Definit les proprietes attendues par le composant IncidentCards.
interface IncidentCardsProps {
  incidents: IncidentSummaryResponse[];
  loading: boolean;
}

// Rend le composant IncidentCards pour l'interface incident cards.
const IncidentCards = ({ incidents, loading }: IncidentCardsProps) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user } = useAuth();

  // Traite l'action utilisateur liee a open details.
  const handleOpenDetails = useCallback(
    (incidentId: string) => {
      incidentNavigation.navigateToIncidentDetail(navigate, incidentId);
    },
    [navigate],
  );

  if (!loading && incidents.length === 0) {
    return (
      <Empty
        description={t("incidents.empty_description")}
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        className={styles.empty}
      />
    );
  }

  return (
    <Row gutter={[16, 16]} className={styles.grid}>
      {incidents.map((incident) => {
        const sla = getIncidentSlaInfo(incident);
        const assigneeName = formatUserName(
          incident.assignedTo,
          null,
          t("incidents.detail.no_assigned"),
        );
        const assigneeInitial = incident.assignedTo
          ? assigneeName.charAt(0).toUpperCase()
          : null;

        const isMyResponsibility =
          user?.id === incident.assignedTo?.id ||
          user?.agencyId === incident.agency?.id;

        return (
          <Col xs={24} sm={12} md={12} lg={8} xl={6} key={incident.id}>
            <Card
              hoverable
              loading={loading}
              className={`${styles.card} ${sla.state === "overdue" ? styles.overdueCard : ""} ${["OPEN", "PENDING_VALIDATION"].includes(incident.status) ? styles.newIncidentCard : ""}`}
            >
              <div className={styles.contentWrapper}>
                <div className={styles.headerArea}>
                  <div className={styles.titleGroup}>
                    {incident.reference && (
                      <Text type="secondary" className={styles.referenceCode}>
                        {incident.reference}
                      </Text>
                    )}
                    <Text
                      strong
                      ellipsis
                      className={styles.incidentTitle}
                      title={incident.title}
                    >
                      {["OPEN", "PENDING_VALIDATION"].includes(
                        incident.status,
                      ) && (
                        <span
                          className={styles.newIncidentDot}
                          title={t("incidents.status.OPEN")}
                        />
                      )}
                      {incident.title}
                    </Text>
                    <StatusTag
                      status={incident.status}
                      label={t(`incidents.status.${incident.status}`)}
                      colorMap={STATUS_COLORS}
                      className={styles.statusTag}
                    />
                    {isMyResponsibility && (
                      <Tag className={styles.myResponsibilityTag}>
                        {t("incidents.tags.myResponsibility")}
                      </Tag>
                    )}
                  </div>
                </div>

                <div className={styles.metaRow}>
                  <div className={styles.metaCol}>
                    <Text type="secondary" className={styles.metaLabel}>
                      {t("incidents.table.criticality")}
                    </Text>
                    <Tag
                      className={`${styles.glassTag} ${styles[`criticality-${incident.criticality.toLowerCase()}`]}`}
                    >
                      {incidentIcons.criticality}{" "}
                      {t(`incidents.criticality.${incident.criticality}`)}
                    </Tag>
                  </div>
                  <div className={styles.metaCol}>
                    <Text type="secondary" className={styles.metaLabel}>
                      {t("incidents.table.type")}
                    </Text>
                    <Text
                      ellipsis
                      className={styles.typeValue}
                      title={
                        incident.type?.displayName ?? incident.type?.name
                      }
                    >
                      {incident.type?.displayName ?? incident.type?.name}
                    </Text>
                  </div>
                </div>

                <div className={styles.contextGrid}>
                  <div className={styles.infoField}>
                    <Text type="secondary" className={styles.metaLabel}>
                      {incidentIcons.user}{" "}
                      {t("incidents.table.assigned_to")}
                    </Text>
                    <div className={styles.assigneeValue}>
                      {assigneeInitial ? (
                        <span className={styles.avatarCircle}>
                          {assigneeInitial}
                        </span>
                      ) : (
                        <span className={styles.emptyAvatar}>
                          {incidentIcons.user}
                        </span>
                      )}
                      <Text
                        ellipsis
                        className={styles.infoValue}
                        title={assigneeName}
                      >
                        {assigneeName}
                      </Text>
                    </div>
                  </div>
                  <div className={styles.infoField}>
                    <Text type="secondary" className={styles.metaLabel}>
                      {t("incidents.table.agency")}
                    </Text>
                    <Text
                      ellipsis
                      className={styles.infoValue}
                      title={incident.agency?.name}
                    >
                      {incident.agency?.name ?? "—"}
                    </Text>
                  </div>
                </div>

                <div className={styles.bottomArea}>
                  <div className={styles.dateGrid}>
                    <div className={styles.dateField}>
                      <Text type="secondary" className={styles.metricLabel}>
                        {incidentIcons.createdAt}{" "}
                        {t("incidents.table.created_at")}
                      </Text>
                      <Text className={styles.metricValue}>
                        {formatDateTime(incident.createdAt)}
                      </Text>
                    </div>
                    <div className={styles.dateField}>
                      <Text type="secondary" className={styles.metricLabel}>
                        {incidentIcons.dueDate}{" "}
                        {t("incidents.table.due_date")}
                      </Text>
                      <Text
                        className={`${styles.metricValue} ${sla.state === "overdue" ? styles.metricValueOverdue : ""}`}
                      >
                        {incident.dueDate
                          ? formatDateTime(incident.dueDate)
                          : t("incidents.detail.no_due_date")}
                      </Text>
                    </div>
                  </div>

                  <Button
                    variant="primary"
                    className={styles.detailsButton}
                    onClick={() =>
                      handleOpenDetails(incidentPathIdentifier(incident))
                    }
                  >
                    {t("incidents.cards.details")}
                  </Button>
                </div>
              </div>
            </Card>
          </Col>
        );
      })}
    </Row>
  );
};

export default IncidentCards;
