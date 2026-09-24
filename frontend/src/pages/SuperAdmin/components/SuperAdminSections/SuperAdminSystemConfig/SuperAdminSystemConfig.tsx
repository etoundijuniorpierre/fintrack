// Composant React : porte l'interface de super-administration systeme configuration.

import { useEffect } from "react";
import { App, Button, Card, Col, Form, InputNumber, Row, Select, Space, Tag, Typography } from "antd";
import {
  ClockCircleOutlined,
  CloudServerOutlined,
  CloudUploadOutlined,
  ControlOutlined,
  FileTextOutlined,
  SafetyOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { superAdminApi } from "../../../../../api/superAdmin";
import type { SystemConfigSection } from "../../../../../api/superAdmin";
import { SectionCard } from "../../../../../components/Layout";
import { AppSwitch } from "../../../../../components/ui";
import { QUERY_KEYS } from "../../../../../utils/constants";
import { asRecord } from "../../../../../utils/superAdmin/superAdminSectionUtils/superAdminSectionUtils";
import { SuperAdminEmailNotifications } from "./SuperAdminEmailNotifications";
import styles from "../../../SuperAdminPage.module.scss";

const { Title, Paragraph } = Typography;

// Definit les proprietes attendues par le composant SystemConfig.
interface SystemConfigProps {
  section?: SystemConfigSection;
  loading: boolean;
}

const THRESHOLD_KEYS = [
  "defaultSlaHours",
  "criticalIncidentHours",
  "slaReminderIntervalHours",
  "maxTransfersBeforeAlert",
  "notificationMaxRetryCount",
  "loginMaxFailedAttempts",
  "tempPasswordValidityMinutes",
  "escalationScanIntervalMinutes",
  "maxReopenCount",
  "reopenTimeLimitHours",
  "autoBlockOverdueWorkingDays",
  "blockedReminderIntervalDays",
  "criticalReminderIntervalHours",
  "prolongedWaitDays",
  "validationDelayHours",
  "validationReminderEnabled",
  "serviceManagerSelfValidationEnabled",
  "pendingActionReminderIntervalHours",
  "pendingActionInternalEnabled",
  "reportRetentionDays",
  "backupScheduleEnabled",
  "backupScheduleHour",
] as const;

// Verifie la regle d'interface liee a is known threshold key.
const isKnownThresholdKey = (
  key: string,
): key is (typeof THRESHOLD_KEYS)[number] =>
  (THRESHOLD_KEYS as readonly string[]).includes(key);

const THRESHOLD_GROUPS = [
  {
    titleKey: "superAdmin.config.thresholdGroups.incidents",
    descriptionKey: "superAdmin.config.thresholdGroupHints.incidents",
    keys: [
      "defaultSlaHours",
      "criticalIncidentHours",
      "slaReminderIntervalHours",
      "maxTransfersBeforeAlert",
      "escalationScanIntervalMinutes",
      "maxReopenCount",
      "reopenTimeLimitHours",
      "autoBlockOverdueWorkingDays",
      "blockedReminderIntervalDays",
      "criticalReminderIntervalHours",
      "prolongedWaitDays",
      "validationDelayHours",
      "validationReminderEnabled",
      "serviceManagerSelfValidationEnabled",
      "pendingActionReminderIntervalHours",
      "pendingActionInternalEnabled",
    ],
  },
  {
    titleKey: "superAdmin.config.thresholdGroups.reports",
    descriptionKey: "superAdmin.config.thresholdGroupHints.reports",
    keys: ["reportRetentionDays"],
  },
  {
    titleKey: "superAdmin.config.thresholdGroups.backup",
    descriptionKey: "superAdmin.config.thresholdGroupHints.backup",
    keys: ["backupScheduleEnabled", "backupScheduleHour"],
  },
  {
    titleKey: "superAdmin.config.thresholdGroups.security",
    descriptionKey: "superAdmin.config.thresholdGroupHints.security",
    keys: [
      "loginMaxFailedAttempts",
      "tempPasswordValidityMinutes",
      "notificationMaxRetryCount",
    ],
  },
] as const;

// Icone d'entete par famille de seuils (aligne sur les cles THRESHOLD_GROUPS).
const THRESHOLD_GROUP_ICONS: Record<string, React.ReactNode> = {
  "superAdmin.config.thresholdGroups.incidents": <WarningOutlined />,
  "superAdmin.config.thresholdGroups.reports": <FileTextOutlined />,
  "superAdmin.config.thresholdGroups.backup": <ClockCircleOutlined />,
  "superAdmin.config.thresholdGroups.security": <SafetyOutlined />,
};

const THRESHOLD_BOUNDS: Record<string, { min: number; max: number }> = {
  defaultSlaHours: { min: 1, max: 720 },
  criticalIncidentHours: { min: 1, max: 168 },
  slaReminderIntervalHours: { min: 1, max: 168 },
  maxTransfersBeforeAlert: { min: 1, max: 20 },
  notificationMaxRetryCount: { min: 1, max: 20 },
  loginMaxFailedAttempts: { min: 1, max: 20 },
  tempPasswordValidityMinutes: { min: 1, max: 1440 },
  escalationScanIntervalMinutes: { min: 1, max: 1440 },
  maxReopenCount: { min: 0, max: 10 },
  reopenTimeLimitHours: { min: 1, max: 720 },
  autoBlockOverdueWorkingDays: { min: 1, max: 60 },
  blockedReminderIntervalDays: { min: 1, max: 90 },
  criticalReminderIntervalHours: { min: 1, max: 168 },
  prolongedWaitDays: { min: 1, max: 365 },
  validationDelayHours: { min: 1, max: 720 },
  validationReminderEnabled: { min: 0, max: 1 },
  serviceManagerSelfValidationEnabled: { min: 0, max: 1 },
  pendingActionReminderIntervalHours: { min: 1, max: 720 },
  pendingActionInternalEnabled: { min: 0, max: 1 },
  reportRetentionDays: { min: 1, max: 3650 },
  backupScheduleEnabled: { min: 0, max: 1 },
  backupScheduleHour: { min: 0, max: 23 },
};

// Rend le composant SuperAdminSystemConfig pour l'interface super-administration systeme configuration.
export const SuperAdminSystemConfig = ({
  section,
  loading,
}: SystemConfigProps) => {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const queryClient = useQueryClient();
  const invalidate = () =>
    queryClient.invalidateQueries({
      queryKey: QUERY_KEYS.SUPER_ADMIN.SYSTEM_CONFIG,
    });
  const systemConfig = asRecord(section);
  const thresholds = asRecord(systemConfig.businessThresholds);
  const [form] = Form.useForm();

  useEffect(() => {
    form.setFieldsValue(thresholds);
  }, [form, JSON.stringify(thresholds)]); // eslint-disable-line react-hooks/exhaustive-deps

  const thresholdsMutation = useMutation({
    mutationFn: (values: Record<string, number>) =>
      superAdminApi.updateThresholds(values),
    onSuccess: () => {
      message.success(t("superAdmin.config.thresholdsSuccess"));
      invalidate();
      // invalide le cache pour recalculer le seuil de réouverture
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.INCIDENTS.ALL });
    },
    onError: () => message.error(t("superAdmin.config.thresholdsError")),
  });

  const backupMutation = useMutation({
    mutationFn: () => superAdminApi.triggerBackup(),
    onSuccess: (data: any) => {
      modal.success({
        title: t("superAdmin.config.backupSuccessTitle"),
        content: (
          <div>
            <p>{data.message}</p>
            <p><strong>PostgreSQL :</strong> {data.postgresStatus}</p>
            <p><strong>MongoDB :</strong> {data.mongoStatus}</p>
            <p><strong>Backblaze B2 :</strong> {data.b2SyncStatus}</p>
            <p><strong>{t("superAdmin.config.labelLocalDirectory")} :</strong> {data.backupDirectory}</p>
          </div>
        ),
      });
    },
    onError: (err: Error) => {
      modal.error({
        title: t("superAdmin.config.backupErrorTitle"),
        content: err.message || t("superAdmin.config.backupErrorMsg"),
      });
    },
  });

  const thresholdKnownKeys = new Set<string>(THRESHOLD_KEYS);
  const knownThresholdKeys = Object.keys(thresholds).filter((k) =>
    thresholdKnownKeys.has(k),
  );
  const formatThresholdValue = (key: string, value: unknown) => {
    if (key === "serviceManagerSelfValidationEnabled" && (value === 0 || value === 1)) {
      return t(`superAdmin.config.selfValidationOptions.${value === 1 ? "serviceManager" : "admin"}`);
    }
    return String(value ?? "-");
  };
  const renderThresholdField = (key: string) => {
    const labelKey = `superAdmin.config.thresholdLabels.${key}`;
    const descKey = `superAdmin.config.thresholdDescriptions.${key}`;
    const bounds = THRESHOLD_BOUNDS[key];
    if (key === "serviceManagerSelfValidationEnabled") {
      return (
        <Form.Item
          key={key}
          label={t(labelKey)}
          tooltip={t(descKey)}
          name={key}
          rules={[{ required: true, message: t("superAdmin.config.thresholdRequired") }]}
        >
          <Select options={[
            { value: 0, label: t("superAdmin.config.selfValidationOptions.admin") },
            { value: 1, label: t("superAdmin.config.selfValidationOptions.serviceManager") },
          ]} />
        </Form.Item>
      );
    }
    if (bounds?.min === 0 && bounds?.max === 1) {
      return (
        <Form.Item
          key={key}
          label={t(labelKey)}
          tooltip={t(descKey)}
          name={key}
          valuePropName="checked"
          getValueProps={(value) => ({ checked: value === 1 })}
          normalize={(value) => (value ? 1 : 0)}
        >
          <AppSwitch />
        </Form.Item>
      );
    }
    return (
      <Form.Item
        key={key}
        label={t(labelKey)}
        tooltip={t(descKey)}
        name={key}
        rules={[
          { required: true, message: t("superAdmin.config.thresholdRequired") },
        ]}
      >
        <InputNumber
          min={bounds?.min ?? 0}
          max={bounds?.max}
          precision={0}
          style={{ width: "100%" }}
        />
      </Form.Item>
    );
  };

  return (
    <SectionCard
      title={t("superAdmin.config.systemParameters")}
      loading={loading}
    >
      <Paragraph type="secondary">{t("superAdmin.config.intro")}</Paragraph>
      <Row gutter={[16, 16]}>
        <Col xs={24}>
          <Card
            title={
              <Space>
                <ControlOutlined />
                <span>{t("superAdmin.config.thresholds")}</span>
              </Space>
            }
            variant="outlined"
          >
            <Paragraph type="secondary">
              {t("superAdmin.config.thresholdsHint")}
            </Paragraph>
          <Form
            form={form}
            layout="vertical"
            onFinish={(values) => {
              const knownValues = Object.fromEntries(
                Object.entries(values as Record<string, number>).filter(
                  ([key]) => isKnownThresholdKey(key),
                ),
              );

              const diffs = Object.keys(knownValues)
                .filter((key) => knownValues[key] !== thresholds[key])
                .map((key) => ({
                  key,
                  oldValue: thresholds[key],
                  newValue: knownValues[key],
                }));

              if (diffs.length === 0) {
                message.info(t("superAdmin.config.noChanges"));
                return;
              }

              modal.confirm({
                title: t("superAdmin.config.confirmDiffTitle"),
                content: (
                  <div>
                    <p>{t("superAdmin.config.confirmDiffMessage")}</p>
                    <ul>
                      {diffs.map((d) => (
                        <li key={d.key}>
                          <strong>
                            {t(`superAdmin.config.thresholdLabels.${d.key}`)}:
                          </strong>{" "}
                          {formatThresholdValue(d.key, d.oldValue)} -&gt;{" "}
                          <strong>{formatThresholdValue(d.key, d.newValue)}</strong>
                        </li>
                      ))}
                    </ul>
                  </div>
                ),
                onOk: () => thresholdsMutation.mutate(knownValues),
              });
            }}
          >
            {THRESHOLD_GROUPS.map((group) => {
              const groupKeys = group.keys.filter((key) =>
                knownThresholdKeys.includes(key),
              );
              if (groupKeys.length === 0) return null;
              return (
                <div key={group.titleKey} className={styles.thresholdGroup}>
                  <Space
                    align="center"
                    size="small"
                    className={styles.thresholdGroupHeader}
                  >
                    {THRESHOLD_GROUP_ICONS[group.titleKey]}
                    <Title level={5} style={{ margin: 0 }}>
                      {t(group.titleKey)}
                    </Title>
                  </Space>
                  <Paragraph type="secondary" className={styles.configHelp}>
                    {t(group.descriptionKey)}
                  </Paragraph>
                  <Row gutter={[16, 0]}>
                    {groupKeys.map((key) => (
                      <Col xs={24} sm={12} key={key}>
                        {renderThresholdField(key)}
                      </Col>
                    ))}
                  </Row>
                </div>
              );
            })}
            <Button
              type="primary"
              htmlType="submit"
              loading={thresholdsMutation.isPending}
            >
              {t("superAdmin.config.saveThresholds")}
            </Button>
          </Form>
          </Card>
        </Col>

        <Col xs={24}>
          <SuperAdminEmailNotifications />
        </Col>

        <Col xs={24}>
          <Card
            title={
              <Space>
                <CloudServerOutlined />
                <span>{t("superAdmin.config.backupSectionTitle")}</span>
              </Space>
            }
            variant="outlined"
          >
            <Paragraph type="secondary">
              {t(
                "superAdmin.config.backupSectionDesc"
              )}
            </Paragraph>
            <Space size="middle" wrap>
              <Tag color="blue">{t("superAdmin.config.tagPostgres")}</Tag>
              <Tag color="green">{t("superAdmin.config.tagMongo")}</Tag>
              <Tag color="purple">{t("superAdmin.config.tagRetentionGfs")}</Tag>
              <Tag color="cyan">{t("superAdmin.config.tagBackblazeB2")}</Tag>
            </Space>
            <div style={{ marginTop: 16 }}>
              <Button
                type="primary"
                icon={<CloudUploadOutlined />}
                loading={backupMutation.isPending}
                onClick={() => {
                  modal.confirm({
                    title: t("superAdmin.config.confirmBackupTitle"),
                    content: t(
                      "superAdmin.config.confirmBackupDesc"
                    ),
                    okText: t("common.actions.confirm"),
                    cancelText: t("common.actions.cancel"),
                    onOk: () => backupMutation.mutate(),
                  });
                }}
              >
                {t("superAdmin.config.triggerBackupBtn")}
              </Button>
            </div>
          </Card>
        </Col>
      </Row>
    </SectionCard>
  );
};
