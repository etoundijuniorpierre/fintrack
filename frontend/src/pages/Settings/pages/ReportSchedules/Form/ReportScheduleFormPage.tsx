// Formulaire pour programmer la generation d'un rapport.

import { memo, useEffect, useCallback, useMemo, useState } from "react";
import { Form, Input, Select, Row, Col, TimePicker } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import {
  useCreateReportSchedule,
  useUpdateReportSchedule,
  useReportSchedule,
} from "../../../../../hooks/settings";
import {
  PageHeader,
  FormField,
  Card,
  FormActions,
} from "../../../../../components/ui";
import { PageContainer } from "../../../../../components/Layout";
import { useAuth } from "../../../../../hooks/auth/useAuth";
import { useUsers } from "../../../../../hooks/user/useUsers/useUsers";
import { useResetTouchedOnNextTick } from "../../../../../hooks/ui/useResetTouchedOnNextTick/useResetTouchedOnNextTick";
import { APP_ROUTES } from "../../../../../utils/constants";
import {
  getReportScopeOptions,
  hasAnyPermission,
  PERMISSIONS,
  USER_VIEW_PERMISSIONS,
} from "../../../../../utils/permissions/permissions";
import { buildEmailRecipientOptions } from "../../../../../utils/formatters/formatters";
import type {
  ReportScheduleRequest,
  ReportType,
  ReportFormat,
} from "../../../../../api/settings/types";
import type { ReportContentType } from "../../../../../api/reporting/types";
import { actionIcons } from "../../../../../utils/icons/appIcons";
import {
  DATE_FORMATS,
  parseTime,
  getLocalizedDayName,
} from "../../../../../utils/date/dateUtils";
import type { Dayjs } from "../../../../../utils/date/dateUtils";
import {
  capitalize,
  hyperCaseAllWordsFormItemProps,
} from "../../../../../utils/formatters/formatters";
import styles from "./ReportScheduleFormPage.module.scss";

// Modele la structure report schedule form values manipulee par le frontend.
interface ReportScheduleFormValues {
  name: string;
  type: ReportType;
  contentType: ReportContentType;
  format: ReportFormat;
  recipientEmails?: string[];
  scope: string;
  sendTime?: Dayjs | null;
  weekDay?: number;
}

// Rend le composant ReportScheduleFormPage pour l'interface planification de rapport formulaire page.
const ReportScheduleFormPage = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { hasPermission } = useAuth();
  const [form] = Form.useForm<ReportScheduleFormValues>();
  const [isTouched, setIsTouched] = useState(false);

  const isEditMode = Boolean(id);

  const { mutate: createReportSchedule, isPending: isCreating } =
    useCreateReportSchedule();
  const { mutate: updateReportSchedule, isPending: isUpdating } =
    useUpdateReportSchedule();
  const { data: scheduleData, isLoading: scheduleLoading } = useReportSchedule(
    id || "",
  );

  const isPending = isCreating || isUpdating || scheduleLoading;

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

  const scopeOptions = useMemo(
    () => getReportScopeOptions(hasPermission, t),
    [hasPermission, t],
  );

  // La liste utilisateurs alimente l'autocompletion des destinataires (retrouver
  // un e-mail via le nom). L'endpoint /users/all autorise deja les droits rapport.
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

  const weekDayOptions = useMemo(() => {
    const days = [];
    for (let i = 1; i <= 7; i++) {
      days.push({
        label: capitalize(getLocalizedDayName(i)),
        value: i,
      });
    }
    return days;
  }, []);

  const selectedType = Form.useWatch("type", form);

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    if (isEditMode && scheduleData) {
      form.setFieldsValue({
        name: scheduleData.name,
        type: scheduleData.type,
        contentType: scheduleData.contentType || "OPERATIONAL",
        format: scheduleData.format,
        recipientEmails: scheduleData.recipientEmails,
        scope: scheduleData.scope || scopeOptions[0]?.value,
        sendTime: parseTime(scheduleData.sendTime),
        weekDay: scheduleData.weekDay,
      });
    } else if (!isEditMode) {
      form.resetFields();
      form.setFieldsValue({
        type: "DAILY",
        contentType: "OPERATIONAL",
        format: "PDF",
        scope: scopeOptions[0]?.value,
      });
    }
  }, [scheduleData, form, isEditMode, scopeOptions]);

  useResetTouchedOnNextTick(setIsTouched, [scheduleData, form, isEditMode]);

  // Gere la soumission du formulaire.
  const handleSubmit = useCallback(
    (values: ReportScheduleFormValues) => {
      const onDone = () => navigate(APP_ROUTES.SETTINGS + "#reportSchedules");

      const payload: ReportScheduleRequest = {
        name: values.name,
        type: values.type,
        contentType: values.contentType,
        format: values.format,
        recipientEmails: values.recipientEmails,
        scope: values.scope,
        sendTime: values.sendTime
          ? values.sendTime.format(DATE_FORMATS.TIME)
          : undefined,
        weekDay: values.type === "WEEKLY" ? values.weekDay : undefined,
      };

      if (isEditMode && id) {
        updateReportSchedule({ id, data: payload }, { onSuccess: onDone });
      } else {
        createReportSchedule(payload, { onSuccess: onDone });
      }
    },
    [isEditMode, id, createReportSchedule, updateReportSchedule, navigate],
  );

  // Traite le retour a l'ecran precedent.
  const handleBack = useCallback(() => {
    navigate(-1);
  }, [navigate]);

  return (
    <PageContainer className="animate-entry">
      <PageHeader
        title={
          isEditMode
            ? t("settings.reportSchedules.page.editTitle")
            : t("settings.reportSchedules.page.createTitle")
        }
        subtitle={
          isEditMode
            ? t("settings.reportSchedules.page.editSubtitle")
            : t("settings.reportSchedules.page.createSubtitle")
        }
        onBack={handleBack}
      />

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        disabled={isPending}
        className={styles.form}
        onFieldsChange={() => setIsTouched(true)}
      >
        <Row gutter={[32, 32]}>
          <Col xs={24} lg={16}>
            <Card
              title={t("common.basicInfo")}
              loading={scheduleLoading}
              className={styles.formCard}
            >
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
              loading={scheduleLoading}
              className={styles.sideCard}
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

              <FormField
                name="scope"
                label={t("settings.reportSchedules.form.labels.scope")}
                required
                rules={[
                  {
                    required: true,
                    message: t(
                      "settings.reportSchedules.form.validation.scope_required",
                    ),
                  },
                ]}
              >
                <Select
                  options={scopeOptions}
                  placeholder={t(
                    "settings.reportSchedules.form.placeholders.scope",
                  )}
                />
              </FormField>

              {selectedType === "WEEKLY" && (
                <FormField
                  name="weekDay"
                  label={t("settings.reportSchedules.form.labels.weekDay")}
                  required
                  rules={[
                    {
                      required: true,
                      message: t(
                        "settings.reportSchedules.form.validation.weekDay_required",
                      ),
                    },
                  ]}
                >
                  <Select
                    options={weekDayOptions}
                    placeholder={t(
                      "settings.reportSchedules.form.placeholders.weekDay",
                    )}
                  />
                </FormField>
              )}

              <FormField
                name="sendTime"
                label={t("settings.reportSchedules.form.labels.sendTime")}
                required
                rules={[
                  {
                    required: true,
                    message: t(
                      "settings.reportSchedules.form.validation.sendTime_required",
                    ),
                  },
                ]}
              >
                <TimePicker
                  format={DATE_FORMATS.TIME}
                  className={styles.fullWidth}
                  placeholder={t(
                    "settings.reportSchedules.form.placeholders.sendTime",
                  )}
                />
              </FormField>
            </Card>
          </Col>
        </Row>

        <FormActions
          className={styles.formActionFooter}
          onCancel={handleBack}
          cancelText={t("settings.buttons.cancel")}
          submitText={t("settings.buttons.save")}
          submitIcon={actionIcons.save}
          loading={isPending}
          submitDisabled={!isTouched || isPending}
        />
      </Form>
    </PageContainer>
  );
});

ReportScheduleFormPage.displayName = "ReportScheduleFormPage";

export default ReportScheduleFormPage;
