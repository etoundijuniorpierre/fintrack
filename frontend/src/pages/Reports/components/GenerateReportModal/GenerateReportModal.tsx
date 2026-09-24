// Modale de selection des filtres pour lancer la generation de rapports.

import { Checkbox, DatePicker, Form, Select, Tooltip } from "antd";
import { Modal } from "../../../../components/ui";
import { QuestionCircleOutlined } from "@ant-design/icons";
import { useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import type {
  ReportGenerateRequest,
  ReportType,
  ReportContentType,
} from "../../../../api/reporting/types";
import type { User } from "../../../../api/user/types";
import { formatToApiDate, getToday } from "../../../../utils/date/dateUtils";
import type { Dayjs } from "../../../../utils/date/dateUtils";
import {
  EMAIL_RECIPIENT_TOKEN_SEPARATORS,
  normalizeEmailRecipients,
  buildEmailRecipientOptions,
} from "../../../../utils/formatters/formatters";
import type { ScopeViewOption } from "../../../../utils/permissions/permissions";
import { isValidEmail } from "../../../../utils/validators/validators";
import {
  getMetricsForReportContent,
  getReportMetricsForScope,
} from "./reportMetrics";
import { useAgencies } from "../../../../hooks/agency/useAgencies";
import { useServices } from "../../../../hooks/service/useServices";
import styles from "./GenerateReportModal.module.scss";

const { RangePicker } = DatePicker;

// Definit les proprietes attendues par le composant GenerateReportModal.
interface GenerateReportModalProps {
  open: boolean;
  scopeOptions: ScopeViewOption[];
  canSendEmail: boolean;
  canSelectAgency: boolean;
  canSelectService: boolean;
  users?: User[];
  // Utilisateurs proposes pour l'autocompletion des destinataires e-mail. Separe
  // de `users` (filtre "sujet") : l'autocompletion ne demande que le droit d'envoi,
  // pas USER_VIEW_ALL.
  recipientUsers?: User[];
  isPending?: boolean;
  onClose: () => void;
  onGenerate: (values: ReportGenerateRequest) => void;
}

// Modele la structure generate report form values manipulee par le frontend.
interface GenerateReportFormValues {
  type: ReportType;
  contentType: ReportContentType;
  period: [Dayjs, Dayjs];
  view?: string;
  format: ReportGenerateRequest["format"];
  metrics?: string[];
  subjectUserId?: string;
  agencyId?: string;
  serviceId?: string;
  sendEmail?: boolean;
  recipients?: string[];
}

// Centralise la logique d'interface liee a generate rapport modal.
const GenerateReportModal = ({
  open,
  scopeOptions,
  canSendEmail,
  canSelectAgency,
  canSelectService,
  users = [],
  recipientUsers = [],
  isPending,
  onClose,
  onGenerate,
}: GenerateReportModalProps) => {
  const { t } = useTranslation();
  const [form] = Form.useForm<GenerateReportFormValues>();

  const reportTypeOptions = useMemo(
    () => [
      { label: t("reports.reportType.DAILY"), value: "DAILY" },
      { label: t("reports.reportType.WEEKLY"), value: "WEEKLY" },
      { label: t("reports.reportType.MONTHLY"), value: "MONTHLY" },
      { label: t("reports.reportType.CUSTOM"), value: "CUSTOM" },
    ],
    [t],
  );

  const typeValue = Form.useWatch("type", form);
  const contentTypeValue = Form.useWatch("contentType", form);
  const viewValue = Form.useWatch("view", form);
  const subjectUserId = Form.useWatch("subjectUserId", form);

  const canFilterByUser = Boolean(viewValue === "user" && users.length > 0);
  const canFilterByAgency = Boolean(viewValue === "agency" && canSelectAgency);
  const canFilterByService = Boolean(
    viewValue === "service" && canSelectService,
  );
  const hasUserFilter = Boolean(subjectUserId);

  const { data: agencies = [] } = useAgencies({ enabled: canFilterByAgency });
  const { data: services = [] } = useServices({ enabled: canFilterByService });

  const metricOptions = useMemo(() => {
    const metrics = getReportMetricsForScope();
    return metrics.map((metric) => ({
      label: (
        <Tooltip title={t(`reports.metrics.${metric}_help`)} placement="top">
          <span
            style={{ display: "inline-flex", alignItems: "center", gap: "4px" }}
          >
            {t(`reports.metrics.${metric}`)}
            <QuestionCircleOutlined style={{ color: "#8c8c8c" }} />
          </span>
        </Tooltip>
      ),
      value: metric,
    }));
  }, [t]);

  useEffect(() => {
    const allowedMetrics = getReportMetricsForScope();
    const allowedMetricValues = allowedMetrics as readonly string[];
    const currentMetrics = form.getFieldValue("metrics") ?? [];
    const filteredMetrics = currentMetrics.filter((metric: string) =>
      allowedMetricValues.includes(metric),
    );

    form.setFieldValue(
      "metrics",
      filteredMetrics.length > 0 ? filteredMetrics : allowedMetrics.slice(0, 3),
    );
  }, [form, hasUserFilter, viewValue]);

  useEffect(() => {
    if (!canFilterByUser) {
      form.setFieldsValue({ subjectUserId: undefined });
    }
  }, [canFilterByUser, form]);

  // Construit les options de selection affichables.
  // Rapport par utilisateur : on conserve les inactifs selectionnables pour
  // permettre l'analyse historique d'un agent parti (revue d'offboarding).
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

  const recipientOptions = useMemo(
    () => buildEmailRecipientOptions(recipientUsers),
    [recipientUsers],
  );

  const agencyOptions = useMemo(
    () => agencies.map((agency) => ({ label: agency.name, value: agency.id })),
    [agencies],
  );

  const serviceOptions = useMemo(
    () =>
      services.map((service) => ({ label: service.name, value: service.id })),
    [services],
  );

  // Traite la validation.
  const handleOk = () => {
    form.validateFields().then((values) => {
      let startDate: string | undefined;
      let endDate: string | undefined;

      const today = getToday();
      if (values.type === "DAILY") {
        // Quotidien : la journee en cours.
        startDate = formatToApiDate(today);
        endDate = formatToApiDate(today);
      } else if (values.type === "WEEKLY") {
        // Hebdomadaire : du lundi de la semaine en cours a aujourd'hui.
        const monday = today.subtract((today.day() + 6) % 7, "day");
        startDate = formatToApiDate(monday);
        endDate = formatToApiDate(today);
      } else if (values.type === "MONTHLY") {
        // Mensuel : du 1er du mois en cours a aujourd'hui.
        startDate = formatToApiDate(today.startOf("month"));
        endDate = formatToApiDate(today);
      } else {
        // Personnalise : plage choisie par l'utilisateur.
        startDate = values.period
          ? formatToApiDate(values.period[0])
          : undefined;
        endDate = values.period ? formatToApiDate(values.period[1]) : undefined;
      }

      if (!startDate || !endDate) {
        return;
      }

      const recipients = normalizeEmailRecipients(values.recipients);

      onGenerate({
        type: values.type,
        contentType: values.contentType,
        startDate,
        endDate,
        format: values.format,
        metrics: getMetricsForReportContent(
          values.contentType,
          values.metrics || [],
          values.view,
          hasUserFilter,
        ),
        filters: {
          view: values.view,
          subjectUserId: Array.isArray(values.subjectUserId)
            ? values.subjectUserId[0]
            : values.subjectUserId,
          agencyId: Array.isArray(values.agencyId)
            ? values.agencyId[0]
            : values.agencyId,
          serviceId: Array.isArray(values.serviceId)
            ? values.serviceId[0]
            : values.serviceId,
          subjectUserIds: Array.isArray(values.subjectUserId)
            ? values.subjectUserId
            : values.subjectUserId
            ? [values.subjectUserId]
            : undefined,
          agencyIds: Array.isArray(values.agencyId)
            ? values.agencyId
            : values.agencyId
            ? [values.agencyId]
            : undefined,
          serviceIds: Array.isArray(values.serviceId)
            ? values.serviceId
            : values.serviceId
            ? [values.serviceId]
            : undefined,
        },
        sendEmail:
          canSendEmail && values.sendEmail ? recipients.length > 0 : false,
        recipients: canSendEmail && values.sendEmail ? recipients : [],
      });
    });
  };

  return (
    <Modal
      title={t("reports.modal.generate_title")}
      open={open}
      onClose={onClose}
      onConfirm={handleOk}
      confirmText={t("reports.buttons.generate")}
      cancelText={t("common.cancel")}
      width={720}
      destroyOnHidden
      confirmLoading={isPending}
      cancelDisabled={isPending}
      closable={!isPending}
      maskClosable={!isPending}
    >
      <Form
        form={form}
        layout="vertical"
        disabled={isPending}
        initialValues={{
          type: "DAILY",
          contentType: "OPERATIONAL",
          format: "PDF",
          metrics: ["synthesis", "distributions", "performance"],
          view: scopeOptions[0]?.value,
        }}
      >
        <Form.Item
          name="contentType"
          label={t("reports.form.contentType")}
          rules={[
            {
              required: true,
              message: t("reports.validation.contentTypeRequired"),
            },
          ]}
        >
          <Select
            options={[
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
            ]}
            placeholder={t("reports.form.contentTypePlaceholder")}
          />
        </Form.Item>

        <Form.Item
          name="type"
          label={t("reports.form.type")}
          rules={[
            { required: true, message: t("reports.validation.typeRequired") },
          ]}
        >
          <Select
            options={reportTypeOptions}
            placeholder={t("reports.form.typePlaceholder")}
          />
        </Form.Item>

        {typeValue === "CUSTOM" && (
          <Form.Item
            name="period"
            label={t("reports.form.period")}
            rules={[
              {
                required: true,
                message: t("reports.validation.periodRequired"),
              },
            ]}
          >
            <RangePicker className={styles.rangePicker} />
          </Form.Item>
        )}

        <Form.Item
          name="view"
          label={t("reports.form.scope")}
          rules={[
            { required: true, message: t("reports.validation.scopeRequired") },
          ]}
        >
          <Select
            options={scopeOptions}
            placeholder={t("reports.form.scopePlaceholder")}
          />
        </Form.Item>

        <Form.Item
          name="format"
          label={t("reports.form.format")}
          rules={[
            { required: true, message: t("reports.validation.formatRequired") },
          ]}
        >
          <Select
            options={[
              { label: t("reports.reportFormat.PDF"), value: "PDF" },
              { label: t("reports.reportFormat.EXCEL"), value: "EXCEL" },
            ]}
            placeholder={t("reports.form.formatPlaceholder")}
          />
        </Form.Item>

        {contentTypeValue === "OPERATIONAL" && (
          <Form.Item name="metrics" label={t("reports.form.metrics")}>
            <Checkbox.Group
              className={styles.metricGrid}
              options={metricOptions}
            />
          </Form.Item>
        )}

        {canFilterByUser && (
          <Form.Item name="subjectUserId" label={t("reports.form.subjectUser")}>
            <Select
              mode="multiple"
              maxTagCount="responsive"
              allowClear
              showSearch
              optionFilterProp="label"
              options={userOptions}
              placeholder={t("reports.form.subjectUserPlaceholder")}
            />
          </Form.Item>
        )}

        {canFilterByAgency && (
          <Form.Item name="agencyId" label={t("reports.form.agency")}>
            <Select
              mode="multiple"
              maxTagCount="responsive"
              allowClear
              showSearch
              optionFilterProp="label"
              options={agencyOptions}
              placeholder={t("reports.form.agencyPlaceholder")}
            />
          </Form.Item>
        )}

        {canFilterByService && (
          <Form.Item name="serviceId" label={t("reports.form.service")}>
            <Select
              mode="multiple"
              maxTagCount="responsive"
              allowClear
              showSearch
              optionFilterProp="label"
              options={serviceOptions}
              placeholder={t("reports.form.servicePlaceholder")}
            />
          </Form.Item>
        )}

        {canSendEmail && (
          <>
            <Form.Item name="sendEmail" valuePropName="checked">
              <Checkbox>{t("reports.form.send_email")}</Checkbox>
            </Form.Item>

            <Form.Item
              noStyle
              shouldUpdate={(prev, curr) => prev.sendEmail !== curr.sendEmail}
            >
              {({ getFieldValue }) =>
                getFieldValue("sendEmail") ? (
                  <Form.Item
                    name="recipients"
                    label={t("reports.form.recipients")}
                    rules={[
                      {
                        validator: (_, value?: string[]) => {
                          const recipients = normalizeEmailRecipients(value);
                          if (recipients.length === 0) {
                            return Promise.reject(
                              new Error(
                                t("reports.emailModal.recipientsRequired"),
                              ),
                            );
                          }
                          const invalid = recipients.find(
                            (email) => !isValidEmail(email),
                          );
                          return invalid
                            ? Promise.reject(
                                new Error(
                                  t("reports.emailModal.recipientsInvalid"),
                                ),
                              )
                            : Promise.resolve();
                        },
                      },
                    ]}
                  >
                    <Select
                      mode="tags"
                      showSearch
                      optionFilterProp="label"
                      tokenSeparators={[...EMAIL_RECIPIENT_TOKEN_SEPARATORS]}
                      options={recipientOptions}
                      placeholder={t("reports.form.recipients_placeholder")}
                    />
                  </Form.Item>
                ) : null
              }
            </Form.Item>
          </>
        )}
      </Form>
    </Modal>
  );
};

export default GenerateReportModal;
