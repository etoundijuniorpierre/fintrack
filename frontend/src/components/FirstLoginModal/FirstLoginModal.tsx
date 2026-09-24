// Modale de premiere connexion : force l'utilisateur a definir un nouveau mot de passe.
import { Modal, Form, Input, Typography, App, Space } from "antd";
import { Button } from "../ui";
import { useTranslation } from "react-i18next";
import { useEffect, useCallback, memo, useMemo } from "react";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "../../api/user/auth/authApi";
import { useAuthStore } from "../../store/authStore/authStore";
import { useLogout } from "../../hooks/auth/useAuth";
import { getApiErrorMessage } from "../../utils/apiMessages/apiMessages";
import { authIcons, formIcons } from "../../utils/icons/appIcons";

import styles from "./FirstLoginModal.module.scss";

const { Title, Text } = Typography;

// Definit les proprietes attendues par le composant FirstLoginModal.
interface FirstLoginModalProps {
  open: boolean;
  onClose: () => void;
}

// Rend le composant FirstLoginModal pour l'interface first connexion modale.
const FirstLoginModal = ({ open, onClose }: FirstLoginModalProps) => {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const { t } = useTranslation();
  const { user, login } = useAuthStore();
  const { mutate: logout } = useLogout();

  // Mutation de changement de mot de passe : ferme la modale en cas de succes.
  const { mutate: changePassword, isPending } = useMutation({
    mutationFn: async (variables: {
      currentPassword: string;
      newPassword: string;
    }) => {
      await authApi.changePassword(user!.id, variables);
      return authApi.login({
        username: user!.username,
        password: variables.newPassword,
      });
    },
    onSuccess: (data) => {
      login(data.token, {
        id: data.id,
        username: data.username,
        roles: Array.from(data.roles),
        permissions: Array.from(data.permissions),
        serviceId: data.serviceId,
        agencyId: data.agencyId,
        managedServiceIds: data.managedServiceIds,
        managedAgencyId: data.managedAgencyId,
        isActive: data.isActive,
        isFirstLogin: false,
      });
      message.success(t("first_login.password_changed_success"));
      onClose();
    },
    onError: (error) => {
      message.error(getApiErrorMessage(error));
    },
  });

  // Reinitialise le formulaire a chaque ouverture de la modale.
  useEffect(() => {
    if (open) {
      form.resetFields();
    }
  }, [open, form]);

  // Soumet le nouveau mot de passe saisi.
  const handleSubmit = useCallback(
    (values: { newPassword: string }) => {
      changePassword({
        currentPassword: "",
        newPassword: values.newPassword,
      });
    },
    [changePassword],
  );

  // Annule l'operation et deconnecte l'utilisateur.
  const handleCancel = useCallback(() => {
    if (!isPending) {
      onClose();
      logout();
    }
  }, [isPending, logout, onClose]);

  const passwordRules = useMemo(
    () => [
      { required: true, message: t("first_login.password_required") },
      { min: 8, message: t("first_login.password_min_length") },
    ],
    [t],
  );

  // Regle de validation verifiant que la confirmation correspond au nouveau mot de passe.
  const confirmRules = useMemo(
    () => [
      { required: true, message: t("first_login.confirm_password_required") },
      ({ getFieldValue }: { getFieldValue: (name: string) => string }) => ({
        validator(_: unknown, value: string) {
          return !value || getFieldValue("newPassword") === value
            ? Promise.resolve()
            : Promise.reject(new Error(t("first_login.passwords_not_match")));
        },
      }),
    ],
    [t],
  );

  const passwordInputProps = useMemo(
    () => ({
      prefix: formIcons.lock,
      iconRender: (visible: boolean) =>
        visible ? authIcons.eyeVisible : authIcons.eyeHidden,
      size: "large" as const,
    }),
    [],
  );

  return (
    <Modal
      title={
        <div className={styles.modalHeader}>
          <span className={styles.headerIcon}>{formIcons.lock}</span>
          <Title level={4} className={styles.modalTitle}>
            {t("first_login.title")}
          </Title>
        </div>
      }
      open={open}
      onCancel={handleCancel}
      footer={null}
      closable={true}
      mask={{ closable: false }}
      width={480}
      className={styles.firstLoginModal}
    >
      <div className={styles.modalContent}>
        <Text className={styles.welcomeMessage}>
          {t("first_login.welcome_message", { username: user?.username })}
        </Text>
        <Text className={styles.instructionMessage}>
          {t("first_login.instruction")}
        </Text>

        <Form
          form={form}
          onFinish={handleSubmit}
          layout="vertical"
          requiredMark={false}
          className={styles.passwordForm}
        >
          <Form.Item
            name="newPassword"
            label={t("first_login.new_password_label")}
            rules={passwordRules}
          >
            <Input.Password
              {...passwordInputProps}
              placeholder={t("first_login.new_password_placeholder")}
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            label={t("first_login.confirm_password_label")}
            rules={confirmRules}
          >
            <Input.Password
              {...passwordInputProps}
              placeholder={t("first_login.confirm_password_placeholder")}
            />
          </Form.Item>

          <Form.Item className={styles.submitItem}>
            <Space direction="vertical" className={styles.submitSpace}>
              <Button
                variant="primary"
                htmlType="submit"
                size="lg"
                loading={isPending}
                block
                className={styles.submitButton}
              >
                {isPending
                  ? t("first_login.changing")
                  : t("first_login.change_password_button")}
              </Button>
              <Button
                variant="secondary"
                size="lg"
                block
                disabled={isPending}
                onClick={handleCancel}
              >
                {t("first_login.cancel_and_logout")}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </div>
    </Modal>
  );
};

export default memo(FirstLoginModal);
