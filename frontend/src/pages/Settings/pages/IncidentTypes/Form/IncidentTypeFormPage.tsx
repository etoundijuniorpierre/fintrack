// Formulaire de saisie pour creer ou editer un type d'incident.

import { memo, useEffect, useMemo, useCallback, useState } from "react";
import {
  Form,
  Input,
  InputNumber,
  Select,
  Row,
  Col,
  Typography,
  Alert,
  Checkbox,
} from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import {
  useCreateIncidentType,
  useUpdateIncidentType,
  useIncidentType,
  useDepartments,
} from "../../../../../hooks/settings";
import { useAssignableUsers, useDirectionValidators } from "../../../../../hooks/user";
import {
  PageHeader,
  FormField,
  Card,
  FormActions,
  AppSwitch,
} from "../../../../../components/ui";
import { PageContainer } from "../../../../../components/Layout";
import { useResetTouchedOnNextTick } from "../../../../../hooks/ui/useResetTouchedOnNextTick/useResetTouchedOnNextTick";
import { APP_ROUTES } from "../../../../../utils/constants";
import { ACTIVE_STATUS_FIELD } from "../../../../../utils/form/activeStatusField";
import {
  hyperCaseAllWordsFormItemProps,
  sentenceCaseFormItemProps,
} from "../../../../../utils/formatters/formatters";
import type { IncidentTypeConfigRequest } from "../../../../../api/settings/types";
import { INCIDENT_ACTOR_ROLES } from "../../../../../api/settings/types";
import type { UserSummaryResponse } from "../../../../../api/user/types";
import { Criticality } from "../../../../../api/incident/enums/enums";
import { actionIcons } from "../../../../../utils/icons/appIcons";
import { mapIncidentTypeConfigToRequest } from "../incidentTypeConfigMapper";
import styles from "./IncidentTypeFormPage.module.scss";

const { Text } = Typography;

// Rend le composant IncidentTypeFormPage pour l'interface incident type formulaire page.
const IncidentTypeFormPage = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [form] = Form.useForm<IncidentTypeConfigRequest>();
  const [isTouched, setIsTouched] = useState(false);

  const selectedServiceId = Form.useWatch("defaultTargetServiceId", form);
  const requiresValidation = Form.useWatch("requiresValidation", form);
  const requiresDirectionValidation = Form.useWatch(
    "requiresDirectionValidation",
    form,
  );

  const isEditMode = Boolean(id);

  const { mutate: createIncidentType, isPending: isCreating } =
    useCreateIncidentType();
  const { mutate: updateIncidentType, isPending: isUpdating } =
    useUpdateIncidentType();
  const { data: departments = [], isLoading: depsLoading } = useDepartments();
  const {
    data: users = [],
    isLoading: usersLoading,
    isSuccess: assignableUsersLoaded,
  } = useAssignableUsers(
    { serviceId: selectedServiceId },
    { enabled: Boolean(selectedServiceId) },
  );
  const { data: directionValidators = [], isLoading: validatorsLoading } =
    useDirectionValidators();
  const { data: incidentTypeData, isLoading: incidentTypeLoading } =
    useIncidentType(id || "");

  const isPending = isCreating || isUpdating || incidentTypeLoading;

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    if (isEditMode && incidentTypeData) {
      form.setFieldsValue(mapIncidentTypeConfigToRequest(incidentTypeData));
    } else if (!isEditMode) {
      form.resetFields();
      form.setFieldValue(ACTIVE_STATUS_FIELD, true);
      form.setFieldValue("requiresValidation", true);
      form.setFieldValue("requiresDirectionValidation", true);
      form.setFieldValue("directionValidatorIds", []);
      form.setFieldValue("requiresCauseAnalysis", true);
      form.setFieldValue("emailNotificationsEnabled", false);
      form.setFieldValue("treaterRoles", ["ASSIGNEE"]);
      form.setFieldValue("resolverRoles", ["SOURCE_AGENCY_MANAGER"]);
      form.setFieldValue("closerRoles", ["ASSIGNEE"]);
      form.setFieldValue("reopenerRoles", ["SOURCE_AGENCY_MANAGER"]);
      form.setFieldValue("validatorScope", "SOURCE_SERVICE_MANAGER");
      form.setFieldValue("defaultCriticality", "HIGH");
    }
  }, [incidentTypeData, form, isEditMode]);

  useResetTouchedOnNextTick(setIsTouched, [incidentTypeData, form, isEditMode]);

  const departmentOptions = useMemo(
    () => departments.map((d) => ({ label: d.name, value: d.id })),
    [departments],
  );



  const selectedService = useMemo(
    () => departments.find((d) => d.id === selectedServiceId),
    [departments, selectedServiceId],
  );

  const showNoHeadWarning =
    selectedServiceId && !selectedService?.headOfService;

  // Construit les options de selection affichables.
  const userOptions = useMemo(
    () =>
      users.map((u) => ({
        label: `${u.firstName} ${u.lastName} (${u.username})`,
        value: u.id,
      })),
    [users],
  );

  // Reinitialise l'utilisateur selectionne seulement apres validation de son eligibilite.
  useEffect(() => {
    const currentUserId = form.getFieldValue("defaultTargetUserId");
    if (!currentUserId) return;

    if (!selectedServiceId || !assignableUsersLoaded) return;

    const isStillEligible = userOptions.some(
      (opt) => opt.value === currentUserId,
    );
    if (!isStillEligible) {
      form.setFieldValue("defaultTargetUserId", undefined);
    }
  }, [
    assignableUsersLoaded,
    form,
    selectedServiceId,
    userOptions,
  ]);

  // Gere la soumission du formulaire.
  const handleSubmit = useCallback(
    (values: IncidentTypeConfigRequest) => {
      const onDone = () => navigate(APP_ROUTES.SETTINGS + "#incidentTypes");

      if (isEditMode && id) {
        updateIncidentType({ id, data: values }, { onSuccess: onDone });
      } else {
        createIncidentType(values, { onSuccess: onDone });
      }
    },
    [isEditMode, id, createIncidentType, updateIncidentType, navigate],
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
            ? t("settings.incidentTypes.page.editTitle")
            : t("settings.incidentTypes.page.createTitle")
        }
        subtitle={
          isEditMode
            ? t("settings.incidentTypes.page.editSubtitle")
            : t("settings.incidentTypes.page.createSubtitle")
        }
        onBack={handleBack}
      />

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        disabled={isPending}
        className={styles.form}
        onFieldsChange={(changedFields) => {
          setIsTouched(true);
          const serviceChanged = changedFields.some(
            (field) => field.name[0] === "defaultTargetServiceId",
          );
          if (serviceChanged) {
            form.setFieldValue("defaultTargetUserId", undefined);
          }
        }}
      >
        <Row gutter={[24, 24]}>
          {/* Colonne Gauche : Infos de base, Affectation & Statut/Notifications */}
          <Col xs={24} lg={12}>
            <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
              <Card
                title={t("settings.incidentTypes.sections.basicAndSla")}
                loading={incidentTypeLoading}
                className={styles.formCard}
              >
                <Row gutter={16}>
                  <Col xs={24} md={12}>
                    <FormField
                      name="name"
                      label={t("settings.incidentTypes.form.labels.name")}
                      required
                      rules={[
                        {
                          required: true,
                          message: t(
                            "settings.incidentTypes.form.validation.name_required",
                          ),
                        },
                      ]}
                      {...hyperCaseAllWordsFormItemProps}
                    >
                      <Input
                        placeholder={t(
                          "settings.incidentTypes.form.placeholders.name",
                        )}
                        maxLength={100}
                      />
                    </FormField>
                  </Col>
                  <Col xs={24} md={12}>
                    <FormField
                      name="displayName"
                      label={t("settings.incidentTypes.form.labels.displayName")}
                      required
                      rules={[
                        {
                          required: true,
                          message: t(
                            "settings.incidentTypes.form.validation.displayName_required",
                          ),
                        },
                      ]}
                      {...hyperCaseAllWordsFormItemProps}
                    >
                      <Input
                        placeholder={t(
                          "settings.incidentTypes.form.placeholders.displayName",
                        )}
                        maxLength={200}
                      />
                    </FormField>
                  </Col>
                </Row>

                <FormField
                  name="description"
                  label={t("settings.incidentTypes.form.labels.description")}
                  rules={[
                    {
                      max: 500,
                      message: t(
                        "settings.incidentTypes.form.validation.description_max",
                      ),
                    },
                  ]}
                  {...sentenceCaseFormItemProps}
                >
                  <Input.TextArea
                    placeholder={t(
                      "settings.incidentTypes.form.placeholders.description",
                    )}
                    maxLength={500}
                    rows={3}
                    showCount
                    spellCheck={true}
                  />
                </FormField>

                <Row gutter={16}>
                  <Col xs={24} md={12}>
                    <FormField
                      name="slaHours"
                      label={t("settings.incidentTypes.form.labels.slaHours")}
                    >
                      <InputNumber
                        min={1}
                        precision={0}
                        placeholder={t(
                          "settings.incidentTypes.form.placeholders.slaHours",
                        )}
                        className={styles.fullWidthInput}
                      />
                    </FormField>
                  </Col>
                  <Col xs={24} md={12}>
                    <FormField
                      name="defaultCriticality"
                      label={t(
                        "settings.incidentTypes.form.labels.defaultCriticality",
                      )}
                      extra={t(
                        "settings.incidentTypes.form.descriptions.defaultCriticality",
                      )}
                    >
                      <Select
                        allowClear
                        placeholder={t(
                          "settings.incidentTypes.form.placeholders.defaultCriticality",
                        )}
                        options={[
                          {
                            value: Criticality.LOW,
                            label: t("incidents.criticality.LOW"),
                          },
                          {
                            value: Criticality.MEDIUM,
                            label: t("incidents.criticality.MEDIUM"),
                          },
                          {
                            value: Criticality.HIGH,
                            label: t("incidents.criticality.HIGH"),
                          },
                          {
                            value: Criticality.CRITICAL,
                            label: t("incidents.criticality.CRITICAL"),
                          },
                        ]}
                      />
                    </FormField>
                  </Col>
                </Row>
              </Card>

              <Card
                title={t("settings.incidentTypes.sections.assignment")}
                loading={incidentTypeLoading}
                className={styles.sideCard}
              >
                <Row gutter={16}>
                  <Col xs={24} md={12}>
                    <FormField
                      name="defaultTargetServiceId"
                      label={t(
                        "settings.incidentTypes.form.labels.defaultTargetService",
                      )}
                    >
                      <Select
                        options={departmentOptions}
                        loading={depsLoading}
                        placeholder={t(
                          "settings.incidentTypes.form.placeholders.defaultTargetService",
                        )}
                        allowClear
                      />
                    </FormField>
                  </Col>
                  <Col xs={24} md={12}>
                    <FormField
                      name="defaultTargetUserId"
                      label={t(
                        "settings.incidentTypes.form.labels.defaultTargetUser",
                      )}
                    >
                      <Select
                        options={userOptions}
                        loading={usersLoading}
                        placeholder={t(
                          "settings.incidentTypes.form.placeholders.defaultTargetUser",
                        )}
                        allowClear
                        showSearch
                        filterOption={(input, option) =>
                          String(option?.label ?? "")
                            .toLowerCase()
                            .includes(input.toLowerCase())
                        }
                      />
                    </FormField>
                  </Col>
                </Row>

                {showNoHeadWarning && (
                  <Alert
                    type="warning"
                    showIcon
                    title={t(
                      "settings.incidentTypes.form.warnings.noServiceHead",
                    )}
                    className={styles.fullRowAlert}
                  />
                )}
              </Card>

              <Card
                title={t("settings.incidentTypes.sections.statusAndNotifications")}
                loading={incidentTypeLoading}
                className={styles.sideCard}
              >
                <div className={styles.switchItemCard}>
                  <div className={styles.switchTextContainer}>
                    <Text className={styles.switchTitle}>
                      {t(
                        "settings.incidentTypes.form.labels.emailNotificationsEnabled",
                      )}
                    </Text>
                    <Text className={styles.switchLabel}>
                      {t(
                        "settings.incidentTypes.form.descriptions.emailNotificationsEnabled",
                      )}
                    </Text>
                  </div>
                  <div className={styles.switchControl}>
                    <Form.Item
                      name="emailNotificationsEnabled"
                      valuePropName="checked"
                      noStyle
                    >
                      <AppSwitch />
                    </Form.Item>
                  </div>
                </div>

                <div className={styles.switchItemCard}>
                  <div className={styles.switchTextContainer}>
                    <Text className={styles.switchTitle}>
                      {t("settings.incidentTypes.form.labels.isActive")}
                    </Text>
                    <Text className={styles.switchLabel}>
                      {t("settings.incidentTypes.form.descriptions.isActive")}
                    </Text>
                  </div>
                  <div className={styles.switchControl}>
                    <Form.Item
                      name={ACTIVE_STATUS_FIELD}
                      valuePropName="checked"
                      noStyle
                    >
                      <AppSwitch />
                    </Form.Item>
                  </div>
                </div>
              </Card>
            </div>
          </Col>

          {/* Colonne Droite : Règles de validation & Acteurs du workflow */}
          <Col xs={24} lg={12}>
            <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
              <Card
                title={t("settings.incidentTypes.sections.validation")}
                loading={incidentTypeLoading}
                className={styles.sideCard}
              >
                <div className={styles.switchItemCard}>
                  <div className={styles.switchTextContainer}>
                    <Text className={styles.switchTitle}>
                      {t("settings.incidentTypes.form.labels.requiresValidation")}
                    </Text>
                    <Text className={styles.switchLabel}>
                      {t(
                        "settings.incidentTypes.form.descriptions.requiresValidation",
                      )}
                    </Text>
                  </div>
                  <div className={styles.switchControl}>
                    <Form.Item
                      name="requiresValidation"
                      valuePropName="checked"
                      noStyle
                    >
                      <AppSwitch />
                    </Form.Item>
                  </div>
                </div>

                {requiresValidation && (
                  <FormField
                    name="validatorScope"
                    label={t("settings.incidentTypes.form.labels.validatorScope")}
                    extra={t(
                      "settings.incidentTypes.form.descriptions.validatorScope",
                    )}
                  >
                    <Select
                      options={[
                        {
                          value: "SOURCE_SERVICE_MANAGER",
                          label: t(
                            "settings.incidentTypes.form.options.validator_source_service",
                          ),
                        },
                        {
                          value: "AGENCY_MANAGER",
                          label: t(
                            "settings.incidentTypes.form.options.validator_agency",
                          ),
                        },
                        {
                          value: "TARGET_SERVICE_MANAGER",
                          label: t(
                            "settings.incidentTypes.form.options.validator_target_service",
                          ),
                        },
                        {
                          value: "ADMIN",
                          label: t(
                            "settings.incidentTypes.form.options.validator_admin",
                          ),
                        },
                      ]}
                    />
                  </FormField>
                )}

                <div className={styles.switchItemCard}>
                  <div className={styles.switchTextContainer}>
                    <Text className={styles.switchTitle}>
                      {t(
                        "settings.incidentTypes.form.labels.requiresDirectionValidation",
                      )}
                    </Text>
                    <Text className={styles.switchLabel}>
                      {t(
                        "settings.incidentTypes.form.descriptions.requiresDirectionValidation",
                      )}
                    </Text>
                  </div>
                  <div className={styles.switchControl}>
                    <Form.Item
                      name="requiresDirectionValidation"
                      valuePropName="checked"
                      noStyle
                    >
                      <AppSwitch />
                    </Form.Item>
                  </div>
                </div>

                {requiresDirectionValidation && (
                  <FormField
                    name="directionValidatorIds"
                    label={t(
                      "settings.incidentTypes.form.labels.directionValidator",
                    )}
                    extra={t(
                      "settings.incidentTypes.form.descriptions.directionValidator",
                    )}
                    rules={[
                      {
                        required: true,
                        type: "array",
                        min: 1,
                        message: t(
                          "settings.incidentTypes.form.rules.directionValidatorRequired",
                        ),
                      },
                    ]}
                  >
                    <Select
                      mode="multiple"
                      allowClear
                      showSearch
                      optionFilterProp="label"
                      loading={validatorsLoading}
                      placeholder={t(
                        "settings.incidentTypes.form.placeholders.directionValidator",
                      )}
                      options={directionValidators.map((u: UserSummaryResponse) => ({
                        value: u.id,
                        label: `${[u.firstName, u.lastName].filter(Boolean).join(" ") || u.username} (${u.email})`,
                      }))}
                    />
                  </FormField>
                )}

                <div className={styles.switchItemCard}>
                  <div className={styles.switchTextContainer}>
                    <Text className={styles.switchTitle}>
                      {t(
                        "settings.incidentTypes.form.labels.requiresCauseAnalysis",
                      )}
                    </Text>
                    <Text className={styles.switchLabel}>
                      {t(
                        "settings.incidentTypes.form.descriptions.requiresCauseAnalysis",
                      )}
                    </Text>
                  </div>
                  <div className={styles.switchControl}>
                    <Form.Item
                      name="requiresCauseAnalysis"
                      valuePropName="checked"
                      noStyle
                    >
                      <AppSwitch />
                    </Form.Item>
                  </div>
                </div>
              </Card>

              <Card
                title={t("settings.incidentTypes.sections.actors")}
                loading={incidentTypeLoading}
                className={styles.sideCard}
              >
                <FormField
                  name="treaterRoles"
                  label={t("settings.incidentTypes.form.labels.treaterType")}
                  extra={t("settings.incidentTypes.form.descriptions.treaterType")}
                >
                  <Checkbox.Group
                    options={INCIDENT_ACTOR_ROLES.map((role) => ({
                      value: role,
                      label: t(`settings.incidentTypes.form.actorRoles.${role}`),
                    }))}
                  />
                </FormField>

                <FormField
                  name="resolverRoles"
                  label={t("settings.incidentTypes.form.labels.resolverType")}
                  extra={t("settings.incidentTypes.form.descriptions.resolverType")}
                >
                  <Checkbox.Group
                    options={INCIDENT_ACTOR_ROLES.map((role) => ({
                      value: role,
                      label: t(`settings.incidentTypes.form.actorRoles.${role}`),
                    }))}
                  />
                </FormField>

                <FormField
                  name="closerRoles"
                  label={t("settings.incidentTypes.form.labels.closerType")}
                  extra={t("settings.incidentTypes.form.descriptions.closerType")}
                >
                  <Checkbox.Group
                    options={INCIDENT_ACTOR_ROLES.map((role) => ({
                      value: role,
                      label: t(`settings.incidentTypes.form.actorRoles.${role}`),
                    }))}
                  />
                </FormField>

                <FormField
                  name="reopenerRoles"
                  label={t("settings.incidentTypes.form.labels.reopenerType")}
                  extra={t("settings.incidentTypes.form.descriptions.reopenerType")}
                >
                  <Checkbox.Group
                    options={INCIDENT_ACTOR_ROLES.map((role) => ({
                      value: role,
                      label: t(`settings.incidentTypes.form.actorRoles.${role}`),
                    }))}
                  />
                </FormField>
              </Card>
            </div>
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

IncidentTypeFormPage.displayName = "IncidentTypeFormPage";

export default IncidentTypeFormPage;
