// Formulaire de saisie pour l'authentification des utilisateurs.
import { Form, Input, Typography } from "antd";
import { useTranslation } from "react-i18next";
import { useCallback, memo, useState } from "react";
import { useLogin } from "../../../../../hooks/auth/useAuth";
import type { LoginCredentials } from "../../../../../api/user/types";
import {
  getApiErrorCode,
  getApiErrorMessage,
  getFailedLoginAttempts,
  getMaxFailedAttempts,
} from "../../../../../utils/apiMessages/apiMessages";
import FormField from "../../../../../components/ui/FormField/FormField";
import { Button } from "../../../../../components/ui";
import {
  authIcons,
  formIcons,
  detailIcons,
} from "../../../../../utils/icons/appIcons";
import ContactAdminModal from "../AdminModal/ContactAdminModal";
import styles from "./LoginForm.module.scss";

const { Title, Text } = Typography;

// Rend le composant LoginForm pour l'interface connexion formulaire.
const LoginForm = () => {
  const { mutate: login, isPending, error, isError } = useLogin();
  const [form] = Form.useForm();
  const { t, i18n } = useTranslation();
  const [contactModalVisible, setContactModalVisible] = useState(false);

  const onFinish = useCallback(
    (values: LoginCredentials) => {
      login({ ...values, username: values.username.trim() });
    },
    [login],
  );

  const renderPasswordIcon = useCallback(
    (visible: boolean) =>
      visible ? authIcons.eyeVisible : authIcons.eyeHidden,
    [],
  );

  return (
    <section className={styles.formWrapper} aria-label={t("login.title")}>
      <div className={styles.header}>
        <img
          src="/Img/logo.svg"
          alt={t("common.app_name")}
          className={styles.logo}
        />
        <Title level={2} className={styles.title}>
          {t("login.title")}
        </Title>
        <Text className={styles.subtitle}>{t("login.subtitle")}</Text>
      </div>

      {isError && (
        <div className={styles.errorAlert} role="alert">
          <span className={styles.errorIcon}>{formIcons.warning}</span>
          <span>
            {(() => {
              const errorCode = getApiErrorCode(error);
              const backendMessage = getApiErrorMessage(error);

              if (errorCode) {
                const translated = t(`error.user.${errorCode.toLowerCase()}`, {
                  max: getMaxFailedAttempts(error) ?? 5,
                  defaultValue: "_MISSING_",
                });
                if (translated !== "_MISSING_") return translated;
              }

              if (backendMessage.includes(" / ")) {
                const parts = backendMessage.split(" / ");
                return i18n.language.startsWith("fr")
                  ? (parts[1] || parts[0]).trim()
                  : parts[0].trim();
              }

              return backendMessage;
            })()}
          </span>

          {(() => {
            const attempts = getFailedLoginAttempts(error);
            const maxAttemptsFromApi = getMaxFailedAttempts(error);
            const MAX_ATTEMPTS =
              maxAttemptsFromApi !== null ? maxAttemptsFromApi : 5;
            if (attempts !== null && attempts > 0) {
              return (
                <span className={styles.failedAttemptsCounter}>
                  {t("login.errors.failed_attempts", {
                    count: attempts,
                    max: MAX_ATTEMPTS,
                    defaultValue: `${attempts}/${MAX_ATTEMPTS} failed attempts`,
                  })}
                </span>
              );
            }
            return null;
          })()}
          <div className={styles.contactAdminWrapper}>
            <a
              className={styles.contactAdminLink}
              onClick={() => setContactModalVisible(true)}
            >
              <span className={styles.contactAdminIcon}>
                {detailIcons.mail}
              </span>
              {t("login.contact_admin")}
            </a>
          </div>
        </div>
      )}

      <Form
        form={form}
        name="login-form"
        onFinish={onFinish}
        layout="vertical"
        requiredMark={false}
        className={styles.form}
        autoComplete="on"
      >
        <FormField
          name="username"
          label={t("login.username_label")}
          required
          rules={[{ required: true, message: t("login.username_required") }]}
        >
          <Input
            id="login-username"
            prefix={<span className={styles.inputIcon}>{formIcons.user}</span>}
            placeholder={t("login.username_placeholder")}
            size="large"
            className={styles.input}
            autoComplete="username"
          />
        </FormField>

        <FormField
          name="password"
          label={t("login.password_label")}
          required
          rules={[{ required: true, message: t("login.password_required") }]}
        >
          <Input.Password
            id="login-password"
            prefix={<span className={styles.inputIcon}>{formIcons.lock}</span>}
            placeholder={t("login.password_placeholder")}
            size="large"
            className={styles.input}
            autoComplete="current-password"
            iconRender={renderPasswordIcon}
          />
        </FormField>

        <Form.Item className={styles.submitItem}>
          <Button
            id="login-submit"
            variant="primary"
            htmlType="submit"
            size="lg"
            loading={isPending}
            className={styles.submitButton}
            block
          >
            {isPending ? t("login.submitting") : t("login.submit_button")}
          </Button>
        </Form.Item>
      </Form>

      <footer className={styles.footer}>
        <Text className={styles.footerText}>
          {t("login.footer", { year: new Date().getFullYear() })}
        </Text>
      </footer>
      <ContactAdminModal
        open={contactModalVisible}
        onCancel={() => setContactModalVisible(false)}
        username={form.getFieldValue("username") || ""}
      />
    </section>
  );
};

export default memo(LoginForm);
