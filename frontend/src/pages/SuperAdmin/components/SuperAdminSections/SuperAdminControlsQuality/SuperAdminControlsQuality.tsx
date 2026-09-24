// Composant React : porte l'interface de super-administration controles qualite.

import type { NavigateFunction } from "react-router-dom";
import { QuestionCircleOutlined } from "@ant-design/icons";
import { Button, Col, List, Row, Space, Tag, Tooltip, Typography } from "antd";
import { useTranslation } from "react-i18next";
import type { ControlsQualitySection } from "../../../../../api/superAdmin";
import { SectionCard } from "../../../../../components/Layout";
import { APP_ROUTES } from "../../../../../utils/constants";
import {
  asArray,
  asNumber,
  asRecord,
  asString,
  severityColor,
} from "../../../../../utils/superAdmin/superAdminSectionUtils/superAdminSectionUtils";
import styles from "../../../SuperAdminPage.module.scss";

const { Text, Paragraph } = Typography;

// Definit les proprietes attendues par le composant ControlsQuality.
interface ControlsQualityProps {
  section?: ControlsQualitySection;
  loading: boolean;
  navigate: NavigateFunction;
}

// Filtre les options disponibles pour super-administration controles qualite.
const filteredRouteForIssue = (key: string): string => {
  switch (key) {
    case "incidents-without-agency":
      return `${APP_ROUTES.INCIDENTS}?missingField=agency`;
    case "incidents-without-service":
      return `${APP_ROUTES.INCIDENTS}?missingField=service`;
    case "incidents-assigned-inactive-user":
      return `${APP_ROUTES.INCIDENTS}?assignedToInactive=true`;
    case "users-without-role":
      return `${APP_ROUTES.USERS}?missingField=role`;
    case "users-without-scope":
      return `${APP_ROUTES.USERS}?missingField=scope`;
    case "reports-without-file":
      return `${APP_ROUTES.REPORTS}?missingFile=true`;
    case "sent-notifications-without-trace":
      return `${APP_ROUTES.NOTIFICATIONS}?missingTrace=true`;
    case "invalid-recipient-notifications":
      return `${APP_ROUTES.NOTIFICATIONS}?invalidRecipient=true`;
    case "agencies-without-head":
      return `${APP_ROUTES.SETTINGS}?missingHead=true#agencies`;
    case "services-without-head":
      return `${APP_ROUTES.SETTINGS}?missingHead=true#services`;
    case "unused-incident-types":
      return `${APP_ROUTES.SETTINGS}?unused=true#incidentTypes`;
    default:
      return APP_ROUTES.INCIDENTS;
  }
};

const ALL_QUALITY_CHECKS: Array<{
  key: string;
  severity: "high" | "medium" | "low";
}> = [
  { key: "incidents-without-agency", severity: "high" },
  { key: "incidents-without-service", severity: "high" },
  { key: "incidents-assigned-inactive-user", severity: "high" },
  { key: "users-without-role", severity: "medium" },
  { key: "users-without-scope", severity: "medium" },
  { key: "reports-without-file", severity: "medium" },
  { key: "sent-notifications-without-trace", severity: "medium" },
  { key: "invalid-recipient-notifications", severity: "medium" },
  { key: "agencies-without-head", severity: "low" },
  { key: "services-without-head", severity: "low" },
  { key: "unused-incident-types", severity: "low" },
];

const INLINE_DETAIL_FIELD: Record<string, string> = {
  "agencies-without-head": "agenciesWithoutHead",
  "services-without-head": "servicesWithoutHead",
  "reports-without-file": "reportsWithoutFile",
  "sent-notifications-without-trace": "sentNotificationsWithoutTrace",
  "invalid-recipient-notifications": "invalidRecipients",
  "unused-incident-types": "unusedIncidentTypes",
  "incidents-without-agency": "incidentsWithoutAgency",
  "incidents-without-service": "incidentsWithoutService",
  "incidents-assigned-inactive-user": "incidentsAssignedInactiveUser",
  "users-without-role": "usersWithoutRole",
  "users-without-scope": "usersWithoutScope",
};

// Rend le composant SuperAdminControlsQuality pour l'interface super-administration controles qualite.
export const SuperAdminControlsQuality = ({
  section,
  loading,
  navigate,
}: ControlsQualityProps) => {
  const { t } = useTranslation();
  const dataQuality = asRecord(section?.dataQuality);
  const countByKey = new Map<string, number>(
    asArray(dataQuality.issues).map((issue) => [
      asString(issue.key),
      asNumber(issue.count),
    ]),
  );

  return (
    <Row gutter={[16, 16]}>
      <Col xs={24}>
        <SectionCard title={t("superAdmin.sections.quality")} loading={loading}>
          <Paragraph type="secondary">
            {t("superAdmin.quality.intro")}
          </Paragraph>
          <List
            dataSource={ALL_QUALITY_CHECKS}
            renderItem={({ key, severity }) => {
              const count = countByKey.get(key) ?? 0;
              const why = t(`superAdmin.qualityIssues.why.${key}`, "");
              const fix = t(`superAdmin.qualityIssues.fix.${key}`, "");
              const tooltip =
                why || fix ? (
                  <div style={{ maxWidth: 360 }}>
                    {why && (
                      <div>
                        <strong>{t("superAdmin.quality.whyLabel")}</strong>{" "}
                        {why}
                      </div>
                    )}
                    {fix && (
                      <div style={{ marginTop: 6 }}>
                        <strong>{t("superAdmin.quality.fixLabel")}</strong>{" "}
                        {fix}
                      </div>
                    )}
                  </div>
                ) : (
                  t("superAdmin.quality.noHelp")
                );

              const detailField = INLINE_DETAIL_FIELD[key];
              const detailItems = detailField
                ? asArray(dataQuality[detailField])
                : [];
              const actions =
                count > 0
                  ? [
                      <Button
                        key="open"
                        type="link"
                        onClick={() => navigate(filteredRouteForIssue(key))}
                      >
                        {t("superAdmin.quality.viewItemsButton")}
                      </Button>,
                    ]
                  : [];

              return (
                <List.Item actions={actions}>
                  <List.Item.Meta
                    title={
                      <Space>
                        <Tag color={severityColor(severity)}>
                          {t(`superAdmin.severity.${severity}`)}
                        </Tag>
                        <Text>{t(`superAdmin.qualityIssues.${key}`, key)}</Text>
                        <Tooltip title={tooltip}>
                          <QuestionCircleOutlined
                            style={{ color: "#8c8c8c", cursor: "help" }}
                          />
                        </Tooltip>
                      </Space>
                    }
                    description={
                      <Space
                        orientation="vertical"
                        size={4}
                        className={styles.fullWidth}
                      >
                        {why && (
                          <Text type="secondary" style={{ fontSize: 12 }}>
                            {why}
                          </Text>
                        )}
                        {count === 0 ? (
                          <Tag color="success">
                            {t("superAdmin.quality.na")}
                          </Tag>
                        ) : (
                          <Space
                            direction="vertical"
                            size={4}
                            className={styles.fullWidth}
                          >
                            <Text>
                              {t("superAdmin.quality.count", { count })}
                            </Text>
                            {detailField && detailItems.length > 0 && (
                              <Space wrap>
                                {detailItems.slice(0, 20).map((item) => (
                                  <Tag
                                    key={asString(item.id, asString(item.name))}
                                  >
                                    {asString(item.name)}
                                  </Tag>
                                ))}
                              </Space>
                            )}
                          </Space>
                        )}
                      </Space>
                    }
                  />
                </List.Item>
              );
            }}
          />
        </SectionCard>
      </Col>
    </Row>
  );
};
