// Page dediee a l'enregistrement d'un nouvel utilisateur.

import { useCallback, memo } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { userNavigation } from "../../../utils/navigation/users/users";
import { UserForm } from "../components";
import type { User } from "../../../api/user/types";
import { PageContainer, SectionCard } from "../../../components/Layout";
import { PageHeader } from "../../../components/ui";

// Rend le composant CreateUser pour l'interface create utilisateur.
const CreateUser = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  // Traite la confirmation de succes.
  const handleSuccess = useCallback(
    (user: User) => {
      userNavigation.navigateToUserDetail(navigate, user.id);
    },
    [navigate],
  );

  // Annule l'action en cours.
  const handleCancel = useCallback(
    () => userNavigation.navigateToUsers(navigate),
    [navigate],
  );

  return (
    <PageContainer>
      <PageHeader
        title={t("users.form.titles.create")}
        subtitle={t("users.form.subtitles.create")}
        onBack={handleCancel}
      />

      <SectionCard>
        <UserForm
          onSuccess={handleSuccess}
          onCancel={handleCancel}
          submitText={t("users.form.buttons.submit_create")}
        />
      </SectionCard>
    </PageContainer>
  );
};

export default memo(CreateUser);
