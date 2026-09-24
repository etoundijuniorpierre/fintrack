// Formulaire de saisie pour la creation et l'edition d'un incident.
import {
  Form,
  Input,
  Select,
  DatePicker,
  Divider,
  Row,
  Col,
  Button,
  Space,
  Upload,
  List,
  Typography,
  Tooltip,
  Checkbox,
  Alert,
  App,
  type FormInstance,
} from "antd";
import { useTranslation } from "react-i18next";
import { memo, useMemo, useCallback, useState, useEffect, useRef } from "react";
import dayjs from "dayjs";
import { useQueryClient } from "@tanstack/react-query";
import { FormField, FormActions } from "../../../../components/ui";
import {
  useCreateIncident,
  useUpdateIncident,
  useIncidentTypes,
  useCriticalities,
  useIncidentCauses,
} from "../../../../hooks/incident/useIncidents/useIncidents";
import { useServices } from "../../../../hooks/service/useServices";
import { useAgencies } from "../../../../hooks/agency/useAgencies";
import { useAuth } from "../../../../hooks/auth/useAuth";
import {
  PERMISSIONS,
  hasAnyPermission,
} from "../../../../utils/permissions/permissions";
import { documentApi } from "../../../../api/document/documentApi";
import type {
  IncidentRequest,
  IncidentResponse,
  IncidentTypeConfigResponse,
  EnumResponse,
} from "../../../../api/incident/types";
import { IncidentCause } from "../../../../api/incident/enums/enums";
import {
  DATE_FORMATS,
  formatToApiDate,
  toDayjs,
} from "../../../../utils/date/dateUtils";
import { QUERY_KEYS } from "../../../../utils/constants";
import { actionIcons, incidentIcons } from "../../../../utils/icons/appIcons";
import AttachmentsSection from "../Attachments/section/AttachmentsSection";
import CommentsSection from "../Comments/sections/CommentsSection";
import { sentenceCaseFormItemProps } from "../../../../utils/formatters/formatters";
import {
  ACCEPTED_ATTACHMENT_ACCEPT,
  MAX_ATTACHMENT_SIZE_BYTES,
  isAcceptedAttachment,
  isAttachmentSizeValid,
} from "../../../../utils/attachments/attachmentValidation";
import { getApiErrorMessage } from "../../../../utils/apiMessages/apiMessages";
import styles from "./IncidentForm.module.scss";

// Centralise la logique d'interface liee a incident form values.
export type IncidentFormValues = Omit<
  IncidentRequest,
  "dueDate" | "incidentDate" | "observationDate"
> & {
  dueDate?: dayjs.Dayjs;
  incidentDate?: dayjs.Dayjs;
  observationDate?: dayjs.Dayjs;
};

const { TextArea } = Input;
const { Text } = Typography;

// Cle du toast d'envoi des PJ : permet de remplacer le message "en cours" par le
// resultat (succes/erreur) plutot que d'empiler deux notifications.
const UPLOAD_MESSAGE_KEY = "incident-create-attachments-upload";

// Le service cible en edition est purement informatif : hors du store de formulaire,
// il lui faut un id explicite pour rester associe a son libelle.
const TARGET_SERVICE_DISPLAY_ID = "incident-target-service-display";

// Definit les proprietes attendues par le composant IncidentForm.
export interface IncidentFormProps {
  form?: FormInstance<IncidentFormValues>;
  /** Valeurs initiales — accepte dayjs pour dueDate (usage edit mode) */
  initialValues?: Partial<IncidentFormValues>;
  onSuccess?: (incident: IncidentResponse) => void;
  onCancel?: () => void;
  submitText?: string;
  showButtons?: boolean;
  disabled?: boolean;
  incidentId?: string;
  isStrictMode?: boolean;
  /** Statut et créateur de l'incident (mode édition) : pilotent les droits sur les pièces jointes. */
  incidentStatus?: string;
  creatorId?: string | null;
  /** Service actuellement destinataire (mode edition) : affiche tant que le type n'en impose pas d'autre. */
  currentServiceName?: string;
}

// Rend le composant SectionDivider pour l'interface incident formulaire.
const SectionDivider = memo(
  ({ icon, title }: { icon: React.ReactNode; title: string }) => (
    <Divider titlePlacement="left" className={styles.sectionDivider}>
      <Space size={8}>
        {icon}
        {title}
      </Space>
    </Divider>
  ),
);
SectionDivider.displayName = "SectionDivider";

// Rend le composant IncidentForm.
const IncidentForm = memo(
  ({
    form: propForm,
    initialValues,
    onSuccess,
    onCancel,
    submitText,
    showButtons = true,
    disabled = false,
    incidentId,
    isStrictMode = false,
    currentServiceName,
    incidentStatus,
    creatorId,
  }: IncidentFormProps) => {
    const [internalForm] = Form.useForm<IncidentFormValues>();
    const form = propForm || internalForm;
    const { t } = useTranslation();
    const { message } = App.useApp();
    const queryClient = useQueryClient();
    const { user, hasPermission } = useAuth();

    const { mutate: createIncident, isPending: isCreating } =
      useCreateIncident();
    const { mutate: updateIncident, isPending: isUpdating } =
      useUpdateIncident();

    const { data: incidentTypesData, isLoading: typesLoading } =
      useIncidentTypes();
    const { data: criticalitiesData, isLoading: criticalitiesLoading } =
      useCriticalities();
    const { data: causesData = [], isLoading: causesLoading } =
      useIncidentCauses();
    const { data: services = [], isLoading: servicesLoading } = useServices();
    // Le selecteur d'agence n'est utile que pour les SUPER_ADMIN / ADMIN
    // sans agence propre ; on conditionne la requete pour eviter un appel
    // inutile (et potentiellement non autorise) cote autres roles.
    const canPickAgency =
      hasPermission(PERMISSIONS.INCIDENT.VIEW_ALL) &&
      !user?.agencyId &&
      !incidentId;
    const { data: agencies = [], isLoading: agenciesLoading } = useAgencies({
      enabled: canPickAgency,
    });

    const [isUploadingFiles, setIsUploadingFiles] = useState(false);
    const isPending = isCreating || isUpdating || isUploadingFiles;
    const [isTouched, setIsTouched] = useState(false);
    const [pendingFiles, setPendingFiles] = useState<File[]>([]);
    const pendingFilesRef = useRef<File[]>([]);

    const isEditMode = Boolean(incidentId);
    const isSubmitDisabled = (isEditMode && !isTouched) || isPending;
    const isFormDisabled = disabled;
    // En mode strict, on verrouille les champs de qualification structurelle.
    const isStructuralDisabled = disabled || isStrictMode;

    const showSelfAssign =
      !isEditMode &&
      hasAnyPermission(hasPermission, [
        PERMISSIONS.INCIDENT.TREAT,
        PERMISSIONS.INCIDENT.RESOLVE,
      ]);

    // Profil global (SUPER_ADMIN / ADMIN sans agence propre) : peut choisir
    // l'agence cible de l'incident. Les autres roles heritent automatiquement
    // de leur propre agence cote backend.
    const showAgencyPicker = canPickAgency;
    const effectiveInitialValues = useMemo(
      () =>
        isEditMode
          ? initialValues
          : {
              incidentDate: dayjs().startOf("day"),
              observationDate: dayjs().startOf("day"),
              ...initialValues,
            },
      [initialValues, isEditMode],
    );

    const agencyOptions = useMemo(
      () => agencies.map((a) => ({ label: a.name, value: a.id })),
      [agencies],
    );

    // Synchro des valeurs initiales quand on entre en mode edit
    useEffect(() => {
      if (incidentId && initialValues) {
        const formattedValues = {
          ...initialValues,
          dueDate: toDayjs(initialValues.dueDate),
          incidentDate: toDayjs(initialValues.incidentDate),
          observationDate: toDayjs(initialValues.observationDate),
        };
        form.setFieldsValue(formattedValues);
        const timer = setTimeout(() => setIsTouched(false), 0);
        return () => clearTimeout(timer);
      }
    }, [incidentId, form, initialValues]);

    // Watch the cause field to conditionally render causeDetail
    const selectedCause = Form.useWatch("cause", form) as
      | IncidentCause
      | undefined;

    const fieldIcons = useMemo(
      () => ({
        file: (
          <span className={styles.inputPrefix}>
            {incidentIcons.description}
          </span>
        ),
        tags: incidentIcons.classification,
        calendar: incidentIcons.dueDate,
        bulb: incidentIcons.cause,
        clip: incidentIcons.attachment,
      }),
      [],
    );

    const allTypes = useMemo<IncidentTypeConfigResponse[]>(() => {
      const wrapper = incidentTypesData as
        | {
          content?: IncidentTypeConfigResponse[];
          data?: IncidentTypeConfigResponse[];
        }
        | undefined;
      return Array.isArray(incidentTypesData)
        ? incidentTypesData
        : wrapper?.content || wrapper?.data || [];
    }, [incidentTypesData]);

    const typeOptions = useMemo(
      () =>
        allTypes
          .filter((type) => type.isActive)
          .map((type) => ({ label: type.displayName, value: type.id })),
      [allTypes],
    );

    // Type selectionne determine le service/responsable impose (points 09/11/13)
    const selectedTypeId = Form.useWatch("typeId", form) as string | undefined;
    const selectedType = useMemo(
      () => allTypes.find((type) => type.id === selectedTypeId),
      [allTypes, selectedTypeId],
    );
    const defaultService = selectedType?.defaultTargetService;
    const defaultUser = selectedType?.defaultTargetUser;
    const typeHasConstraint = Boolean(defaultService || defaultUser);

    const serviceOptions = useMemo(
      () => services.map((s) => ({ label: s.name, value: s.id })),
      [services],
    );

    // Auto-attribution : prime sur toute autre configuration de routage.
    const assignToSelf = Form.useWatch("assignToSelf", form) as
      | boolean
      | undefined;

    // Le service cible libre est neutralise soit par une contrainte de type,
    // soit par l'auto-attribution (qui annule tout routage vers un service).
    const targetServiceDisabled = typeHasConstraint || Boolean(assignToSelf);

    // Des que le service cible n'est plus modifiable, on vide sa valeur.
    useEffect(() => {
      if (targetServiceDisabled) {
        form.setFieldValue("targetServiceId", undefined);
      }
    }, [targetServiceDisabled, form]);

    // Pre-remplit la criticite a partir de la valeur par defaut du type, mais
    // seulement en mode creation et si l'utilisateur n'a pas encore selectionne.
    useEffect(() => {
      if (isEditMode) return;
      if (!selectedType?.defaultCriticality) return;
      const currentCriticality = form.getFieldValue("criticality");
      if (currentCriticality) return;
      form.setFieldValue("criticality", selectedType.defaultCriticality);
    }, [selectedType, isEditMode, form]);

    // Renseigne l'echeance (champ desactive) a partir du SLA du type
    useEffect(() => {
      if (isEditMode && selectedType?.id === initialValues?.typeId) return;
      const slaHours = selectedType?.slaHours;
      if (slaHours == null) return;
      form.setFieldValue("dueDate", dayjs().add(slaHours, "hour"));
    }, [selectedType, isEditMode, initialValues?.typeId, form]);

    const criticalityOptions = useMemo(() => {
      const wrapper = criticalitiesData as
        | { content?: EnumResponse[]; data?: EnumResponse[] }
        | undefined;
      const data: EnumResponse[] = Array.isArray(criticalitiesData)
        ? criticalitiesData
        : wrapper?.content || wrapper?.data || [];

      // Libelle via i18n frontend (source unique), avec repli sur le nom traduit par le backend.
      return data.map((item: EnumResponse) => ({
        label: t(`incidents.criticality.${item.code}`, item.name),
        value: item.code,
      }));
    }, [criticalitiesData, t]);

    const causeOptions = useMemo(
      () =>
        causesData.map((item: EnumResponse) => ({
          label: t(`incidents.cause_values.${item.code}`, item.name),
          value: item.code,
        })),
      [causesData, t],
    );

    // L'evenement et sa constatation ne peuvent pas etre dans le futur.
    const disabledFutureDate = useCallback(
      (current: dayjs.Dayjs) =>
        Boolean(current && current.isAfter(dayjs(), "day")),
      [],
    );

    // Derive causeDetail field config based on selected cause
    const causeDetailConfig = useMemo(() => {
      if (!selectedCause) return null;
      if (selectedCause === IncidentCause.HUMAN) {
        return {
          type: "textarea" as const,
          label: t("incidents.form.labels.cause_detail_human"),
          placeholder: t("incidents.form.placeholders.cause_detail_human"),
        };
      }
      if (selectedCause === IncidentCause.EXTERNAL) {
        return {
          type: "text" as const,
          label: t("incidents.form.labels.cause_detail_external"),
          placeholder: t("incidents.form.placeholders.cause_detail_external"),
        };
      }
      return {
        type: "textarea" as const,
        label: t("incidents.form.labels.cause_detail_other"),
        placeholder: t("incidents.form.placeholders.cause_detail_other"),
      };
    }, [selectedCause, t]);

    // Gere les fichiers en attente pendant la creation uniquement.
    const handleAddPendingFile = useCallback(
      (file: File) => {
        if (!isAcceptedAttachment(file)) {
          message.error(
            t("incidents.attachments.invalid_type"),
          );
          return Upload.LIST_IGNORE;
        }
        if (!isAttachmentSizeValid(file)) {
          message.error(
            t(file.size === 0 ? "incidents.attachments.empty_file" : "incidents.attachments.too_large", {
              name: file.name,
              limit: Math.round(MAX_ATTACHMENT_SIZE_BYTES / (1024 * 1024)),
            }),
          );
          return Upload.LIST_IGNORE;
        }
        const nextFiles = [...pendingFilesRef.current, file];
        pendingFilesRef.current = nextFiles;
        setPendingFiles(nextFiles);
        return false; // Bloque l'upload automatique Ant Design avant creation.
      },
      [message, t],
    );

    // Une piece jointe ajoutee ou retiree en mode edition est deja enregistree par
    // AttachmentsSection ; le bouton de validation restait pourtant grise, laissant
    // croire que la modification n'avait pas ete prise en compte.
    const handleAttachmentsChange = useCallback(() => setIsTouched(true), []);

    // Traite le retrait d'un fichier en attente.
    const handleRemovePendingFile = useCallback((index: number) => {
      const nextFiles = pendingFilesRef.current.filter((_, i) => i !== index);
      pendingFilesRef.current = nextFiles;
      setPendingFiles(nextFiles);
    }, []);

    const onFinish = useCallback(
      (values: IncidentFormValues) => {
        const causeDetail = values.causeDetail;
        const {
          dueDate: _dueDate,
          ...requestValues
        } = values;
        void _dueDate;

        const data: IncidentRequest = {
          ...requestValues,
          incidentDate: formatToApiDate(values.incidentDate),
          observationDate: formatToApiDate(values.observationDate),
          causeDetail,
        };

        if (incidentId) {
          // Champs reserves a la creation non pris en charge par la mise a jour.
          const { targetServiceId: _t, assignToSelf: _a, ...updateData } = data;
          void _t;
          void _a;
          updateIncident(
            { id: incidentId, data: updateData },
            {
              onSuccess: (response: IncidentResponse) => {
                onSuccess?.(response);
              },
            },
          );
        } else {
          const filesToUpload = [...pendingFilesRef.current];
          createIncident(data, {
            onSuccess: async (response: IncidentResponse) => {
              form.resetFields();
              if (filesToUpload.length > 0) {
                setIsUploadingFiles(true);
                // Toast persistant : rassure l'utilisateur que le temps d'attente
                // vient de l'envoi de SES fichiers (remplace par succes/erreur a la fin).
                message.open({
                  type: "loading",
                  key: UPLOAD_MESSAGE_KEY,
                  content: t("incidents.attachments.messages.uploading"),
                  duration: 0,
                });
                try {
                  const uploadResults = await Promise.allSettled(
                    filesToUpload.map((file) =>
                      documentApi.upload(file, response.id),
                    ),
                  );
                  const failedUploads = uploadResults.filter(
                    (result) => result.status === "rejected",
                  ).length;
                  if (failedUploads > 0) {
                    const firstFailure = uploadResults.find(
                      (result) => result.status === "rejected",
                    );
                    const apiMessage =
                      firstFailure?.status === "rejected"
                        ? getApiErrorMessage(firstFailure.reason)
                        : null;
                    message.error({
                      key: UPLOAD_MESSAGE_KEY,
                      content:
                        apiMessage ||
                        t(
                          "incidents.attachments.messages.upload_partial_error",
                          { count: failedUploads },
                        ),
                    });
                  } else {
                    message.success({
                      key: UPLOAD_MESSAGE_KEY,
                      content: t(
                        "incidents.attachments.messages.upload_success",
                      ),
                    });
                  }
                  setPendingFiles([]);
                  queryClient.invalidateQueries({
                    queryKey: QUERY_KEYS.DOCUMENTS.BY_INCIDENT(response.id),
                  });
                } finally {
                  setIsUploadingFiles(false);
                }
              }
              onSuccess?.(response);
            },
          });
        }
      },
      [
        incidentId,
        createIncident,
        updateIncident,
        onSuccess,
        form,
        queryClient,
        message,
        t,
      ],
    );

    const filterOption = useCallback(
      (input: string, option?: { label?: unknown }) =>
        (option?.label as string)?.toLowerCase().includes(input.toLowerCase()),
      [],
    );

    const rules = useMemo(
      () => ({
        title: [
          {
            required: true,
            message: t("incidents.form.validation.title_required"),
          },
          { min: 3, message: t("incidents.form.validation.title_min") },
          { max: 200, message: t("incidents.form.validation.title_max") },
        ],
        description: [
          {
            required: true,
            message: t("incidents.form.validation.description_required"),
          },
          {
            max: 5000,
            message: t("incidents.form.validation.description_max"),
          },
        ],
        typeId: [
          {
            required: true,
            message: t("incidents.form.validation.type_required"),
          },
        ],
        criticality: [
          {
            required: true,
            message: t("incidents.form.validation.criticality_required"),
          },
        ],
        incidentDate: [
          {
            validator: (_: unknown, value: dayjs.Dayjs | undefined) => {
              if (value?.isAfter(dayjs(), "day")) {
                return Promise.reject(
                  new Error(t("incidents.form.validation.date_not_future")),
                );
              }
              const observationDate = form.getFieldValue("observationDate");
              if (value && observationDate && value.isAfter(observationDate, "day")) {
                return Promise.reject(
                  new Error(t("incidents.form.validation.date_order")),
                );
              }
              return Promise.resolve();
            },
          },
        ],
        observationDate: [
          {
            validator: (_: unknown, value: dayjs.Dayjs | undefined) => {
              if (value?.isAfter(dayjs(), "day")) {
                return Promise.reject(
                  new Error(t("incidents.form.validation.date_not_future")),
                );
              }
              const incidentDate = form.getFieldValue("incidentDate");
              if (value && incidentDate && incidentDate.isAfter(value, "day")) {
                return Promise.reject(
                  new Error(t("incidents.form.validation.date_order")),
                );
              }
              return Promise.resolve();
            },
          },
        ],
        causeDetail: [
          {
            max: 2000,
            message: t("incidents.form.validation.cause_detail_max"),
          },
        ],
      }),
      [form, t],
    );

    return (
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={effectiveInitialValues}
        disabled={disabled}
        className={styles.incidentForm}
        onFieldsChange={() => setIsTouched(true)}
      >
        {/* ── 1. Informations générales ── */}
        <SectionDivider
          icon={incidentIcons.description}
          title={t("incidents.form.sections.information")}
        />

        <Row gutter={24}>
          <Col span={24}>
            <FormField
              name="title"
              label={t("incidents.form.labels.title")}
              rules={rules.title}
              {...sentenceCaseFormItemProps}
            >
              <Input
                prefix={fieldIcons.file}
                placeholder={t("incidents.form.placeholders.title")}
                maxLength={200}
                showCount
                disabled={isFormDisabled}
              />
            </FormField>
          </Col>
        </Row>

        <Row gutter={24}>
          <Col span={24}>
            <FormField
              name="description"
              label={t("incidents.form.labels.description")}
              rules={rules.description}
              {...sentenceCaseFormItemProps}
            >
              <TextArea
                rows={4}
                placeholder={t("incidents.form.placeholders.description")}
                maxLength={5000}
                showCount
                disabled={isFormDisabled}
              spellCheck={true} />
            </FormField>
          </Col>
        </Row>

        {/* Sélecteur d'agence — réservé aux profils globaux (SUPER_ADMIN / ADMIN
            sans agence propre). Pour les autres, l'agence est imposée côté
            backend en fonction de l'utilisateur courant. */}
        {showAgencyPicker && (
          <Row gutter={24}>
            <Col xs={24} sm={12}>
              <FormField
                name="agencyId"
                label={t("incidents.form.labels.agency")}
                rules={[
                  {
                    required: true,
                    message: t("incidents.form.validation.agency_required"),
                  },
                ]}
              >
                <Select
                  placeholder={t("incidents.form.placeholders.agency")}
                  options={agencyOptions}
                  loading={agenciesLoading}
                  showSearch
                  filterOption={filterOption}
                  allowClear
                />
              </FormField>
            </Col>
          </Row>
        )}

        {/* ── 2. Classification (type + criticité + échéance) ── */}
        <SectionDivider
          icon={incidentIcons.classification}
          title={t("incidents.form.sections.classification")}
        />

        <Row gutter={24}>
          <Col xs={24} sm={8}>
            <FormField
              name="typeId"
              label={t("incidents.form.labels.type")}
              rules={rules.typeId}
            >
              <Select
                placeholder={t("incidents.form.placeholders.type")}
                options={typeOptions}
                loading={typesLoading}
                suffixIcon={fieldIcons.tags}
                allowClear
                disabled={isStructuralDisabled}
              />
            </FormField>
          </Col>

          <Col xs={24} sm={8}>
            <FormField
              name="criticality"
              label={t("incidents.form.labels.criticality")}
              rules={rules.criticality}
            >
              <Select
                placeholder={t("incidents.form.placeholders.criticality")}
                options={criticalityOptions}
                loading={criticalitiesLoading}
                suffixIcon={fieldIcons.tags}
                allowClear
                disabled={isStructuralDisabled}
              />
            </FormField>
          </Col>

          <Col xs={24} sm={8}>
            <FormField
              name="dueDate"
              label={t("incidents.form.labels.due_date")}
            >
              <DatePicker
                className={styles.datePicker}
                placeholder={t("incidents.form.placeholders.due_date")}
                format={DATE_FORMATS.DISPLAY}
                suffixIcon={fieldIcons.calendar}
                disabled
              />
            </FormField>
          </Col>
        </Row>

        <Row gutter={24}>
          <Col xs={24} sm={8}>
            <FormField
              name="incidentDate"
              label={t("incidents.form.labels.incident_date")}
              dependencies={["observationDate"]}
              rules={rules.incidentDate}
            >
              <DatePicker
                className={styles.datePicker}
                placeholder={t("incidents.form.placeholders.incident_date")}
                disabledDate={disabledFutureDate}
                format={DATE_FORMATS.DISPLAY}
                suffixIcon={fieldIcons.calendar}
                allowClear={false}
                disabled={isStructuralDisabled}
              />
            </FormField>
          </Col>

          <Col xs={24} sm={8}>
            <FormField
              name="observationDate"
              label={t("incidents.form.labels.observation_date")}
              dependencies={["incidentDate"]}
              rules={rules.observationDate}
            >
              <DatePicker
                className={styles.datePicker}
                placeholder={t("incidents.form.placeholders.observation_date")}
                disabledDate={disabledFutureDate}
                format={DATE_FORMATS.DISPLAY}
                suffixIcon={fieldIcons.calendar}
                allowClear={false}
                disabled={isStructuralDisabled}
              />
            </FormField>
          </Col>

          <Col xs={24} sm={8}>
            {isEditMode ? (
              /* Le service de destination decoule du type : en edition il n'est pas
                 saisissable, mais il doit rester visible et suivre le type choisi. */
              <Form.Item
                label={t("incidents.form.labels.target_service")}
                htmlFor={TARGET_SERVICE_DISPLAY_ID}
              >
                <Input
                  id={TARGET_SERVICE_DISPLAY_ID}
                  value={
                    defaultService?.name ??
                    currentServiceName ??
                    t("incidents.form.info.no_target_service")
                  }
                  readOnly
                  disabled
                />
              </Form.Item>
            ) : (
              <FormField
                name="targetServiceId"
                label={t("incidents.form.labels.target_service")}
              >
                <Select
                  placeholder={
                    defaultService
                      ? defaultService.name
                      : t("incidents.form.placeholders.target_service")
                  }
                  options={serviceOptions}
                  loading={servicesLoading}
                  showSearch
                  filterOption={filterOption}
                  allowClear
                  disabled={targetServiceDisabled || isStructuralDisabled}
                />
              </FormField>
            )}
          </Col>
        </Row>

        {!isEditMode && typeHasConstraint && !assignToSelf && (
          <Row gutter={24}>
            <Col span={24}>
              <Alert
                type="info"
                showIcon
                message={
                  defaultService
                    ? t("incidents.form.info.auto_service", {
                      name: defaultService.name,
                    })
                    : t("incidents.form.info.auto_user", {
                      name:
                        `${defaultUser?.firstName ?? ""} ${defaultUser?.lastName ?? ""
                          }`.trim() || defaultUser?.username,
                    })
                }
              />
            </Col>
          </Row>
        )}

        {showSelfAssign && (
          <Row gutter={24}>
            <Col span={24}>
              <Form.Item name="assignToSelf" valuePropName="checked" noStyle>
                <Checkbox disabled={isFormDisabled}>
                  {t("incidents.form.labels.assign_to_self")}
                </Checkbox>
              </Form.Item>
              {assignToSelf && typeHasConstraint && (
                <Alert
                  type="info"
                  showIcon
                  className={styles.selfAssignAlert}
                  message={t("incidents.form.info.self_assign_overrides")}
                />
              )}
            </Col>
          </Row>
        )}

        {/* ── 3. Analyse des causes (optionnel) ── */}
        <SectionDivider
          icon={incidentIcons.cause}
          title={t("incidents.form.sections.cause")}
        />

        <Row gutter={24}>
          <Col xs={24} sm={12}>
            <FormField name="cause" label={t("incidents.form.labels.cause")}>
              <Select
                placeholder={t("incidents.form.placeholders.cause")}
                options={causeOptions}
                loading={causesLoading}
                allowClear
                onChange={() => form.setFieldValue("causeDetail", undefined)}
                disabled={isFormDisabled}
              />
            </FormField>
          </Col>

          {causeDetailConfig && (
            <Col xs={24} sm={12}>
              {causeDetailConfig.type === "text" ? (
                <FormField
                  name="causeDetail"
                  label={causeDetailConfig.label}
                  rules={rules.causeDetail}
                  {...sentenceCaseFormItemProps}
                >
                  <Input
                    placeholder={causeDetailConfig.placeholder}
                    maxLength={2000}
                    disabled={isFormDisabled}
                  />
                </FormField>
              ) : (
                <FormField
                  name="causeDetail"
                  label={causeDetailConfig.label}
                  rules={rules.causeDetail}
                  {...sentenceCaseFormItemProps}
                >
                  <TextArea
                    rows={3}
                    placeholder={causeDetailConfig.placeholder}
                    maxLength={2000}
                    showCount
                    disabled={isFormDisabled}
                  spellCheck={true} />
                </FormField>
              )}
            </Col>
          )}
        </Row>

        {/* ── 4. Pièces jointes ── */}
        {!disabled && (
          <>
            {isEditMode ? (
              /* Mode édition : section complète avec récupération, téléversement et suppression. */
              <AttachmentsSection
                incidentId={incidentId!}
                incidentStatus={incidentStatus}
                creatorId={creatorId}
                onAttachmentsChange={handleAttachmentsChange}
              />
            ) : (
              /* Create mode : liste de fichiers en attente, uploadés après création */
              <>
                <SectionDivider
                  icon={incidentIcons.attachment}
                  title={t("incidents.attachments.title")}
                />

                {pendingFiles.length > 0 && (
                  <List
                    size="small"
                    dataSource={pendingFiles}
                    className={styles.pendingFilesList}
                    renderItem={(file, index) => (
                      <List.Item
                        actions={[
                          <Tooltip
                            key="remove"
                            title={t("incidents.attachments.delete")}
                          >
                            <Button
                              type="text"
                              size="small"
                              danger
                              icon={actionIcons.delete}
                              onClick={() => handleRemovePendingFile(index)}
                              aria-label={t("incidents.attachments.delete")}
                            />
                          </Tooltip>,
                        ]}
                      >
                        <Space size={8}>
                          <span className={styles.pendingFileIcon}>
                            {incidentIcons.attachment}
                          </span>
                          <Text>{file.name}</Text>
                          <Text
                            type="secondary"
                            className={styles.pendingFileSize}
                          >
                            ({(file.size / 1024).toFixed(1)} Ko)
                          </Text>
                        </Space>
                      </List.Item>
                    )}
                  />
                )}

                <Upload
                  beforeUpload={handleAddPendingFile}
                  showUploadList={false}
                  multiple
                  accept={ACCEPTED_ATTACHMENT_ACCEPT}
                >
                  <Button icon={actionIcons.upload}>
                    {t("incidents.attachments.add_button")}
                  </Button>
                </Upload>
              </>
            )}
          </>
        )}

        {/* ── 5. Commentaires (mode édition uniquement) ── */}
        {isEditMode && !disabled && (
          <CommentsSection incidentId={incidentId!} />
        )}

        {/* ── Boutons ── */}
        {showButtons && !disabled && (
          <FormActions
            className={styles.formActionFooter}
            onCancel={onCancel}
            cancelText={t("incidents.form.buttons.cancel")}
            submitText={
              isUploadingFiles
                ? t("incidents.form.buttons.uploading")
                : submitText ||
                  (incidentId
                    ? t("incidents.form.buttons.submit_edit")
                    : t("incidents.form.buttons.submit_create"))
            }
            loading={isPending}
            submitDisabled={isSubmitDisabled}
          />
        )}
      </Form>
    );
  },
);

IncidentForm.displayName = "IncidentForm";

export default IncidentForm;
