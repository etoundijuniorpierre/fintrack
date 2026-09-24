// Page affichant les informations completes d'une agence.

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
  useAgency,
  useUpdateAgency,
  useDeleteAgency,
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
import type { AgencyRequest } from "../../../../../api/settings/types";
import { actionIcons, detailIcons } from "../../../../../utils/icons/appIcons";
import styles from "./AgencyDetailsPage.module.scss";

const { Title, Text, Paragraph } = Typography;

// Rend le composant AgencyDetailsPage pour l'interface agence details page.
const AgencyDetailsPage = memo(() => {
  const { id } = useParams<{ id: string }>();
  const agencyId = id ?? "";
  const navigate = useNavigate();
  const { t } = useTranslation();
  const { hasPermission } = useAuth();

  const { data: agency, isLoading } = useAgency(agencyId);
  const { mutate: deleteAgency } = useDeleteAgency();
  const { mutate: updateAgency, isPending: isUpdating } = useUpdateAgency();
  const { data: users = [], isLoading: usersLoading } = useUsers();

  const [form] = Form.useForm<AgencyRequest>();
  const [isEditing, setIsEditing] = useState(false);
  const [isTouched, setIsTouched] = useState(false);

  const canUpdate = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);
  const canDelete = hasPermission(PERMISSIONS.SETTINGS.SYSTEM);

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    if (isEditing && agency) {
      form.setFieldsValue({
        name: agency.name,
        city: agency.city,
        address: agency.address,
        [ACTIVE_STATUS_FIELD]: agency.isActive,
        headUserId: agency.headOfAgency?.id,
      });
      const timer = setTimeout(() => setIsTouched(false), 0);
      return () => clearTimeout(timer);
    }
  }, [isEditing, agency, form]);

  // Construit les options de selection affichables.
  const userOptions = useMemo(
    () =>
      users
        .filter((u) => hasRoleName(u.roles, ROLE_NAMES.AGENCY_MANAGER))
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
    if (agencyId) {
      navigate(APP_ROUTES.SETTINGS_AGENCIES_EDIT(agencyId));
    }
  }, [agencyId, navigate]);

  // Traite l'annulation de l'edition.
  const handleEditCancel = useCallback(() => {
    setIsEditing(false);
    setIsTouched(false);
    resetFormState(
      form,
      agency
        ? {
            name: agency.name,
            city: agency.city,
            address: agency.address,
            [ACTIVE_STATUS_FIELD]: agency.isActive,
            headUserId: agency.headOfAgency?.id,
          }
        : undefined,
    );
  }, [form, agency]);

  // Traite la validation de l'edition.
  const handleEditSubmit = useCallback(
    (values: AgencyRequest) => {
      if (agencyId) {
        updateAgency(
          { id: agencyId, data: values },
          {
            onSuccess: () => {
              setIsEditing(false);
              setIsTouched(false);
            },
          },
        );
      }
    },
    [agencyId, updateAgency],
  );

  // Gere la suppression d'un element.
  const handleDelete = useCallback(() => {
    if (agencyId) {
      deleteAgency(agencyId, {
        onSuccess: () => navigate("/settings#agencies"),
      });
    }
  }, [agencyId, deleteAgency, navigate]);

  if (isLoading)
    return (
      <PageContainer>
        <div />
      </PageContainer>
    );
  if (!agency)
    return (
      <PageContainer>{t("settings.agencies.messages.not_found")}</PageContainer>
    );

  return (
    <PageContainer className="animate-entry">
      <PageHeader
        title={isEditing ? t("settings.agencies.page.editTitle") : agency.name}
        onBack={handleBack}
        subtitle={
          !isEditing
            ? t("settings.agencies.page.detailsSubtitle", { code: agency.code })
            : undefined
        }
        actions={
          !isEditing ? (
            <Space size="small">
              {canUpdate && (
                <Button
                  icon={
                    agency.isActive ? actionIcons.close : actionIcons.validate
                  }
                  onClick={() =>
                    updateAgency({
                      id: agencyId,
                      data: {
                        name: agency.name,
                        city: agency.city ?? "",
                        address: agency.address,
                        headUserId: agency.headOfAgency?.id,
                        isActive: !agency.isActive,
                      },
                    })
                  }
                  loading={isUpdating}
                >
                  {agency.isActive
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
                  title={t("settings.agencies.messages.delete_confirm")}
                  description={t("settings.agencies.messages.delete_ask")}
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
                <Form.Item label={t("settings.agencies.form.labels.code")}>
                  <Input value={agency.code} disabled />
                </Form.Item>
                <Row gutter={24}>
                  <Col xs={24} md={8}>
                    <FormField
                      name="city"
                      label={t("settings.agencies.form.labels.city")}
                      required
                      rules={[
                        {
                          required: true,
                          message: t(
                            "settings.agencies.form.validation.city_required",
                          ),
                        },
                        {
                          max: 100,
                          message: t(
                            "settings.agencies.form.validation.city_max",
                          ),
                        },
                      ]}
                      {...hyperCaseAllWordsFormItemProps}
                    >
                      <Input
                        placeholder={t(
                          "settings.agencies.form.placeholders.city",
                        )}
                        maxLength={100}
                      />
                    </FormField>
                  </Col>
                  <Col xs={24} md={16}>
                    <FormField
                      name="name"
                      label={t("settings.agencies.form.labels.name")}
                      required
                      rules={[
                        {
                          required: true,
                          message: t(
                            "settings.agencies.form.validation.name_required",
                          ),
                        },
                      ]}
                      {...hyperCaseAllWordsFormItemProps}
                    >
                      <Input
                        placeholder={t(
                          "settings.agencies.form.placeholders.name",
                        )}
                        maxLength={100}
                      />
                    </FormField>
                  </Col>
                </Row>

                <FormField
                  name="address"
                  label={t("settings.agencies.form.labels.address")}
                  rules={[
                    {
                      max: 1000,
                      message: t(
                        "settings.agencies.form.validation.address_max",
                      ),
                    },
                  ]}
                  {...sentenceCaseFormItemProps}
                >
                  <Input.TextArea
                    placeholder={t(
                      "settings.agencies.form.placeholders.address",
                    )}
                    maxLength={1000}
                    rows={4}
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
                  label={t("settings.agencies.form.labels.headOfAgency")}
                >
                  <Select
                    options={userOptions}
                    loading={usersLoading}
                    placeholder={t(
                      "settings.agencies.form.placeholders.headOfAgency",
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
                  label={t("settings.agencies.form.labels.isActive")}
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
                <div className={styles.agencyHeader}>
                  <div className={styles.agencyIcon}>
                    {detailIcons.environment}
                  </div>
                  <div className={styles.agencyTitleInfo}>
                    <div className={styles.titleRow}>
                      <Title level={2} className={styles.agencyTitle}>
                        {agency.name}
                      </Title>
                      <Tag
                        color={agency.isActive ? "success" : "error"}
                        className={styles.statusTag}
                      >
                        {agency.isActive
                          ? t("common.active")
                          : t("common.inactive")}
                      </Tag>
                    </div>
                    <Paragraph className={styles.agencyCode}>
                      <Text type="secondary">
                        {t("settings.agencies.form.labels.code")}:
                      </Text>{" "}
                      <Text strong>{agency.code}</Text>
                    </Paragraph>
                    {agency.city && (
                      <Paragraph className={styles.agencyCode}>
                        <Text type="secondary">
                          {t("settings.agencies.form.labels.city")}:
                        </Text>{" "}
                        <Text strong>{agency.city}</Text>
                      </Paragraph>
                    )}
                    <Paragraph className={styles.agencyAddress}>
                      <span className={styles.addressIcon}>
                        {detailIcons.environment}
                      </span>{" "}
                      {agency.address || t("common.noAddress")}
                    </Paragraph>
                  </div>
                </div>
              </Card>

              <Card
                className={styles.membersCard}
                title={
                  <div className={styles.cardTitle}>
                    {detailIcons.team} {t("settings.agencies.members")}
                  </div>
                }
              >
                <List
                  itemLayout="horizontal"
                  dataSource={agency.members || []}
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
                  locale={{ emptyText: t("settings.agencies.noMembers") }}
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
                        {t("settings.agencies.headOfAgency")}
                      </Text>
                      <Text className={styles.infoValue}>
                        {agency.headOfAgency
                          ? `${agency.headOfAgency.firstName} ${agency.headOfAgency.lastName}`
                          : "-"}
                      </Text>
                      {agency.headOfAgency?.email && (
                        <Text className={styles.infoSubValue}>
                          {agency.headOfAgency.email}
                        </Text>
                      )}
                    </div>
                  </div>

                  <Divider />

                  <div className={styles.infoItem}>
                    <div className={styles.infoIcon}>{detailIcons.team}</div>
                    <div className={styles.infoContent}>
                      <Text className={styles.infoLabel}>
                        {t("settings.agencies.totalMembers")}
                      </Text>
                      <Text className={styles.infoValue}>
                        {agency.members?.length || 0}
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

AgencyDetailsPage.displayName = "AgencyDetailsPage";

export default AgencyDetailsPage;
