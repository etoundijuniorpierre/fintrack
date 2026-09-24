// Page affichant les details de planification d'un rapport.

import { memo, useState, useEffect, useMemo, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Button,
  Space,
  Typography,
  Row,
  Col,
  Divider,
  Tag,
  List,
  Form,
  Input,
  Select,
} from "antd";
import { useTranslation } from "react-i18next";
import {
  useReportSchedule,
  useUpdateReportSchedule,
  useDeleteReportSchedule,
  useToggleReportSchedule,
} from "../../../../../hooks/settings";
import { useAuth } from "../../../../../hooks/auth/useAuth";
import { useUsers } from "../../../../../hooks/user/useUsers/useUsers";
import {
  PERMISSIONS,
  hasAnyPermission,
  USER_VIEW_PERMISSIONS,
} from "../../../../../utils/permissions/permissions";
import { buildEmailRecipientOptions } from "../../../../../utils/formatters/formatters";
import {
  PageHeader,
  Card,
  ConfirmDialog,
  FormField,
  FormActions,
} from "../../../../../components/ui";
import { PageContainer } from "../../../../../components/Layout";
import { APP_ROUTES } from "../../../../../utils/constants";
import {
  capitalize,
  hyperCaseAllWordsFormItemProps,
  formatDate,
  formatDateTime,
} from "../../../../../utils/formatters/formatters";
import { getLocalizedDayName } from "../../../../../utils/date/dateUtils";
import { resetFormState } from "../../../../../utils/form/formReset/formReset";
import type { ReportScheduleRequest } from "../../../../../api/settings/types";
import { actionIcons, detailIcons } from "../../../../../utils/icons/appIcons";
import styles from "./ReportScheduleDetailsPage.module.scss";

const { Title, Text } = Typography;

// Rend le composant ReportScheduleDetailsPage pour l'interface planification de rapport details page.
const ReportScheduleDetailsPage = memo(() => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();

  const { data: schedule, isLoading } = useReportSchedule(id || "");
  const { mutate: deleteSchedule } = useDeleteReportSchedule();
  const { mutate: updateSchedule, isPending: isUpdating } =
    useUpdateReportSchedule();
  const { mutate: toggleReportSchedule, isPending: isToggling } =
    useToggleReportSchedule();

  const [form] = Form.useForm<ReportScheduleRequest>();
  const [isEditing, setIsEditing] = useState(false);
  const [isTouched, setIsTouched] = useState(false);

  const canUpdate = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);
  const canDelete = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);

  // Autocompletion des destinataires : retrouver un e-mail en tapant le nom.
  const canSearchRecipients = hasAnyPermission(hasPermission, [
    ...USER_VIEW_PERMISSIONS,
    PERMISSIONS.REPORT.GENERATE,
    PERMISSIONS.REPORT.SEND_EMAIL,
  ]);
  const { data: users = [] } = useUsers({ enabled: canSearchRecipients });
  const recipientOptions = useMemo(
    () => buildEmailRecipientOptions(users),
    [users],
  );

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    if (isEditing && schedule) {
      form.setFieldsValue({
        name: schedule.name,
        type: schedule.type,
        contentType: schedule.contentType || "OPERATIONAL",
        format: schedule.format,
        recipientEmails: schedule.recipientEmails,
      });
      const timer = setTimeout(() => setIsTouched(false), 0);
      return () => clearTimeout(timer);
    }
  }, [isEditing, schedule, form]);

  const reportTypeOptions = useMemo(
    () => [
      { label: t("settings.reportSchedules.reportType.DAILY"), value: "DAILY" },
      {
        label: t("settings.reportSchedules.reportType.WEEKLY"),
        value: "WEEKLY",
      },
      {
        label: t("settings.reportSchedules.reportType.MONTHLY"),
        value: "MONTHLY",
      },
      {
        label: t("settings.reportSchedules.reportType.CUSTOM"),
        value: "CUSTOM",
      },
    ],
    [t],
  );

  const reportFormatOptions = useMemo(
    () => [
      { label: t("settings.reportSchedules.reportFormat.PDF"), value: "PDF" },
      {
        label: t("settings.reportSchedules.reportFormat.EXCEL"),
        value: "EXCEL",
      },
      { label: t("settings.reportSchedules.reportFormat.JSON"), value: "JSON" },
    ],
    [t],
  );

  const reportContentTypeOptions = useMemo(
    () => [
      {
        label: t("reports.contentType.OPERATIONAL"),
        value: "OPERATIONAL",
      },
      {
        label: t("reports.contentType.INCIDENT_TYPE_ANALYSIS"),
        value: "INCIDENT_TYPE_ANALYSIS",
      },
      {
        label: t("reports.contentType.INCIDENT_STATUS_OVERVIEW"),
        value: "INCIDENT_STATUS_OVERVIEW",
      },
    ],
    [t],
  );

  // Traite le retour a l'ecran precedent.
  const handleBack = useCallback(() => navigate(-1), [navigate]);
  // Traite le demarrage de l'edition.
  const handleEditStart = useCallback(() => {
    if (id) {
      navigate(APP_ROUTES.SETTINGS_REPORT_SCHEDULES_EDIT(id));
    }
  }, [id, navigate]);

  const handleEditCancel = useCallback(() => {
    setIsEditing(false);
    setIsTouched(false);
    resetFormState(
      form,
      schedule
        ? {
            name: schedule.name,
            type: schedule.type,
            contentType: schedule.contentType || "OPERATIONAL",
            format: schedule.format,
            recipientEmails: schedule.recipientEmails,
          }
        : undefined,
    );
  }, [form, schedule]);

  // Traite la validation de l'edition.
  const handleEditSubmit = useCallback(
    (values: ReportScheduleRequest) => {
      if (id && schedule) {
        const payload: ReportScheduleRequest = {
          ...values,
          contentType: values.contentType || "OPERATIONAL",
          scope: schedule.scope,
          sendTime: schedule.sendTime,
          weekDay: schedule.weekDay,
        };
        updateSchedule(
          { id, data: payload },
          {
            onSuccess: () => {
              setIsEditing(false);
              setIsTouched(false);
            },
          },
        );
      }
    },
    [id, schedule, updateSchedule],
  );

  // Gere la suppression d'un element.
  const handleDelete = useCallback(() => {
    if (id) {
      deleteSchedule(id, {
        onSuccess: () => navigate("/settings#reportSchedules"),
      });
    }
  }, [id, deleteSchedule, navigate]);

  if (isLoading)
    return (
      <PageContainer>
        <div />
      </PageContainer>
    );
  if (!schedule)
    return (
      <PageContainer>
        {t("settings.reportSchedules.messages.not_found")}
      </PageContainer>
    );

  return (
    <PageContainer className={styles.pageContainer}>
      <PageHeader
        title={
          isEditing
            ? t("settings.reportSchedules.page.editTitle")
            : schedule.name
        }
        onBack={handleBack}
        subtitle={
          !isEditing
            ? t("settings.reportSchedules.page.detailsSubtitle", {
                type: t(`settings.reportSchedules.reportType.${schedule.type}`),
              })
            : undefined
        }
        actions={
          !isEditing ? (
            <Space size="small">
              {canUpdate && (
                <Button
                  icon={
                    schedule.isActive ? actionIcons.close : actionIcons.validate
                  }
                  onClick={() => toggleReportSchedule(schedule.id)}
                  loading={isToggling}
                >
                  {schedule.isActive
                    ? t("common.deactivate")
                    : t("common.activate")}
                </Button>
              )}
              {canUpdate && (
                <Button
                  type="primary"
                  icon={actionIcons.edit}
                  onClick={handleEditStart}
                  className="btn-primary"
                >
                  {t("settings.buttons.edit")}
                </Button>
              )}
              {canDelete && (
                <ConfirmDialog
                  title={t("settings.reportSchedules.messages.delete_confirm")}
                  description={t(
                    "settings.reportSchedules.messages.delete_ask",
                  )}
                  onConfirm={handleDelete}
                  danger
                >
                  <Button
                    danger
                    type="primary"
                    icon={actionIcons.delete}
                    className="btn-danger"
                  >
                    {t("settings.buttons.delete")}
                  </Button>
                </ConfirmDialog>
              )}
            </Space>
          ) : (
            <Space size="small">
              <Button icon={actionIcons.close} onClick={handleEditCancel}>
                {t("settings.buttons.cancel")}
              </Button>
            </Space>
          )
        }
      />

      {isEditing ? (
        <Form
          form={form}
          layout="vertical"
          onFinish={handleEditSubmit}
          disabled={isUpdating}
          className={styles.editForm}
          onFieldsChange={() => setIsTouched(true)}
        >
          <Row gutter={[32, 32]}>
            <Col xs={24} lg={16}>
              <Card title={t("common.basicInfo")} className={styles.editCard}>
                <FormField
                  name="name"
                  label={t("settings.reportSchedules.form.labels.name")}
                  required
                  rules={[
                    {
                      required: true,
                      message: t(
                        "settings.reportSchedules.form.validation.name_required",
                      ),
                    },
                  ]}
                  {...hyperCaseAllWordsFormItemProps}
                >
                  <Input
                    placeholder={t(
                      "settings.reportSchedules.form.placeholders.name",
                    )}
                    maxLength={100}
                  />
                </FormField>

                <FormField
                  name="recipientEmails"
                  label={t(
                    "settings.reportSchedules.form.labels.recipientEmails",
                  )}
                  rules={[
                    {
                      required: true,
                      message: t(
                        "settings.reportSchedules.form.validation.recipientEmails_required",
                      ),
                    },
                  ]}
                >
                  <Select
                    mode="tags"
                    showSearch
                    optionFilterProp="label"
                    options={recipientOptions}
                    placeholder={t(
                      "settings.reportSchedules.form.placeholders.recipientEmails",
                    )}
                    tokenSeparators={[",", " "]}
                  />
                </FormField>
              </Card>
            </Col>

            <Col xs={24} lg={8}>
              <Card
                title={t("common.configuration")}
                className={styles.editSideCard}
              >
                <FormField
                  name="type"
                  label={t("settings.reportSchedules.form.labels.type")}
                  required
                  rules={[
                    {
                      required: true,
                      message: t(
                        "settings.reportSchedules.form.validation.type_required",
                      ),
                    },
                  ]}
                >
                  <Select
                    options={reportTypeOptions}
                    placeholder={t(
                      "settings.reportSchedules.form.placeholders.type",
                    )}
                  />
                </FormField>

                <FormField
                  name="format"
                  label={t("settings.reportSchedules.form.labels.format")}
                  required
                  rules={[
                    {
                      required: true,
                      message: t(
                        "settings.reportSchedules.form.validation.format_required",
                      ),
                    },
                  ]}
                >
                  <Select
                    options={reportFormatOptions}
                    placeholder={t(
                      "settings.reportSchedules.form.placeholders.format",
                    )}
                  />
                </FormField>

                <FormField
                  name="contentType"
                  label={t("reports.form.contentType")}
                  required
                  rules={[
                    {
                      required: true,
                      message: t("reports.validation.contentTypeRequired"),
                    },
                  ]}
                >
                  <Select
                    options={reportContentTypeOptions}
                    placeholder={t("reports.form.contentTypePlaceholder")}
                  />
                </FormField>
              </Card>
            </Col>
          </Row>

          <FormActions
            className={styles.formActionFooter}
            onCancel={handleEditCancel}
            cancelText={t("settings.buttons.cancel")}
            submitText={t("settings.buttons.save")}
            submitIcon={actionIcons.save}
            loading={isUpdating}
            submitDisabled={!isTouched || isUpdating}
          />
        </Form>
      ) : (
        <div className={styles.content}>
          <Row gutter={[32, 32]}>
            <Col xs={24} lg={16}>
              <div className={styles.mainColumn}>
                <Card className={styles.mainCard}>
                  <div className={styles.scheduleHeader}>
                    <div className={styles.scheduleIcon}>
                      {detailIcons.schedule}
                    </div>
                    <div className={styles.scheduleTitleInfo}>
                      <div className={styles.titleRow}>
                        <Title level={2} className={styles.scheduleTitle}>
                          {schedule.name}
                        </Title>
                        <Tag
                          color={schedule.isActive ? "success" : "error"}
                          className={styles.statusTag}
                        >
                          {schedule.isActive
                            ? t("common.active")
                            : t("common.inactive")}
                        </Tag>
                      </div>
                      <Text className={styles.scheduleMeta}>
                        {t(
                          `settings.reportSchedules.reportType.${schedule.type}`,
                        )}{" "}
                        / {schedule.format}
                      </Text>
                    </div>
                  </div>
                </Card>

                <Card
                  className={styles.recipientsCard}
                  title={t("settings.reportSchedules.sections.recipients")}
                >
                  <List
                    itemLayout="horizontal"
                    dataSource={schedule.recipientEmails || []}
                    locale={{
                      emptyText: t(
                        "settings.reportSchedules.sections.noRecipients",
                      ),
                    }}
                    renderItem={(email) => (
                      <List.Item>
                        <List.Item.Meta
                          avatar={
                            <span className={styles.mailIcon}>
                              {detailIcons.mail}
                            </span>
                          }
                          title={email}
                        />
                      </List.Item>
                    )}
                  />
                </Card>

                <Card
                  className={styles.metadataCard}
                  title={t("settings.reportSchedules.details.metadata")}
                >
                  <div className={styles.metadataGrid}>
                    <div className={styles.metadataItem}>
                      <Text type="secondary">
                        {t("settings.reportSchedules.form.labels.sendTime")}
                      </Text>
                      <Text strong>{schedule.sendTime?.slice(0, 5) || "-"}</Text>
                    </div>
                    <div className={styles.metadataItem}>
                      <Text type="secondary">
                        {t("settings.reportSchedules.form.labels.weekDay")}
                      </Text>
                      <Text strong>
                        {schedule.weekDay === undefined
                          ? "-"
                          : capitalize(getLocalizedDayName(schedule.weekDay))}
                      </Text>
                    </div>
                    <div className={styles.metadataItem}>
                      <Text type="secondary">
                        {t("settings.reportSchedules.details.createdBy")}
                      </Text>
                      <Text strong>
                        {schedule.createdBy
                          ? `${schedule.createdBy.firstName} ${schedule.createdBy.lastName}`
                          : "-"}
                      </Text>
                    </div>
                    <div className={styles.metadataItem}>
                      <Text type="secondary">
                        {t("settings.reportSchedules.details.identifier")}
                      </Text>
                      <Text strong>{schedule.id}</Text>
                    </div>
                    <div className={styles.metadataItem}>
                      <Text type="secondary">
                        {t("settings.reportSchedules.details.createdAt")}
                      </Text>
                      <Text strong>{formatDateTime(schedule.createdAt)}</Text>
                    </div>
                    <div className={styles.metadataItem}>
                      <Text type="secondary">
                        {t("settings.reportSchedules.details.updatedAt")}
                      </Text>
                      <Text strong>{formatDateTime(schedule.updatedAt)}</Text>
                    </div>
                  </div>
                </Card>
              </div>
            </Col>
            <Col xs={24} lg={8}>
              <Card className={styles.statsCard} title={t("common.overview")}>
                <div className={styles.statItem}>
                  <div className={styles.statIcon}>{detailIcons.calendar}</div>
                  <div className={styles.statContent}>
                    <Text type="secondary">
                      {t("settings.reportSchedules.table.type")}
                    </Text>
                    <Title level={4}>
                      <Tag color="blue">
                        {t(
                          `settings.reportSchedules.reportType.${schedule.type}`,
                        )}
                      </Tag>
                    </Title>
                  </div>
                </div>
                <Divider className={styles.statDivider} />
                <div className={styles.statItem}>
                  <div className={styles.statIcon}>{actionIcons.report}</div>
                  <div className={styles.statContent}>
                    <Text type="secondary">{t("reports.table.contentType")}</Text>
                    <Title level={4}>
                      <Tag color="cyan">
                        {t(
                          `reports.contentType.${schedule.contentType || "OPERATIONAL"}`,
                        )}
                      </Tag>
                    </Title>
                  </div>
                </div>
                <Divider className={styles.statDivider} />
                <div className={styles.statItem}>
                  <div className={styles.statIcon}>{actionIcons.report}</div>
                  <div className={styles.statContent}>
                    <Text type="secondary">
                      {t("settings.reportSchedules.table.format")}
                    </Text>
                    <Title level={4}>
                      <Tag color="orange">{schedule.format}</Tag>
                    </Title>
                  </div>
                </div>
                <Divider className={styles.statDivider} />
                <div className={styles.statItem}>
                  <div className={styles.statIcon}>
                    {detailIcons.environment}
                  </div>
                  <div className={styles.statContent}>
                    <Text type="secondary">
                      {t("settings.reportSchedules.table.scope")}
                    </Text>
                    <Title level={4}>
                      <Tag color="purple">
                        {schedule.scope
                          ? t(`reports.scope.${schedule.scope.toLowerCase()}`)
                          : "-"}
                      </Tag>
                    </Title>
                  </div>
                </div>
                <Divider className={styles.statDivider} />
                <div className={styles.statItem}>
                  <div className={styles.statIcon}>{detailIcons.schedule}</div>
                  <div className={styles.statContent}>
                    <Text type="secondary">
                      {t("settings.reportSchedules.table.lastGeneratedAt")}
                    </Text>
                    <Title level={4}>
                      {schedule.lastGeneratedAt
                        ? formatDate(schedule.lastGeneratedAt)
                        : "-"}
                    </Title>
                  </div>
                </div>
              </Card>
            </Col>
          </Row>
        </div>
      )}
    </PageContainer>
  );
});

ReportScheduleDetailsPage.displayName = "ReportScheduleDetailsPage";

export default ReportScheduleDetailsPage;
