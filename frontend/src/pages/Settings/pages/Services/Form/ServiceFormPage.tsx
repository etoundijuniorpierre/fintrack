// Formulaire de saisie pour ajouter ou mettre a jour un service.
import { memo, useEffect, useCallback, useMemo, useState } from "react";
import { Form, Input, Row, Col, Select, Divider } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import {
  useCreateDepartment,
  useUpdateDepartment,
  useDepartment,
} from "../../../../../hooks/settings";
import { useUsers } from "../../../../../hooks/user";
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
import { ROLE_NAMES, hasRoleName } from "../../../../../utils/roles/roles";
import type { ServiceRequest } from "../../../../../api/settings/types";
import { actionIcons } from "../../../../../utils/icons/appIcons";
import styles from "./ServiceFormPage.module.scss";

// Rend le composant ServiceFormPage pour l'interface service formulaire page.
const ServiceFormPage = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [form] = Form.useForm<ServiceRequest>();
  const [isTouched, setIsTouched] = useState(false);

  const isEditMode = Boolean(id);

  const { mutate: createService, isPending: isCreating } =
    useCreateDepartment();
  const { mutate: updateService, isPending: isUpdating } =
    useUpdateDepartment();
  const { data: serviceData, isLoading: serviceLoading } = useDepartment(
    id || "",
  );
  const { data: users = [], isLoading: usersLoading } = useUsers();

  const isPending = isCreating || isUpdating || serviceLoading;

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    if (isEditMode && serviceData) {
      form.setFieldsValue({
        name: serviceData.name,
        description: serviceData.description,
        [ACTIVE_STATUS_FIELD]: serviceData.isActive,
        headUserId: serviceData.headOfService?.id,
      });
    } else if (!isEditMode) {
      form.resetFields();
      form.setFieldValue(ACTIVE_STATUS_FIELD, true);
    }
  }, [serviceData, form, isEditMode]);

  useResetTouchedOnNextTick(setIsTouched, [serviceData, form, isEditMode]);

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

  // Gere la soumission du formulaire.
  const handleSubmit = useCallback(
    (values: ServiceRequest) => {
      const onDone = () => navigate(APP_ROUTES.SETTINGS + "#services");

      if (isEditMode && id) {
        updateService({ id, data: values }, { onSuccess: onDone });
      } else {
        createService(values, { onSuccess: onDone });
      }
    },
    [isEditMode, id, createService, updateService, navigate],
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
            ? t("settings.services.page.editTitle")
            : t("settings.services.page.createTitle")
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
            <Card title={t("common.basicInfo")} className={styles.formCard}>
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
            <Card title={t("common.configuration")} className={styles.sideCard}>
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

ServiceFormPage.displayName = "ServiceFormPage";

export default ServiceFormPage;
