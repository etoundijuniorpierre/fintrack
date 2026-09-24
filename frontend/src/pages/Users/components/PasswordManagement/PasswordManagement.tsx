// Composant de reinitialisation et de gestion du mot de passe.

import { useState, useCallback, memo } from "react";
import { Modal, Form, Input, Space, Typography, App } from "antd";
import { Button } from "../../../../components/ui";
import { useTranslation } from "react-i18next";
import { useRegeneratePassword } from "../../../../hooks/user/useUsers";
import { useReauth } from "../../../../hooks/auth/useAuth";
import type { User, LoginCredentials } from "../../../../api/user/types";
import { authIcons, formIcons } from "../../../../utils/icons/appIcons";
import styles from "./PasswordManagement.module.scss";

const { Text } = Typography;

// Definit les proprietes attendues par le composant PasswordManagement.
interface PasswordManagementProps {
  user: User;
  currentUser?: { id?: string; username: string };
}

// Rend le composant PasswordManagement pour l'interface password management.
const PasswordManagement = memo(
  ({ user, currentUser }: PasswordManagementProps) => {
    const { t } = useTranslation();
    const { message, modal } = App.useApp();
    const [isAuthModalVisible, setIsAuthModalVisible] = useState(false);
    const [authForm] = Form.useForm<LoginCredentials>();

    const { mutate: resetPassword, isPending: isResetting } =
      useRegeneratePassword();
    const { mutate: reauth, isPending: isAuthing } = useReauth();

    // Traite l'annulation de la reauthentification.
    const handleAuthModalCancel = useCallback(() => {
      setIsAuthModalVisible(false);
      authForm.resetFields();
    }, [authForm]);

    // Traite l'ouverture de la modale de mot de passe.
    const handleOpenPasswordModal = useCallback(() => {
      setIsAuthModalVisible(true);
    }, []);

    // Traite la reinitialisation du mot de passe.
    const handlePasswordReset = useCallback(() => {
      modal.confirm({
        title: t("users.form.messages.reset_password_confirm"),
        content: t("users.form.messages.reset_password_ask"),
        okText: t("common.yes"),
        cancelText: t("common.no"),
        okButtonProps: { danger: true },
        onOk: () => {
          resetPassword(user.id);
        },
      });
    }, [modal, t, resetPassword, user.id]);

    const onAuthFinish = useCallback(
      (values: LoginCredentials) => {
        reauth(values, {
          onSuccess: () => {
            setIsAuthModalVisible(false);
            authForm.resetFields();
            message.success(t("auth.reauth_success"));

            handlePasswordReset();
          },
          onError: () => message.error(t("auth.reauth_failed")),
        });
      },
      [reauth, t, authForm, message, handlePasswordReset],
    );

    const rules = {
      username: [{ required: true }],
      password: [
        { required: true, message: t("auth.validation.password_required") },
      ],
    };

    return (
      <>
        <div className={styles.passwordSection}>
          <div className={styles.passwordSectionInfo}>
            <span className={styles.passwordSectionIcon}>{formIcons.info}</span>
            <Text type="secondary">
              {t("users.form.messages.password_section_info")}
            </Text>
          </div>
          <Button
            icon={formIcons.lock}
            onClick={handleOpenPasswordModal}
            disabled={!user.isActive && !user.isFirstLogin}
            loading={isResetting}
            variant="secondary"
          >
            {t("users.form.buttons.reset_password")}
          </Button>
          {!user.isActive && !user.isFirstLogin && (
            <Text type="secondary" className={styles.passwordSectionWarning}>
              {t("users.form.messages.user_inactive")}
            </Text>
          )}
        </div>

        <Modal
          title={
            <Space>
              <span className={styles.reauthIcon}>{authIcons.certificate}</span>
              <span>{t("auth.reauth_title")}</span>
            </Space>
          }
          open={isAuthModalVisible}
          onCancel={handleAuthModalCancel}
          footer={null}
          destroyOnHidden
        >
          <div className={styles.reauthMessage}>
            <Text type="secondary">{t("auth.reauth_message")}</Text>
          </div>
          <Form
            form={authForm}
            layout="vertical"
            onFinish={onAuthFinish}
            initialValues={{ username: currentUser?.username }}
          >
            <Form.Item
              name="username"
              label={t("auth.labels.username")}
              rules={rules.username}
            >
              <Input prefix={formIcons.user} disabled />
            </Form.Item>
            <Form.Item
              name="password"
              label={t("auth.labels.password")}
              rules={rules.password}
            >
              <Input.Password prefix={formIcons.lock} autoFocus />
            </Form.Item>
            <Form.Item className={styles.reauthActions}>
              <Space>
                <Button variant="secondary" onClick={handleAuthModalCancel}>
                  {t("common.cancel")}
                </Button>
                <Button
                  variant="primary"
                  htmlType="submit"
                  loading={isAuthing}
                >
                  {t("common.confirm")}
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Modal>
      </>
    );
  },
);

PasswordManagement.displayName = "PasswordManagement";

export default PasswordManagement;
