// Formulaire permettant a l'utilisateur de modifier ses donnees de profil.

import React, { memo } from "react";
import { Divider, Form, Input, Space, Row, Col } from "antd";
import { useTranslation } from "react-i18next";
import type { User } from "../../../../api/user/types";
import { isValidEmailWithSubdomain, isValidPhone } from "../../../../utils/validators/validators";
import styles from "./ProfileForm.module.scss";
import { phoneFormItemProps } from "../../../../utils/formatters/formatters";
import { Button } from "../../../../components/ui";
import { actionIcons, formIcons } from "../../../../utils/icons/appIcons";

// Definit les proprietes attendues par le composant ProfileForm.
interface ProfileFormProps {
  form: ReturnType<typeof Form.useForm>[0];
  initialValues?: Partial<User>;
  isEditing: boolean;
  isActive?: boolean;
  canChangePassword?: boolean;
  onFieldsChange?: () => void;
  onCancel?: () => void;
  onSave?: () => void;
  isSaving?: boolean;
  isTouched?: boolean;
}

// Rend le composant ProfileForm.
const ProfileForm: React.FC<ProfileFormProps> = memo(
  ({
    form,
    initialValues,
    isEditing,
    isActive = true,
    canChangePassword = false,
    onFieldsChange,
    onCancel,
    onSave,
    isSaving = false,
    isTouched = false,
  }) => {
    const { t } = useTranslation();

    return (
      <Form
        form={form}
        layout="vertical"
        initialValues={initialValues}
        disabled={!isEditing}
        className={styles.form}
        onFieldsChange={onFieldsChange}
      >
        <Form.Item
          name="username"
          label={t("myProfile.labels.username")}
          rules={[
            {
              required: true,
              whitespace: true,
              message: t("myProfile.validation.username_required"),
            },
          ]}
        >
          <Input maxLength={100} />
        </Form.Item>
        <Form.Item
          name="email"
          label={t("myProfile.labels.email")}
          rules={[
            { type: "email", message: t("myProfile.validation.email_invalid") },
            {
              validator: async (_, value) => {
                if (value && !isValidEmailWithSubdomain(value)) {
                  throw new Error(
                    t("myProfile.validation.email_subdomain_invalid"),
                  );
                }
                return Promise.resolve();
              },
            }
          ]}
        >
          <Input maxLength={100} disabled />
        </Form.Item>
        <Form.Item
          name="phoneNumber"
          label={t("myProfile.labels.phone")}
          rules={[
            {
              validator: (_, value) =>
                !value || isValidPhone(String(value).replace(/\s/g, ""))
                  ? Promise.resolve()
                  : Promise.reject(
                      new Error(t("myProfile.validation.phone_invalid")),
                    ),
            },
          ]}
          {...phoneFormItemProps}
        >
          <Input />
        </Form.Item>

        {isEditing && canChangePassword && (
          <>
            <Divider>{t("myProfile.change_password.title")}</Divider>
            <Form.Item
              name="currentPassword"
              label={t("myProfile.change_password.current_label")}
              dependencies={["newPassword"]}
              rules={[
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!getFieldValue("newPassword") || value)
                      return Promise.resolve();
                    return Promise.reject(
                      new Error(
                        t("myProfile.change_password.current_required"),
                      ),
                    );
                  },
                }),
              ]}
            >
              <Input.Password prefix={formIcons.lock} disabled={!isActive} />
            </Form.Item>
            <Form.Item
              name="newPassword"
              label={t("myProfile.change_password.new_label")}
              rules={[
                {
                  min: 8,
                  message: t("myProfile.validation.password_min_length"),
                },
              ]}
            >
              <Input.Password prefix={formIcons.lock} disabled={!isActive} />
            </Form.Item>
            <Form.Item
              name="confirmPassword"
              label={t("myProfile.change_password.confirm_label")}
              dependencies={["newPassword"]}
              rules={[
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue("newPassword") === value)
                      return Promise.resolve();
                    return Promise.reject(
                      new Error(t("myProfile.validation.password_mismatch")),
                    );
                  },
                }),
              ]}
            >
              <Input.Password prefix={formIcons.lock} disabled={!isActive} />
            </Form.Item>
          </>
        )}

        {isEditing && (
          <>
            <Divider />
            <Row justify="end">
              <Col>
                <Space size={12}>
                  {onCancel && (
                    <Button
                      variant="secondary"
                      icon={actionIcons.close}
                      onClick={onCancel}
                      aria-label={t("myProfile.buttons.cancel")}
                    >
                      {t("myProfile.buttons.cancel")}
                    </Button>
                  )}
                  {onSave && (
                    <Button
                      variant="primary"
                      icon={actionIcons.save}
                      onClick={onSave}
                      loading={isSaving}
                      disabled={!isTouched || isSaving}
                      aria-label={t("myProfile.buttons.save")}
                    >
                      {t("myProfile.buttons.save")}
                    </Button>
                  )}
                </Space>
              </Col>
            </Row>
          </>
        )}
      </Form>
    );
  },
);

ProfileForm.displayName = "ProfileForm";

export default ProfileForm;
