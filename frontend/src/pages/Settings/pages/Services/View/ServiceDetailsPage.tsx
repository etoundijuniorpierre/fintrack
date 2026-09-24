// Page affichant les informations completes d'un service.

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
  useDepartment,
  useUpdateDepartment,
  useDeleteDepartment,
} from "../../../../../hooks/settings";
import { useUsers } from "../../../../../hooks/user";
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
  hyperCaseAllWordsFormItemProps,
  sentenceCaseFormItemProps,
} from "../../../../../utils/formatters/formatters";
import { ROLE_NAMES, hasRoleName } from "../../../../../utils/roles/roles";
import type { ServiceRequest } from "../../../../../api/settings/types";
import { actionIcons, detailIcons } from "../../../../../utils/icons/appIcons";
import styles from "./ServiceDetailsPage.module.scss";

const { Title, Text, Paragraph } = Typography;

// Rend le composant ServiceDetailsPage pour l'interface service details page.
const ServiceDetailsPage = memo(() => {
  const { id } = useParams<{ id: string }>();
  const serviceId = id ?? "";
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();

  const { data: service, isLoading } = useDepartment(serviceId);
  const { mutate: deleteService } = useDeleteDepartment();
  const { mutate: updateService, isPending: isUpdating } =
    useUpdateDepartment();
  const { data: users = [], isLoading: usersLoading } = useUsers();

  const [form] = Form.useForm<ServiceRequest>();
  const [isEditing, setIsEditing] = useState(false);
  const [isTouched, setIsTouched] = useState(false);

  const canUpdate = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);
  const canDelete = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    if (isEditing && service) {
      form.setFieldsValue({
        name: service.name,
        description: service.description,
        [ACTIVE_STATUS_FIELD]: service.isActive,
        headUserId: service.headOfService?.id,
      });
      const timer = setTimeout(() => setIsTouched(false), 0);
      return () => clearTimeout(timer);
    }
  }, [isEditing, service, form]);

  // Construit les options de selection affichables.
  const userOptions = useMemo(
    () =>
      users
        .filter((u) => hasRoleName(u.roles, ROLE_NAMES.SERVICE_MANAGER))
        .map((u) => ({
          label: `${u.firstName} ${u.lastName} (${u.username})`,
          value: u.id,
        })),
    [users],
  );

  // Traite le retour a l'ecran precedent.
  const handleBack = useCallback(() => navigate(-1), [navigate]);
  // Traite le demarrage de l'edition.
  const handleEditStart = useCallback(() => {
    if (serviceId) {
      navigate(APP_ROUTES.SETTINGS_SERVICES_EDIT(serviceId));
    }
  }, [navigate, serviceId]);

  const handleEditCancel = useCallback(() => {
    setIsEditing(false);
    setIsTouched(false);
    resetFormState(
      form,
      service
        ? {
            name: service.name,
            description: service.description,
            [ACTIVE_STATUS_FIELD]: service.isActive,
            headUserId: service.headOfService?.id,
          }
        : undefined,
    );
  }, [form, service]);

  // Traite la validation de l'edition.
  const handleEditSubmit = useCallback(
    (values: ServiceRequest) => {
      if (serviceId) {
        updateService(
          { id: serviceId, data: values },
          {
            onSuccess: () => {
              setIsEditing(false);
              setIsTouched(false);
            },
          },
        );
      }
    },
    [serviceId, updateService],
  );

  // Gere la suppression d'un element.
  const handleDelete = useCallback(() => {
    if (serviceId) {
      deleteService(serviceId, {
        onSuccess: () => navigate("/settings#services"),
      });
    }
  }, [serviceId, deleteService, navigate]);

  if (isLoading)
    return (
      <PageContainer>
        <div />
      </PageContainer>
    );
  if (!service)
    return (
      <PageContainer>{t("settings.services.messages.not_found")}</PageContainer>
    );

  return (
    <PageContainer className="animate-entry">
      <PageHeader
        title={isEditing ? t("settings.services.page.editTitle") : service.name}
        onBack={handleBack}
        subtitle={
          !isEditing
            ? t("settings.services.page.detailsSubtitle", {
                name: service.name,
              })
            : undefined
        }
        actions={
          !isEditing ? (
            <Space size="small">
              {canUpdate && (
                <Button
                  icon={
                    service.isActive ? actionIcons.close : actionIcons.validate
                  }
                  onClick={() =>
                    updateService({
                      id: serviceId,
                      data: {
                        name: service.name,
                        description: service.description,
                        headUserId: service.headOfService?.id,
                        isActive: !service.isActive,
                      },
                    })
                  }
                  loading={isUpdating}
                >
                  {service.isActive
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
                  title={t("settings.services.messages.delete_confirm")}
                  description={t("settings.services.messages.delete_ask")}
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
                  label={t("settings.services.form.labels.name")}
                  required
                  rules={[
                    {
                      required: true,
                      message: t(
                        "settings.services.form.validation.name_required",
                      ),
                    },
                  ]}
                  {...hyperCaseAllWordsFormItemProps}
                >
                  <Input
                    placeholder={t("settings.services.form.placeholders.name")}
                    maxLength={100}
                  />
                </FormField>

                <FormField
                  name="description"
                  label={t("settings.services.form.labels.description")}
                  rules={[
                    {
                      max: 1000,
                      message: t(
                        "settings.services.form.validation.description_max",
                      ),
                    },
                  ]}
                  {...sentenceCaseFormItemProps}
                >
                  <Input.TextArea
                    placeholder={t(
                      "settings.services.form.placeholders.description",
                    )}
                    maxLength={1000}
                    rows={6}
                    showCount
                  spellCheck={true} />
                </FormField>
              </Card>
            </Col>

            <Col xs={24} lg={8}>
              <Card
                title={t("common.configuration")}
                className={styles.editCard}
              >
                <FormField
                  name="headUserId"
                  label={t("settings.services.form.labels.headOfService")}
                >
                  <Select
                    options={userOptions}
                    loading={usersLoading}
                    placeholder={t(
                      "settings.services.form.placeholders.headOfService",
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

                <Form.Item
                  label={t("settings.services.form.labels.isActive")}
                  name={ACTIVE_STATUS_FIELD}
                  valuePropName="checked"
                  className={styles.switchItem}
                >
                  <AppSwitch />
                </Form.Item>
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
              <Card className={styles.mainCard}>
                <div className={styles.serviceHeader}>
                  <div className={styles.serviceIcon}>{detailIcons.app}</div>
                  <div className={styles.serviceTitleInfo}>
                    <div className={styles.titleRow}>
                      <Title level={2} className={styles.serviceTitle}>
                        {service.name}
                      </Title>
                      <Tag
                        color={service.isActive ? "success" : "error"}
                        className={styles.statusTag}
                      >
                        {service.isActive
                          ? t("common.active")
                          : t("common.inactive")}
                      </Tag>
                    </div>
                    <Paragraph className={styles.serviceDescription}>
                      {service.description || t("common.noDescription")}
                    </Paragraph>
                  </div>
                </div>
              </Card>

              <Card
                className={styles.membersCard}
                title={
                  <div className={styles.cardTitle}>
                    {detailIcons.team} {t("settings.services.members")}
                  </div>
                }
              >
                <List
                  itemLayout="horizontal"
                  dataSource={service.members || []}
                  className={styles.memberList}
                  renderItem={(member) => (
                    <List.Item className={styles.memberItem}>
                      <List.Item.Meta
                        avatar={
                          <div className={styles.memberAvatar}>
                            {detailIcons.user}
                          </div>
                        }
                        title={
                          <Text
                            className={styles.memberName}
                          >{`${member.firstName} ${member.lastName}`}</Text>
                        }
                        description={
                          <Text className={styles.memberEmail}>
                            {member.email}
                          </Text>
                        }
                      />
                      <Tag color="blue" className={styles.roleTag}>
                        {t("common.member")}
                      </Tag>
                    </List.Item>
                  )}
                  locale={{ emptyText: t("settings.services.noMembers") }}
                />
              </Card>
            </Col>

            <Col xs={24} lg={8}>
              <Card className={styles.sideCard}>
                <Title level={4} className={styles.sideTitle}>
                  {t("common.overview")}
                </Title>
                <div className={styles.infoGrid}>
                  <div className={styles.infoItem}>
                    <div className={styles.infoIcon}>{detailIcons.user}</div>
                    <div className={styles.infoContent}>
                      <Text className={styles.infoLabel}>
                        {t("settings.services.headOfService")}
                      </Text>
                      <Text className={styles.infoValue}>
                        {service.headOfService
                          ? `${service.headOfService.firstName} ${service.headOfService.lastName}`
                          : "-"}
                      </Text>
                      {service.headOfService?.email && (
                        <Text className={styles.infoSubValue}>
                          {service.headOfService.email}
                        </Text>
                      )}
                    </div>
                  </div>

                  <Divider />

                  <div className={styles.infoItem}>
                    <div className={styles.infoIcon}>{detailIcons.team}</div>
                    <div className={styles.infoContent}>
                      <Text className={styles.infoLabel}>
                        {t("settings.services.totalMembers")}
                      </Text>
                      <Text className={styles.infoValue}>
                        {service.members?.length || 0}
                      </Text>
                    </div>
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

ServiceDetailsPage.displayName = "ServiceDetailsPage";

export default ServiceDetailsPage;
