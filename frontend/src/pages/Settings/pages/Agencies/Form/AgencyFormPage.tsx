// Formulaire de saisie pour creer ou editer une agence.

import { memo, useEffect, useCallback, useMemo, useState } from "react";
import { Form, Input, Row, Col, Select, Divider } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import {
  useCreateAgency,
  useUpdateAgency,
  useAgency,
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
import type { AgencyRequest } from "../../../../../api/settings/types";
import { actionIcons } from "../../../../../utils/icons/appIcons";
import styles from "./AgencyFormPage.module.scss";

// Rend le composant AgencyFormPage pour l'interface agence formulaire page.
const AgencyFormPage = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [form] = Form.useForm<AgencyRequest>();
  const [isTouched, setIsTouched] = useState(false);

  const isEditMode = Boolean(id);

  const { mutate: createAgency, isPending: isCreating } = useCreateAgency();
  const { mutate: updateAgency, isPending: isUpdating } = useUpdateAgency();
  const { data: agencyData, isLoading: agencyLoading } = useAgency(id || "");
  const { data: users = [], isLoading: usersLoading } = useUsers();

  const isPending = isCreating || isUpdating || agencyLoading;

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    if (isEditMode && agencyData) {
      form.setFieldsValue({
        name: agencyData.name,
        city: agencyData.city,
        address: agencyData.address,
        [ACTIVE_STATUS_FIELD]: agencyData.isActive,
        headUserId: agencyData.headOfAgency?.id,
      });
    } else if (!isEditMode) {
      form.resetFields();
      form.setFieldValue(ACTIVE_STATUS_FIELD, true);
    }
  }, [agencyData, form, isEditMode]);

  useResetTouchedOnNextTick(setIsTouched, [agencyData, form, isEditMode]);

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

  // Gere la soumission du formulaire.
  const handleSubmit = useCallback(
    (values: AgencyRequest) => {
      const onDone = () => navigate(APP_ROUTES.SETTINGS + "#agencies");

      if (isEditMode && id) {
        updateAgency({ id, data: values }, { onSuccess: onDone });
      } else {
        createAgency(values, { onSuccess: onDone });
      }
    },
    [isEditMode, id, createAgency, updateAgency, navigate],
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
            ? t("settings.agencies.page.editTitle")
            : t("settings.agencies.page.createTitle")
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
              {isEditMode && agencyData?.code && (
                <Form.Item label={t("settings.agencies.form.labels.code")}>
                  <Input value={agencyData.code} disabled />
                </Form.Item>
              )}
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
                    message: t("settings.agencies.form.validation.address_max"),
                  },
                ]}
                {...sentenceCaseFormItemProps}
              >
                <Input.TextArea
                  placeholder={t("settings.agencies.form.placeholders.address")}
                  maxLength={1000}
                  rows={4}
                spellCheck={true} />
              </FormField>
            </Card>
          </Col>

          <Col xs={24} lg={8}>
            <Card title={t("common.configuration")} className={styles.sideCard}>
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

AgencyFormPage.displayName = "AgencyFormPage";

export default AgencyFormPage;
