// Page affichant les details du profil utilisateur en cours.
import React, { useState, useCallback, useMemo, memo } from "react";
import { Form, Space, App, Descriptions, Tag, Typography, Upload } from "antd";
import { useTranslation } from "react-i18next";
import { useAuthStore } from "../../store/authStore/authStore";
import { useMyProfile, useUpdateProfile } from "../../hooks/user/useMyProfile";
import ProfileForm from "./components/ProfileForm/ProfileForm";
import RolesSection from "./components/RolesSection/RolesSection";
import UserAvatar from "../../components/UserAvatar/UserAvatar";
import { documentApi } from "../../api/document/documentApi";
import { authApi } from "../../api/user/auth/authApi";
import { getApiErrorMessage } from "../../utils/apiMessages/apiMessages";
import type { ProfileUpdateRequest } from "../../api/user/types";
import { PageContainer, SectionCard } from "../../components/Layout";
import { PageHeader, Button } from "../../components/ui";
import PageLoader from "../../components/Loading/PageLoader";
import { actionIcons } from "../../utils/icons/appIcons";
import { resetFormState } from "../../utils/form/formReset/formReset";
import { PERMISSIONS } from "../../utils/permissions/permissions";

// Rend le composant MyProfile.
const MyProfile: React.FC = memo(() => {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const {
    user: authUser,
    hasPermission,
    login: updateAuthSession,
    logout,
  } = useAuthStore();
  // Determine l'identifiant du profil courant.
  const userId = authUser?.id ?? "";
  const canChangePassword = hasPermission(PERMISSIONS.USER.MANAGE_PROFILE);

  const { data: profile, isError, refetch } = useMyProfile(userId);
  const { mutate: updateProfile, isPending: isSaving } = useUpdateProfile();

  const [form] = Form.useForm();
  const [isEditing, setIsEditing] = useState(false);
  const [isTouched, setIsTouched] = useState(false);

  // Traite le passage en edition.
  const handleEdit = useCallback(() => {
    form.setFieldsValue({
      username: profile?.username,
      email: profile?.email,
      phoneNumber: profile?.phoneNumber,
    });
    setIsTouched(false);
    setIsEditing(true);
  }, [form, profile]);

  // Annule l'action en cours.
  const handleCancel = useCallback(() => {
    resetFormState(
      form,
      profile
        ? {
            username: profile.username,
            email: profile.email,
            phoneNumber: profile.phoneNumber,
          }
        : undefined,
    );
    setIsTouched(false);
    setIsEditing(false);
  }, [form, profile]);

  // Traite la modification des champs.
  const handleFieldsChange = useCallback(() => {
    setIsTouched(true);
  }, []);

  // Traite l'enregistrement.
  const handleSave = useCallback(async () => {
    try {
      const values = await form.validateFields();
      const data: ProfileUpdateRequest = {
        username: values.username.trim(),
        email: profile?.email,
        phoneNumber: values.phoneNumber,
        avatarDocumentId: profile?.avatarDocumentId,
      };
      const usernameChanged = data.username !== authUser?.username;

      updateProfile(
        { id: userId, data },
        {
          onSuccess: async () => {
            if (usernameChanged) {
              try {
                const session = await authApi.refreshToken();
                updateAuthSession(session.token, {
                  id: session.id,
                  username: session.username,
                  roles: Array.from(session.roles),
                  permissions: Array.from(session.permissions),
                  serviceId: session.serviceId,
                  agencyId: session.agencyId,
                  managedServiceIds: session.managedServiceIds,
                  managedAgencyId: session.managedAgencyId,
                  isActive: session.isActive,
                  isFirstLogin: Boolean(session.isFirstLogin),
                });
              } catch (err) {
                message.error(getApiErrorMessage(err));
                logout();
                return;
              }
            }

            message.success(t("myProfile.messages.update_success"));

            if (values.currentPassword && values.newPassword) {
              try {
                await authApi.changePassword(userId, {
                  currentPassword: values.currentPassword,
                  newPassword: values.newPassword,
                });
                message.success(
                  t("myProfile.messages.password_change_success"),
                );
              } catch (err) {
                message.error(getApiErrorMessage(err));
              }
            }

            setIsTouched(false);
            setIsEditing(false);
          },
          onError: () => {
            message.error(t("myProfile.messages.update_error"));
          },
        },
      );
    } catch (err) {
      console.error("Erreur lors de la sauvegarde du profil :", err);
    }
  }, [
    authUser,
    form,
    logout,
    message,
    profile,
    t,
    updateAuthSession,
    updateProfile,
    userId,
  ]);

  // Traite l'import de l'avatar.
  const handleAvatarUpload = useCallback(
    async (file: File) => {
      try {
        const attachment = await documentApi.uploadAvatar(file);
        updateProfile(
          {
            id: userId,
            data: {
              username: profile?.username ?? authUser?.username ?? "",
              email: profile?.email,
              phoneNumber: profile?.phoneNumber,
              avatarDocumentId: attachment.id,
            },
          },
          {
            onSuccess: () => {
              message.success(t("myProfile.messages.avatar_update_success"));
            },
            onError: () => {
              void documentApi.delete(attachment.id).catch(() => undefined);
              message.error(t("myProfile.messages.avatar_update_error"));
            },
          },
        );
      } catch {
        message.error(t("myProfile.messages.avatar_upload_error"));
      }
    },
    [authUser, message, profile, t, updateProfile, userId],
  );

  const personalInfoToolbar = useMemo(
    () => (
      <Space>
        {!isEditing && (
          <Button
            key="edit"
            icon={actionIcons.edit}
            onClick={handleEdit}
            aria-label={t("myProfile.buttons.edit")}
          >
            {t("myProfile.buttons.edit")}
          </Button>
        )}
      </Space>
    ),
    [isEditing, handleEdit, t],
  );

  const roles = useMemo(() => profile?.roles ?? [], [profile?.roles]);

  // Un chef de service est, par construction, le responsable de son propre
  // service : on le detecte en comparant l'utilisateur courant au chef defini
  // sur le service auquel il est rattache.
  const isHeadOfOwnService = Boolean(
    profile?.service?.headOfService?.id &&
    profile.service.headOfService.id === userId,
  );

  if (isError) {
    return (
      <PageLoader isLoading={false} isError={true} onRetry={refetch}>
        <span />
      </PageLoader>
    );
  }

  return (
    <PageContainer>
      <PageHeader title={t("myProfile.title")} />

      <SectionCard title={t("myProfile.sections.avatar")}>
        <Space size={16} align="center">
          <UserAvatar avatarDocumentId={profile?.avatarDocumentId} size={72} />
          <Upload
            accept="image/*"
            showUploadList={false}
            beforeUpload={(file) => {
              if (file.size === 0) {
                message.error(t("incidents.attachments.empty_file", { name: file.name }));
                return Upload.LIST_IGNORE;
              }
              if (!file.type.startsWith("image/")) {
                message.error(t("myProfile.validation.avatar_image"));
                return Upload.LIST_IGNORE;
              }
              if (file.size > 2 * 1024 * 1024) {
                message.error(t("myProfile.validation.avatar_size"));
                return Upload.LIST_IGNORE;
              }
              return true;
            }}
            customRequest={({ file, onSuccess }) => {
              void handleAvatarUpload(file as File).then(() =>
                onSuccess?.("ok"),
              );
            }}
          >
            <Button
              variant="secondary"
              icon={actionIcons.upload}
              loading={isSaving}
            >
              {t("myProfile.buttons.change_avatar")}
            </Button>
          </Upload>
        </Space>
      </SectionCard>

      <SectionCard
        title={t("myProfile.sections.personal")}
        toolbar={personalInfoToolbar}
      >
        <ProfileForm
          form={form}
          initialValues={profile}
          isEditing={isEditing}
          isActive={profile?.isActive}
          canChangePassword={canChangePassword}
          onFieldsChange={handleFieldsChange}
          onCancel={handleCancel}
          onSave={handleSave}
          isSaving={isSaving}
          isTouched={isTouched}
        />
      </SectionCard>

      <SectionCard title={t("myProfile.sections.affectation")}>
        <Descriptions column={{ xs: 1, sm: 2 }} colon size="small">
          <Descriptions.Item label={t("myProfile.labels.agency")}>
            {profile?.agency?.name ?? (
              <Typography.Text type="secondary">
                {t("myProfile.labels.none")}
              </Typography.Text>
            )}
          </Descriptions.Item>
          <Descriptions.Item label={t("myProfile.labels.service")}>
            {profile?.service?.name ? (
              <Space size={8}>
                <span>{profile.service.name}</span>
                {isHeadOfOwnService && (
                  <Tag color="blue">
                    {t("myProfile.labels.head_of_service")}
                  </Tag>
                )}
              </Space>
            ) : (
              <Typography.Text type="secondary">
                {t("myProfile.labels.none")}
              </Typography.Text>
            )}
          </Descriptions.Item>
        </Descriptions>
      </SectionCard>

      <SectionCard title={t("myProfile.sections.roles")}>
        <RolesSection roles={roles} />
      </SectionCard>
    </PageContainer>
  );
});

MyProfile.displayName = "MyProfile";

export default MyProfile;
