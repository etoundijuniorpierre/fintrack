import { memo } from "react";
import { Alert, Descriptions, List, Spin, Tag, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { useQuery } from "@tanstack/react-query";
import { incidentApi } from "../../../../api/incident/incidentApi/incidentApi";
import type { IncidentTypeConfigResponse } from "../../../../api/incident/types";
import styles from "../../Help.module.scss";

const { Paragraph, Text } = Typography;

// Retourne le libelle lisible d'un utilisateur cible configure par defaut.
const getUserLabel = (
  user: IncidentTypeConfigResponse["defaultTargetUser"],
): string => {
  if (!user) return "";
  return (
    [user.firstName, user.lastName].filter(Boolean).join(" ") || user.username
  );
};

// Section exposant les types d'incidents dynamiques configurés dans le système.
export const IncidentTypesSection = memo(() => {
  const { t } = useTranslation();

  const {
    data: incidentTypes = [],
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["help", "incidentTypes"],
    queryFn: () => incidentApi.getIncidentTypes(),
    // On garde en cache longtemps car ça change rarement
    staleTime: 1000 * 60 * 30,
  });

  if (isLoading) {
    return <Spin size="small" data-testid="loading-spinner" />;
  }

  if (isError) {
    return <Alert type="error" message={t("common.error")} />;
  }

  // Ne garder que les types actifs pour le guide utilisateur
  const activeTypes = incidentTypes.filter(
    (type: IncidentTypeConfigResponse) => type.isActive,
  );

  return (
    <div>
      <Paragraph type="secondary" className={styles.sectionIntro}>
        {t("help.sections.types.intro")}
      </Paragraph>

      <List
        size="small"
        dataSource={activeTypes}
        renderItem={(type: IncidentTypeConfigResponse) => (
          <List.Item>
            <div>
              <Text strong>{type.displayName}</Text>
              {type.description && (
                <>
                  {" — "}
                  <Text>{type.description}</Text>
                </>
              )}
              <Descriptions
                size="small"
                column={1}
                className={styles.typeDescriptions}
              >
                <Descriptions.Item
                  label={t("help.sections.types.defaultService")}
                >
                  {type.defaultTargetService?.name ??
                    t("help.sections.types.noDefaultService")}
                </Descriptions.Item>
                <Descriptions.Item
                  label={t("help.sections.types.defaultUser")}
                >
                  {getUserLabel(type.defaultTargetUser) ||
                    t("help.sections.types.noDefaultUser")}
                </Descriptions.Item>
                <Descriptions.Item label={t("help.sections.types.sla")}>
                  {type.slaHours != null
                    ? `${type.slaHours} h`
                    : t("help.sections.types.noSla")}
                </Descriptions.Item>
                <Descriptions.Item
                  label={t("help.sections.types.validation")}
                >
                  <Tag color={type.requiresValidation ? "blue" : "default"}>
                    {t(
                      type.requiresValidation
                        ? "help.sections.types.boolean.yes"
                        : "help.sections.types.boolean.no",
                    )}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label={t("help.sections.types.validator")}>
                  {t(
                    `help.sections.types.validatorScopes.${type.validatorScope}`,
                    { defaultValue: type.validatorScope },
                  )}
                </Descriptions.Item>
                <Descriptions.Item label={t("help.sections.types.closer")}>
                  {type.closerRoles && type.closerRoles.length > 0
                    ? type.closerRoles
                        .map((role) =>
                          t(`settings.incidentTypes.form.actorRoles.${role}`),
                        )
                        .join(", ")
                    : "-"}
                </Descriptions.Item>
                <Descriptions.Item
                  label={t("help.sections.types.causeAnalysis")}
                >
                  {t(
                    type.requiresCauseAnalysis
                      ? "help.sections.types.boolean.yes"
                      : "help.sections.types.boolean.no",
                  )}
                </Descriptions.Item>
                <Descriptions.Item
                  label={t("help.sections.types.notifications")}
                >
                  {t(
                    type.emailNotificationsEnabled
                      ? "help.sections.types.boolean.enabled"
                      : "help.sections.types.boolean.disabled",
                  )}
                </Descriptions.Item>
              </Descriptions>
            </div>
          </List.Item>
        )}
      />
    </div>
  );
});

IncidentTypesSection.displayName = "IncidentTypesSection";
