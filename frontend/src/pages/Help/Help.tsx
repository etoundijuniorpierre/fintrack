// Centre d'aide : présente uniquement les informations utiles aux permissions effectives de l'utilisateur.
import React, { memo, useMemo, useState } from "react";
import { Button, Divider, Modal, Tabs, Typography } from "antd";
import { Trans, useTranslation } from "react-i18next";
import { PageContainer, SectionCard } from "../../components/Layout";
import { PageHeader } from "../../components/ui";
import {
  Criticality,
  IncidentCause,
  IncidentStatus,
} from "../../api/incident/enums/enums";
import { useAuth } from "../../hooks/auth/useAuth";
import {
  PERMISSIONS,
  USER_CREATE_PERMISSIONS,
  USER_VIEW_PERMISSIONS,
  hasAnyPermission,
  type PermissionChecker,
} from "../../utils/permissions/permissions";
import { detailIcons } from "../../utils/icons/appIcons";
import FieldGuideSection from "./components/FieldGuideSection";
import IncidentTypesSection from "./components/IncidentTypesSection";
import IncidentForm from "../Incidents/components/Form/IncidentForm";
import { FAQ_KEYS, TEXT_SECTIONS } from "../../utils/help/helpContent";
import styles from "./Help.module.scss";

const { Paragraph, Text } = Typography;
const STATUS_CODES = Object.values(IncidentStatus);
const CRITICALITY_CODES = Object.values(Criticality);
const CAUSE_CODES = Object.values(IncidentCause);

interface HelpTopicLike {
  key: string;
  requiredPermissions?: readonly string[];
}

// Retient seulement les rubriques dont la permission est effectivement accordée.
const filterVisible = <T extends HelpTopicLike>(
  entries: T[],
  hasPermission: PermissionChecker,
): T[] =>
  entries.filter(
    (entry) =>
      !entry.requiredPermissions ||
      hasAnyPermission(hasPermission, entry.requiredPermissions),
  );

// Rend le centre d'aide et masque les informations liées aux droits absents.
const Help = memo(() => {
  const { t } = useTranslation();
  const { hasPermission } = useAuth();
  const [isSupportModalVisible, setIsSupportModalVisible] = useState(false);

  const canViewIncidents = hasAnyPermission(
    hasPermission,
    Object.values(PERMISSIONS.INCIDENT),
  );
  const canViewIncidentTypes = hasAnyPermission(hasPermission, [
    PERMISSIONS.SETTINGS.INCIDENT_TYPES,
    PERMISSIONS.INCIDENT.CREATE,
    PERMISSIONS.INCIDENT.VIEW_AGENCY,
    PERMISSIONS.INCIDENT.VIEW_ALL,
  ]);
  const canViewNotifications = hasAnyPermission(hasPermission, [
    PERMISSIONS.NOTIFICATION.VIEW_OWN,
    PERMISSIONS.NOTIFICATION.VIEW_ALL,
    PERMISSIONS.NOTIFICATION.MANAGE,
  ]);
  const canViewReports = hasAnyPermission(hasPermission, [
    PERMISSIONS.REPORT.VIEW_OWN,
    PERMISSIONS.REPORT.VIEW_SERVICE,
    PERMISSIONS.REPORT.VIEW_AGENCY,
    PERMISSIONS.REPORT.VIEW_ALL,
    PERMISSIONS.REPORT.GENERATE,
    PERMISSIONS.REPORT.EXPORT,
    PERMISSIONS.REPORT.SEND_EMAIL,
    PERMISSIONS.REPORT.DELETE,
  ]);
  const canViewUsers = hasAnyPermission(hasPermission, [
    ...USER_VIEW_PERMISSIONS,
    ...USER_CREATE_PERMISSIONS,
    PERMISSIONS.USER.UPDATE,
    PERMISSIONS.USER.DELETE,
    PERMISSIONS.ROLE.ASSIGN,
  ]);
  const canViewProfile = hasPermission(PERMISSIONS.USER.MANAGE_PROFILE);
  const canViewDashboard =
    canViewIncidents || hasPermission(PERMISSIONS.DASHBOARD.CONFIGURE);
  const canViewSettings = hasAnyPermission(hasPermission, [
    PERMISSIONS.SETTINGS.SYSTEM,
    PERMISSIONS.SETTINGS.INCIDENT_TYPES,
    PERMISSIONS.ROLE.CREATE,
    PERMISSIONS.ROLE.ASSIGN,
    ...USER_CREATE_PERMISSIONS,
  ]);

  const renderTopicBlocks = <T extends HelpTopicLike>(
    topics: T[],
    title: (topic: T) => string,
    body: (topic: T) => string,
  ) => {
    const visible = filterVisible(topics, hasPermission);
    return visible.map((topic, index) => (
      <div key={topic.key}>
        <Paragraph>
          <Text strong className={styles.topicTitle}>
            {t(title(topic))}
          </Text>
          <br />
          <Text type="secondary">
            <Trans i18nKey={body(topic)} />
          </Text>
        </Paragraph>
        {index < visible.length - 1 && (
          <Divider className={styles.topicDivider} />
        )}
      </div>
    ));
  };

  const renderParagraphs = (sectionKey: string) =>
    renderTopicBlocks(
      TEXT_SECTIONS[sectionKey] ?? [],
      (topic) => `help.sections.${sectionKey}.items.${topic.key}.title`,
      (topic) => `help.sections.${sectionKey}.items.${topic.key}.body`,
    );

  const renderCatalog = (
    codes: string[],
    labelKey: (code: string) => string,
    catalogKey: (code: string) => string,
  ) =>
    renderTopicBlocks(
      codes.map((code) => ({ key: code })),
      (topic) => labelKey(topic.key),
      (topic) => catalogKey(topic.key),
    );

  const items = useMemo(() => {
    const tabs: {
      key: string;
      label: string;
      children: React.ReactNode;
    }[] = [
      {
        key: "workflow",
        label: t("help.sections.workflow.title"),
        children: renderParagraphs("workflow"),
      },
    ];

    if (canViewIncidents) {
      tabs.push(
        {
          key: "managing",
          label: t("help.sections.managing.title"),
          children: renderParagraphs("managing"),
        },
        {
          key: "incidentDetails",
          label: t("help.sections.incidentDetails.title"),
          children: renderParagraphs("incidentDetails"),
        },
        {
          key: "statuses",
          label: t("help.sections.statuses.title"),
          children: (
            <>
              <Paragraph type="secondary" className={styles.sectionIntro}>
                {t("help.sections.statuses.intro")}
              </Paragraph>
              {renderCatalog(
                STATUS_CODES,
                (code) => `incidents.status.${code}`,
                (code) => `help.statuses_catalog.${code}`,
              )}
            </>
          ),
        },
        {
          key: "criticality",
          label: t("help.sections.criticality.title"),
          children: (
            <>
              <Paragraph type="secondary" className={styles.sectionIntro}>
                {t("help.sections.criticality.intro")}
              </Paragraph>
              {renderCatalog(
                CRITICALITY_CODES,
                (code) => `incidents.criticality.${code}`,
                (code) => `help.criticality_catalog.${code}`,
              )}
            </>
          ),
        },
        {
          key: "causes",
          label: t("help.sections.causes.title"),
          children: (
            <>
              <Paragraph type="secondary" className={styles.sectionIntro}>
                {t("help.sections.causes.intro")}
              </Paragraph>
              {renderCatalog(
                CAUSE_CODES,
                (code) => `incidents.cause_values.${code}`,
                (code) => `help.causes_catalog.${code}`,
              )}
            </>
          ),
        },
      );
    }

    if (
      hasAnyPermission(hasPermission, [
        PERMISSIONS.INCIDENT.CREATE,
        PERMISSIONS.INCIDENT.UPDATE,
      ])
    ) {
      tabs.push({
        key: "fields",
        label: t("help.sections.fields.title"),
        children: <FieldGuideSection />,
      });
    }

    if (canViewIncidentTypes) {
      tabs.push({
        key: "types",
        label: t("help.sections.types.title"),
        children: <IncidentTypesSection />,
      });
    }

    if (canViewDashboard) {
      tabs.push({
        key: "dashboard",
        label: t("help.sections.dashboard.title"),
        children: renderParagraphs("dashboard"),
      });
    }

    if (canViewNotifications) {
      tabs.push({
        key: "notifications",
        label: t("help.sections.notifications.title"),
        children: renderParagraphs("notifications"),
      });
    }

    if (canViewReports) {
      tabs.push({
        key: "reports",
        label: t("help.sections.reports.title"),
        children: renderParagraphs("reports"),
      });
    }

    if (canViewProfile) {
      tabs.push({
        key: "profile",
        label: t("help.sections.profile.title"),
        children: renderParagraphs("profile"),
      });
    }

    if (canViewUsers) {
      tabs.push({
        key: "users",
        label: t("help.sections.users.title"),
        children: renderParagraphs("users"),
      });
    }

    if (hasPermission(PERMISSIONS.AUDIT.VIEW)) {
      tabs.push({
        key: "audit",
        label: t("help.sections.audit.title"),
        children: renderParagraphs("audit"),
      });
    }

    if (canViewSettings) {
      tabs.push({
        key: "settings",
        label: t("help.sections.settings.title"),
        children: renderParagraphs("settings"),
      });
    }

    tabs.push({
      key: "faq",
      label: t("help.sections.faq.title"),
      children: renderTopicBlocks(
        FAQ_KEYS,
        (topic) => `help.sections.faq.items.${topic.key}.q`,
        (topic) => `help.sections.faq.items.${topic.key}.a`,
      ),
    });

    return tabs;
    // Les assistants de rendu partagent volontairement le vérificateur courant.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    t,
    hasPermission,
    canViewIncidents,
    canViewIncidentTypes,
    canViewNotifications,
    canViewReports,
    canViewUsers,
    canViewProfile,
    canViewDashboard,
    canViewSettings,
  ]);

  return (
    <PageContainer>
      <PageHeader
        title={t("help.title")}
        subtitle={t("help.subtitle")}
        actions={
          <Button
            type="primary"
            icon={detailIcons.mail}
            onClick={() => setIsSupportModalVisible(true)}
            disabled
          >
            {t("help.support.button")}
          </Button>
        }
      />

      <SectionCard>
        <Paragraph>
          <Text strong>{t("help.intro.title")}</Text>
        </Paragraph>
        <Paragraph>{t("help.intro.tagline")}</Paragraph>
        <Paragraph>{t("help.intro.description")}</Paragraph>
        <Paragraph type="secondary">{t("help.intro.audience")}</Paragraph>
      </SectionCard>

      <SectionCard>
        <Tabs tabPosition="left" defaultActiveKey="workflow" items={items} />
      </SectionCard>

      <Modal
        title={t("help.support.modal_title")}
        open={isSupportModalVisible}
        onCancel={() => setIsSupportModalVisible(false)}
        footer={null}
        width={800}
        destroyOnClose
      >
        <IncidentForm
          onSuccess={() => setIsSupportModalVisible(false)}
          onCancel={() => setIsSupportModalVisible(false)}
        />
      </Modal>
    </PageContainer>
  );
});

Help.displayName = "Help";

export default Help;
