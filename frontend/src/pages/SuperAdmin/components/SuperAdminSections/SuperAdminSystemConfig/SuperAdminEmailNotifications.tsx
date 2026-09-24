// Composant React : reglages d'envoi e-mail par evenement (interrupteur + liste
// d'utilisateurs exclus), pilotes par le Super Admin et lus en runtime par les
// services emetteurs.

import { useMemo, useState, useEffect } from "react";
import { App, Button, Card, Divider, Select, Space, Typography } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { superAdminApi } from "../../../../../api/superAdmin";
import { AppSwitch } from "../../../../../components/ui";
import type {
  EmailNotificationEventSetting,
  EmailNotificationSettings,
} from "../../../../../api/superAdmin";
import { useUsers } from "../../../../../hooks/user/useUsers/useUsers";
import { QUERY_KEYS } from "../../../../../utils/constants";
import styles from "../../../SuperAdminPage.module.scss";

const { Title, Paragraph, Text } = Typography;

// Ordre d'affichage des familles.
const CATEGORY_ORDER = ["LIFECYCLE", "SCHEDULED", "DIRECTION"] as const;

// Rend la section des reglages e-mail par evenement.
export const SuperAdminEmailNotifications = () => {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const { data: users = [] } = useUsers();

  const { data, isLoading } = useQuery({
    queryKey: [...QUERY_KEYS.SUPER_ADMIN.SYSTEM_CONFIG, "email-notifications"],
    queryFn: () => superAdminApi.getEmailNotifications(),
  });

  // Copie editable indexee par evenement.
  const [draft, setDraft] = useState<
    Record<string, EmailNotificationEventSetting>
  >({});

  useEffect(() => {
    if (!data?.events) return;
    const next: Record<string, EmailNotificationEventSetting> = {};
    data.events.forEach((event) => {
      next[event.event] = {
        ...event,
        excludedUserIds: [...(event.excludedUserIds ?? [])],
      };
    });
    setDraft(next);
  }, [data]);

  const userOptions = useMemo(
    () =>
      users.map((user) => ({
        label:
          [user.firstName, user.lastName].filter(Boolean).join(" ") ||
          user.username,
        value: user.id,
      })),
    [users],
  );

  const grouped = useMemo(() => {
    const map: Record<string, EmailNotificationEventSetting[]> = {};
    (data?.events ?? []).forEach((event) => {
      (map[event.category] ??= []).push(event);
    });
    return map;
  }, [data]);

  const mutation = useMutation({
    mutationFn: (settings: EmailNotificationSettings) =>
      superAdminApi.updateEmailNotifications(settings),
    onSuccess: () => {
      message.success(t("superAdmin.config.emailNotifications.saveSuccess"));
      queryClient.invalidateQueries({
        queryKey: [
          ...QUERY_KEYS.SUPER_ADMIN.SYSTEM_CONFIG,
          "email-notifications",
        ],
      });
    },
    onError: () =>
      message.error(t("superAdmin.config.emailNotifications.saveError")),
  });

  const setEnabled = (eventKey: string, enabled: boolean) =>
    setDraft((prev) => ({
      ...prev,
      [eventKey]: { ...prev[eventKey], enabled },
    }));

  const setExcluded = (eventKey: string, excludedUserIds: string[]) =>
    setDraft((prev) => ({
      ...prev,
      [eventKey]: { ...prev[eventKey], excludedUserIds },
    }));

  const handleSave = () =>
    mutation.mutate({ events: Object.values(draft) });

  return (
    <Card
      title={t("superAdmin.config.emailNotifications.title")}
      loading={isLoading}
      variant="outlined"
    >
      <Paragraph type="secondary">
        {t("superAdmin.config.emailNotifications.intro")}
      </Paragraph>

      {CATEGORY_ORDER.filter((category) => grouped[category]?.length).map(
        (category) => (
          <div key={category} className={styles.thresholdGroup}>
            <Title level={5}>
              {t(`superAdmin.config.emailNotifications.categories.${category}`)}
            </Title>
            <Paragraph type="secondary" className={styles.configHelp}>
              {t(
                `superAdmin.config.emailNotifications.categoryHints.${category}`,
              )}
            </Paragraph>

            {grouped[category].map((event) => {
              const current = draft[event.event] ?? event;
              return (
                <div key={event.event} style={{ marginBottom: 16 }}>
                  <Space
                    align="center"
                    style={{
                      justifyContent: "space-between",
                      width: "100%",
                    }}
                  >
                    <Text strong>
                      {t(
                        `superAdmin.config.emailNotifications.events.${event.event}`,
                      )}
                    </Text>
                    <AppSwitch
                      checked={current.enabled}
                      onChange={(value) => setEnabled(event.event, value)}
                    />
                  </Space>
                  {event.supportsExclusion && (
                    <Select
                      mode="multiple"
                      allowClear
                      disabled={!current.enabled}
                      style={{ width: "100%", marginTop: 8 }}
                      placeholder={t(
                        "superAdmin.config.emailNotifications.excludePlaceholder",
                      )}
                      options={userOptions}
                      value={current.excludedUserIds}
                      onChange={(value) => setExcluded(event.event, value)}
                      optionFilterProp="label"
                      showSearch
                    />
                  )}
                  <Divider style={{ margin: "12px 0 0" }} />
                </div>
              );
            })}
          </div>
        ),
      )}

      <Button
        type="primary"
        loading={mutation.isPending}
        onClick={handleSave}
        disabled={isLoading}
      >
        {t("superAdmin.config.emailNotifications.save")}
      </Button>
    </Card>
  );
};
