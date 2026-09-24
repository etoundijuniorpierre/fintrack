// Formulaire de saisie pour creer ou modifier un role utilisateur.
import { memo, useEffect, useMemo, useCallback, useRef, useState } from "react";
import { Form, Input, Row, Col, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate, useParams } from "react-router-dom";
import {
  useCreateRole,
  useUpdateRole,
  usePermissions,
  useRole,
  useRoles,
} from "../../../../../hooks/settings";
import {
  getRoleTranslationKey,
  normalizeRoleName,
  ROLE_NAMES,
} from "../../../../../utils/roles/roles";
import {
  PageHeader,
  FormField,
  Card,
  FormActions,
} from "../../../../../components/ui";
import { PageContainer } from "../../../../../components/Layout";
import { useResetTouchedOnNextTick } from "../../../../../hooks/ui/useResetTouchedOnNextTick/useResetTouchedOnNextTick";
import { APP_ROUTES } from "../../../../../utils/constants";
import { PERMISSIONS } from "../../../../../utils/permissions/permissions";
import { useAuth } from "../../../../../hooks/auth/useAuth";
import {
  hyperCaseAllWordsFormItemProps,
  sentenceCaseFormItemProps,
} from "../../../../../utils/formatters/formatters";
import type { RoleRequest } from "../../../../../api/settings/types";
import { PermissionChecklist } from "../../../../Users/components/Form/PermissionChecklist";
import { actionIcons, formIcons } from "../../../../../utils/icons/appIcons";
import styles from "./RoleFormPage.module.scss";

const { Text } = Typography;

// Rend le composant RoleFormPage pour l'interface role formulaire page.
const RoleFormPage = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [form] = Form.useForm<RoleRequest>();
  const [isTouched, setIsTouched] = useState(false);
  const defaultsApplied = useRef(false);

  const isEditMode = Boolean(id);

  const { mutate: createRole, isPending: isCreating } = useCreateRole();
  const { mutate: updateRole, isPending: isUpdating } = useUpdateRole();
  const { data: permissions = [], isLoading: permsLoading } = usePermissions();
  const { data: roles = [], isLoading: rolesLoading } = useRoles();
  const { data: roleData, isLoading: roleLoading } = useRole(id || "");
  const { hasPermission } = useAuth();
  // Anti-escalade : seul un profil qui detient deja "creer un administrateur"
  // peut l'octroyer a un role (les autres ne la voient pas dans la liste).
  const canGrantCreateAdmin = hasPermission(PERMISSIONS.USER.CREATE_ADMIN);

  const isPending =
    isCreating || isUpdating || roleLoading || (!isEditMode && rolesLoading);

  // Effet secondaire pour charger ou synchroniser les donnees.
  useEffect(() => {
    if (isEditMode && roleData) {
      const key = getRoleTranslationKey(roleData.name);
      const resolvedDescription = t(`users.role_descriptions.${key}`, {
        defaultValue: roleData.description ?? "",
      });
      form.setFieldsValue({
        name: roleData.name,
        displayName: roleData.displayName ?? "",
        description: resolvedDescription,
        permissionIds: roleData.permissions?.map((p) => p.id) ?? [],
      });
    } else if (!isEditMode && !defaultsApplied.current) {
      const agentRole = roles.find(
        (role) => normalizeRoleName(role.name) === ROLE_NAMES.AGENT,
      );
      if (!agentRole) return;
      form.resetFields();
      form.setFieldValue(
        "permissionIds",
        agentRole.permissions?.map((permission) => permission.id) ?? [],
      );
      defaultsApplied.current = true;
    }
  }, [roleData, roles, form, isEditMode, t]);

  useResetTouchedOnNextTick(setIsTouched, [roleData, form, isEditMode]);

  const permissionOptions = useMemo(
    () =>
      permissions
        .filter(
          (p) =>
            canGrantCreateAdmin || p.name !== PERMISSIONS.USER.CREATE_ADMIN,
        )
        .map((p) => ({
          label: String(t(`users.permissions.${p.name}`, p.name)),
          value: p.id,
          desc: String(
            t(`users.permission_descriptions.${p.name}`, p.description ?? ""),
          ),
          name: p.name,
        })),
    [permissions, t, canGrantCreateAdmin],
  );

  // Gere la soumission du formulaire.
  const handleSubmit = useCallback(
    (values: RoleRequest) => {
      const onDone = () => navigate(APP_ROUTES.SETTINGS + "#rolesPermissions");

      if (isEditMode && id) {
        updateRole({ id, data: values }, { onSuccess: onDone });
      } else {
        createRole(values, { onSuccess: onDone });
      }
    },
    [isEditMode, id, createRole, updateRole, navigate],
  );

  // Traite le retour a l'ecran precedent.
  const handleBack = useCallback(() => {
    navigate(-1);
  }, [navigate]);

  return (
    <PageContainer>
      <PageHeader
        title={
          isEditMode
            ? t("settings.roles.page.editTitle")
            : t("settings.roles.page.createTitle")
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
        <Row gutter={[24, 24]}>
          <Col xs={24} lg={8}>
            <Card
              title={t("settings.roles.sections.identity")}
              className={styles.infoCard}
            >
              <FormField
                name="name"
                label={t("settings.roles.form.labels.name")}
                required
                rules={[
                  {
                    required: true,
                    message: t("settings.roles.form.validation.name_required"),
                  },
                ]}
                {...hyperCaseAllWordsFormItemProps}
              >
                <Input
                  placeholder={t("settings.roles.form.placeholders.name")}
                  maxLength={100}
                  disabled={isEditMode && roleData?.isSystem}
                  prefix={
                    isEditMode && roleData?.isSystem ? formIcons.lock : null
                  }
                />
              </FormField>

              <FormField
                name="displayName"
                label={t("settings.roles.form.labels.display_name")}
                rules={[
                  {
                    max: 100,
                    message: t(
                      "settings.roles.form.validation.display_name_max",
                    ),
                  },
                ]}
                {...hyperCaseAllWordsFormItemProps}
              >
                <Input
                  placeholder={t(
                    "settings.roles.form.placeholders.display_name",
                  )}
                  maxLength={100}
                />
              </FormField>

              <FormField
                name="description"
                label={t("settings.roles.form.labels.description")}
                rules={[
                  {
                    max: 1000,
                    message: t(
                      "settings.roles.form.validation.description_max",
                    ),
                  },
                ]}
                {...sentenceCaseFormItemProps}
              >
                <Input.TextArea
                  placeholder={t(
                    "settings.roles.form.placeholders.description",
                  )}
                  maxLength={1000}
                  rows={4}
                spellCheck={true} />
              </FormField>

              {roleData?.isSystem && (
                <div className={styles.systemNotice}>
                  {formIcons.info}
                  <Text type="secondary">
                    {t("settings.roles.messages.system_role_edit_notice")}
                  </Text>
                </div>
              )}
            </Card>
          </Col>

          <Col xs={24} lg={16}>
            <Card
              title={t("settings.roles.form.labels.permissions")}
              loading={permsLoading}
              className={styles.permissionsCard}
            >
              <div className={styles.permissionsGrid}>
                <Form.Item name="permissionIds">
                  <PermissionChecklist
                    options={permissionOptions}
                    loading={permsLoading}
                    disabled={isPending}
                  />
                </Form.Item>
              </div>
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

RoleFormPage.displayName = "RoleFormPage";

export default RoleFormPage;
