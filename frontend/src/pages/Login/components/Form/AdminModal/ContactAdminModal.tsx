// Composant React : porte l'interface de contact administration modale.

import { Form, Input, message } from "antd";
import { Modal } from "../../../../../components/ui";
import { useTranslation } from "react-i18next";
import { useCallback, useEffect } from "react";
import { useMutation } from "@tanstack/react-query";
import { authApi } from "../../../../../api/user/auth/authApi";
import { sentenceCaseFormItemProps } from "../../../../../utils/formatters/formatters";

// Definit les proprietes attendues par le composant ContactAdminModal.
interface ContactAdminModalProps {
  open: boolean;
  onCancel: () => void;
  username: string;
}

// Rend le composant ContactAdminModal pour l'interface contact administration modale.
const ContactAdminModal = ({
  open,
  onCancel,
  username,
}: ContactAdminModalProps) => {
  const { t } = useTranslation();
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      form.setFieldsValue({ username: username });
    }
  }, [open, username, form]);

  const { mutate, isPending } = useMutation({
    mutationFn: (values: {
      username: string;
      subject: string;
      message: string;
    }) => authApi.contactAdmin(values),
    onSuccess: () => {
      message.success(t("login.contact_admin_success"));
      form.resetFields();
      onCancel();
    },
    onError: () => {
      message.error(t("login.contact_admin_error"));
    },
  });

  const onOk = useCallback(() => {
    form
      .validateFields()
      .then((values) => {
        mutate(values);
      })
      // Les erreurs de validation sont déjà affichées par le formulaire : on évite un rejet non géré.
      .catch(() => {});
  }, [form, mutate]);

  return (
    <Modal
      title={t("login.contact_admin_title")}
      open={open}
      onConfirm={onOk}
      onClose={onCancel}
      confirmLoading={isPending}
      destroyOnClose
      confirmText={t("login.contact_admin_send")}
      cancelText={t("login.contact_admin_cancel")}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="username"
          label={t("login.username_label")}
          rules={[{ required: true, message: t("login.username_required") }]}
        >
          <Input disabled />
        </Form.Item>
        <Form.Item
          name="subject"
          label={t("login.subject_label")}
          rules={[{ required: true, message: t("login.subject_required") }]}
          {...sentenceCaseFormItemProps}
        >
          <Input />
        </Form.Item>
        <Form.Item
          name="message"
          label={t("login.message_label")}
          rules={[{ required: true, message: t("login.message_required") }]}
          {...sentenceCaseFormItemProps}
        >
          <Input.TextArea rows={4} 
          spellCheck={true} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ContactAdminModal;
