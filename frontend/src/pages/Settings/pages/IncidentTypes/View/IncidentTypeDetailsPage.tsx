// Page de details d'un type d'incident.

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
  Form,
  Input,
  InputNumber,
  Select,
  Checkbox,
} from "antd";
import { useTranslation } from "react-i18next";
import {
  useIncidentType,
  useUpdateIncidentType,
  useDeleteIncidentType,
  useDepartments,
} from "../../../../../hooks/settings";
import { useUsers, useDirectionValidators } from "../../../../../hooks/user";
import { useAuth } from "../../../../../hooks/auth/useAuth";
import { PERMISSIONS } from "../../../../../utils/permissions/permissions";
import {
  PageHeader,
  Card,
  ConfirmDialog,
  FormField,
  FormActions,
  AppSwitch,
} from "../../../../../components/ui";
import { PageContainer } from "../../../../../components/Layout";
import { APP_ROUTES } from "../../../../../utils/constants";
import { ACTIVE_STATUS_FIELD } from "../../../../../utils/form/activeStatusField";
import { resetFormState } from "../../../../../utils/form/formReset/formReset";
import {
  formatDateTime,
  hyperCaseAllWordsFormItemProps,
  sentenceCaseFormItemProps,
} from "../../../../../utils/formatters/formatters";
import type { IncidentTypeConfigRequest } from "../../../../../api/settings/types";
import { INCIDENT_ACTOR_ROLES } from "../../../../../api/settings/types";
import type { UserSummaryResponse } from "../../../../../api/user/types";
import { Criticality } from "../../../../../api/incident/enums/enums";
import { actionIcons, detailIcons } from "../../../../../utils/icons/appIcons";
import styles from "./IncidentTypeDetailsPage.module.scss";
import { mapIncidentTypeConfigToRequest } from "../incidentTypeConfigMapper";

const { Title, Text, Paragraph } = Typography;

// Rend le composant IncidentTypeDetailsPage pour l'interface incident type details page.
const IncidentTypeDetailsPage = memo(() => {
  const { id } = useParams<{ id: string }>();
  const incidentTypeId = id ?? "";
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();

  const { data: incidentType, isLoading } = useIncidentType(incidentTypeId);
  const { mutate: deleteIncidentType } = useDeleteIncidentType();
  const { mutate: updateIncidentType, isPending: isUpdating } =
    useUpdateIncidentType();
  const { data: departments = [], isLoading: depsLoading } = useDepartments();
  const { data: users = [], isLoading: usersLoading } = useUsers();
  const { data: directionValidators = [], isLoading: validatorsLoading } =
    useDirectionValidators();

  const [form] = Form.useForm<IncidentTypeConfigRequest>();
  const [isEditing, setIsEditing] = useState(false);
  const [isTouched, setIsTouched] = useState(false);

  const canUpdate = hasPermission(PERMISSIONS.SETTINGS.INCIDENT_TYPES);
  const canDelete = hasPermission(PERMISSIONS.SETTINGS.INCIDENT_TYPES);
  const requiresValidation = Form.useWatch("requiresValidation", form);
  const requiresDirectionValidation = Form.useWatch(
    "requiresDirectionValidation",
    form,
  );

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    if (isEditing && incidentType) {
      form.setFieldsValue(mapIncidentTypeConfigToRequest(incidentType));
      const timer = setTimeout(() => setIsTouched(false), 0);
      return () => clearTimeout(timer);
    }
  }, [isEditing, incidentType, form]);

  const departmentOptions = useMemo(
    () => departments.map((d) => ({ label: d.name, value: d.id })),
    [departments],
  );

  // Construit les options de selection affichables.
  const userOptions = useMemo(
    () =>
      users.map((u) => ({
        label: `${u.firstName} ${u.lastName} (${u.username})`,
        value: u.id,
      })),
    [users],
  );

  const rolesLabel = (roles?: string[]) =>
    roles && roles.length > 0
      ? roles
          .map((role) => t(`settings.incidentTypes.form.actorRoles.${role}`))
          .join(", ")
      : "-";

  const treaterTypeLabel = rolesLabel(incidentType?.treaterRoles);
  const resolverTypeLabel = rolesLabel(incidentType?.resolverRoles);
  const closerTypeLabel = rolesLabel(incidentType?.closerRoles);
  const reopenerTypeLabel = rolesLabel(incidentType?.reopenerRoles);

  // Construit une requete complete pour eviter les mises a jour partielles invalides.
  const buildPayload = useCallback(
    (
      overrides: Partial<IncidentTypeConfigRequest> = {},
    ): IncidentTypeConfigRequest | null => {
      if (!incidentType) return null;
      return {
        ...mapIncidentTypeConfigToRequest(incidentType),
        ...overrides,
      };
    },
    [incidentType],
  );

  // Traite le retour a l'ecran precedent.
  const handleBack = useCallback(() => navigate(-1), [navigate]);
  // Traite le demarrage de l'edition.
  const handleEditStart = useCallback(() => {
    if (incidentTypeId) {
      navigate(APP_ROUTES.SETTINGS_INCIDENT_TYPES_EDIT(incidentTypeId));
    }
  }, [incidentTypeId, navigate]);

  // Traite l'annulation de l'edition.
  const handleEditCancel = useCallback(() => {
    setIsEditing(false);
    setIsTouched(false);
    resetFormState(form, buildPayload() ?? undefined);
  }, [form, buildPayload]);

  // Traite la validation de l'edition.
  const handleEditSubmit = useCallback(
    (values: IncidentTypeConfigRequest) => {
      if (incidentTypeId) {
        updateIncidentType(
          { id: incidentTypeId, data: values },
          {
            onSuccess: () => {
              setIsEditing(false);
              setIsTouched(false);
            },
          },
        );
      }
    },
    [incidentTypeId, updateIncidentType],
  );

  // Bascule le statut actif sans perdre les autres parametres du type.
  const handleToggleActive = useCallback(() => {
    const payload = buildPayload({
      [ACTIVE_STATUS_FIELD]: !incidentType?.isActive,
    });
    if (incidentTypeId && payload) {
      updateIncidentType({ id: incidentTypeId, data: payload });
    }
  }, [
    buildPayload,
    incidentType?.isActive,
    incidentTypeId,
    updateIncidentType,
  ]);

  // Gere la suppression d'un element.
  const handleDelete = useCallback(() => {
    if (incidentTypeId) {
      deleteIncidentType(incidentTypeId, {
        onSuccess: () => navigate("/settings#incidentTypes"),
      });
    }
  }, [incidentTypeId, deleteIncidentType, navigate]);

  if (isLoading)
    return (
      <PageContainer>
        <div />
      </PageContainer>
    );
  if (!incidentType)
    return (
      <PageContainer>
        {t("settings.incidentTypes.messages.not_found")}
      </PageContainer>
    );

  return (
    <PageContainer className="animate-entry">
      <PageHeader
        title={
          isEditing
            ? t("settings.incidentTypes.page.editTitle")
            : incidentType.displayName
        }
        onBack={handleBack}
        subtitle={
          !isEditing
            ? t("settings.incidentTypes.page.detailsSubtitle", {
                name: incidentType.name,
              })
            : undefined
        }
        actions={
          !isEditing ? (
            <Space size="small">
              {canUpdate && (
                <Button
                  icon={
                    incidentType.isActive
                      ? actionIcons.close
                      : actionIcons.validate
                  }
                  onClick={handleToggleActive}
                  loading={isUpdating}
                >
                  {incidentType.isActive
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
                  title={t("settings.incidentTypes.messages.delete_confirm")}
                  description={t("settings.incidentTypes.messages.delete_ask")}
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
          <Row gutter={[24, 24]}>
            <Col xs={24}>
              <Card title={t("common.basicInfo")} className={styles.editCard}>
                <Row gutter={24}>
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
                      label={t(
                        "settings.incidentTypes.form.labels.displayName",
                      )}
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
                  spellCheck={true} />
                </FormField>
              </Card>
            </Col>

            <Col xs={24}>
              <Card
                title={t("common.configuration")}
                className={styles.editSideCard}
              >
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

                <Divider />

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

                <Divider />

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

                <FormField
                  name="validatorScope"
                  label={t("settings.incidentTypes.form.labels.validatorScope")}
                  extra={t(
                    "settings.incidentTypes.form.descriptions.validatorScope",
                  )}
                >
                  <Select
                    disabled={!requiresValidation}
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

                <Divider />

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
                  extra={t(
                    "settings.incidentTypes.form.descriptions.closerType",
                  )}
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
                  extra={t("settings.incidentTypes.form.descriptions.reopenerType")}>
                  <Checkbox.Group
                    options={INCIDENT_ACTOR_ROLES.map((role) => ({
                      value: role,
                      label: t(`settings.incidentTypes.form.actorRoles.${role}`),
                    }))}
                  />
                </FormField>

                <Divider />

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
          <div className={styles.detailsStack}>
            <Card className={styles.mainCard}>
              <div className={styles.typeHeader}>
                <div className={styles.typeIcon}>{detailIcons.thunderbolt}</div>
                <div className={styles.typeTitleInfo}>
                  <div className={styles.titleRow}>
                    <Title level={2} className={styles.typeTitle}>
                      {incidentType.displayName}
                    </Title>
                    <Tag
                      color={incidentType.isActive ? "success" : "error"}
                      className={styles.statusTag}
                    >
                      {incidentType.isActive
                        ? t("common.active")
                        : t("common.inactive")}
                    </Tag>
                  </div>
                  <Paragraph className={styles.typeDescription}>
                    {incidentType.description || t("common.noDescription")}
                  </Paragraph>
                </div>
              </div>
            </Card>

            <Row gutter={[24, 24]}>
              <Col xs={24} lg={12}>
                <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
                  <Card className={styles.sideCard}>
                    <Title level={4} className={styles.sideTitle}>
                      {t("settings.incidentTypes.sections.basicAndSla")}
                    </Title>
                    <div className={styles.infoGrid}>
                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.key}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.form.labels.name")}
                          </Text>
                          <Text className={styles.infoValue}>{incidentType.name}</Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.clock}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.table.slaHours")}
                          </Text>
                          <Text className={styles.infoValue}>
                            {incidentType.slaHours
                              ? `${incidentType.slaHours} ${t("common.hours")}`
                              : "-"}
                          </Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.thunderbolt}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t(
                              "settings.incidentTypes.form.labels.defaultCriticality",
                            )}
                          </Text>
                          <Text className={styles.infoValue}>
                            {incidentType.defaultCriticality
                              ? t(
                                  `incidents.criticality.${incidentType.defaultCriticality}`,
                                )
                              : "-"}
                          </Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.app}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.table.defaultTargetService")}
                          </Text>
                          <Text className={styles.infoValue}>
                            {incidentType.defaultTargetService?.name || "-"}
                          </Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.user}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.table.defaultTargetUser")}
                          </Text>
                          <Text className={styles.infoValue}>
                            {incidentType.defaultTargetUser
                              ? `${incidentType.defaultTargetUser.firstName} ${incidentType.defaultTargetUser.lastName}`
                              : "-"}
                          </Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.mail}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t(
                              "settings.incidentTypes.form.labels.emailNotificationsEnabled",
                            )}
                          </Text>
                          <Text className={styles.infoValue}>
                            {incidentType.emailNotificationsEnabled
                              ? t("common.yes")
                              : t("common.no")}
                          </Text>
                        </div>
                      </div>
                    </div>
                  </Card>

                  <Card className={styles.sideCard}>
                    <Title level={4} className={styles.sideTitle}>
                      {t("settings.incidentTypes.sections.actors")}
                    </Title>
                    <div className={styles.infoGrid}>
                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.user}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.form.labels.treaterType")}
                          </Text>
                          <Text className={styles.infoValue}>{treaterTypeLabel}</Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.user}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.form.labels.resolverType")}
                          </Text>
                          <Text className={styles.infoValue}>{resolverTypeLabel}</Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.key}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.table.closerType")}
                          </Text>
                          <Text className={styles.infoValue}>{closerTypeLabel}</Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{actionIcons.reopen}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.form.labels.reopenerType")}
                          </Text>
                          <Text className={styles.infoValue}>{reopenerTypeLabel}</Text>
                        </div>
                      </div>
                    </div>
                  </Card>
                </div>
              </Col>

              <Col xs={24} lg={12}>
                <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
                  <Card className={styles.sideCard}>
                    <Title level={4} className={styles.sideTitle}>
                      {t("settings.incidentTypes.sections.validation")}
                    </Title>
                    <div className={styles.infoGrid}>
                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{actionIcons.validate}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.form.labels.requiresValidation")}
                          </Text>
                          <Text className={styles.infoValue}>
                            {incidentType.requiresValidation
                              ? t("common.yes")
                              : t("common.no")}
                          </Text>
                        </div>
                      </div>

                      {incidentType.requiresValidation && (
                        <>
                          <Divider />
                          <div className={styles.infoItem}>
                            <div className={styles.infoIcon}>{detailIcons.user}</div>
                            <div className={styles.infoContent}>
                              <Text className={styles.infoLabel}>
                                {t(
                                  "settings.incidentTypes.form.labels.validatorScope",
                                )}
                              </Text>
                              <Text className={styles.infoValue}>
                                {t(
                                  {
                                    SOURCE_SERVICE_MANAGER:
                                      "settings.incidentTypes.form.options.validator_source_service",
                                    AGENCY_MANAGER:
                                      "settings.incidentTypes.form.options.validator_agency",
                                    TARGET_SERVICE_MANAGER:
                                      "settings.incidentTypes.form.options.validator_target_service",
                                    ADMIN:
                                      "settings.incidentTypes.form.options.validator_admin",
                                  }[incidentType.validatorScope] ??
                                    "settings.incidentTypes.form.options.validator_source_service",
                                )}
                              </Text>
                            </div>
                          </div>
                        </>
                      )}

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{actionIcons.validate}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t(
                              "settings.incidentTypes.form.labels.requiresDirectionValidation",
                            )}
                          </Text>
                          <Text className={styles.infoValue}>
                            {incidentType.requiresDirectionValidation
                              ? t("common.yes")
                              : t("common.no")}
                          </Text>
                        </div>
                      </div>

                      {incidentType.requiresDirectionValidation && (
                        <>
                          <Divider />
                          <div className={styles.infoItem}>
                            <div className={styles.infoIcon}>{detailIcons.user}</div>
                            <div className={styles.infoContent}>
                              <Text className={styles.infoLabel}>
                                {t(
                                  "settings.incidentTypes.details.directionValidators",
                                )}
                              </Text>
                              <Text className={styles.infoValue}>
                                {incidentType.directionValidators?.length
                                  ? incidentType.directionValidators
                                      .map(
                                        (validator) =>
                                          `${validator.firstName} ${validator.lastName}`,
                                      )
                                      .join(", ")
                                  : "-"}
                              </Text>
                            </div>
                          </div>
                        </>
                      )}

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.solution}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t(
                              "settings.incidentTypes.form.labels.requiresCauseAnalysis",
                            )}
                          </Text>
                          <Text className={styles.infoValue}>
                            {incidentType.requiresCauseAnalysis
                              ? t("common.yes")
                              : t("common.no")}
                          </Text>
                        </div>
                      </div>
                    </div>
                  </Card>

                  <Card className={styles.sideCard}>
                    <Title level={4} className={styles.sideTitle}>
                      {t("settings.incidentTypes.details.metadata")}
                    </Title>
                    <div className={styles.infoGrid}>
                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.key}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.details.identifier")}
                          </Text>
                          <Text className={styles.infoValue}>{incidentType.id}</Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.clock}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.details.createdAt")}
                          </Text>
                          <Text className={styles.infoValue}>
                            {formatDateTime(incidentType.createdAt)}
                          </Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.clock}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.details.updatedAt")}
                          </Text>
                          <Text className={styles.infoValue}>
                            {formatDateTime(incidentType.updatedAt)}
                          </Text>
                        </div>
                      </div>

                      <Divider />

                      <div className={styles.infoItem}>
                        <div className={styles.infoIcon}>{detailIcons.user}</div>
                        <div className={styles.infoContent}>
                          <Text className={styles.infoLabel}>
                            {t("settings.incidentTypes.details.modifiedBy")}
                          </Text>
                          <Text className={styles.infoValue}>
                            {incidentType.modifiedBy || "-"}
                          </Text>
                        </div>
                      </div>
                    </div>
                  </Card>
                </div>
              </Col>
            </Row>
          </div>
        </div>
      )}
    </PageContainer>
  );
});

IncidentTypeDetailsPage.displayName = "IncidentTypeDetailsPage";

export default IncidentTypeDetailsPage;
