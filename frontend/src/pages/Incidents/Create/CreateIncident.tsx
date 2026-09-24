// Page de creation d'incident : affiche le formulaire et gere la redirection apres succes.
import { memo, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, Navigate } from "react-router-dom";
import { useAuth } from "../../../hooks/auth/useAuth";
import {
  incidentNavigation,
  incidentPathIdentifier,
} from "../../../utils/navigation/incidents/incidents";
import { INCIDENT_ROUTES } from "../../../api/incident/routes/routes";
import { PERMISSIONS } from "../../../utils/permissions/permissions";
import IncidentForm from "../components/Form/IncidentForm";
import { PageContainer, SectionCard } from "../../../components/Layout";
import { PageHeader } from "../../../components/ui";
import type { IncidentResponse } from "../../../api/incident/types";

// Rend le composant CreateIncident pour l'interface create incident.
const CreateIncident = memo(() => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();

  // Redirige vers le detail de l'incident cree.
  const handleSuccess = useCallback(
    (incident: IncidentResponse) =>
      incidentNavigation.navigateToIncidentDetail(
        navigate,
        incidentPathIdentifier(incident),
      ),
    [navigate],
  );

  // Traite l'annulation.
  const handleCancel = useCallback(
    () => incidentNavigation.navigateToIncidents(navigate),
    [navigate],
  );

  if (!hasPermission(PERMISSIONS.INCIDENT.CREATE)) {
    return <Navigate to={INCIDENT_ROUTES.LIST} replace />;
  }

  return (
    <PageContainer>
      <PageHeader
        title={t("incidents.form.titles.create")}
        subtitle={t("incidents.form.subtitles.create")}
        onBack={handleCancel}
      />

      <SectionCard>
        <IncidentForm onSuccess={handleSuccess} onCancel={handleCancel} />
      </SectionCard>
    </PageContainer>
  );
});

CreateIncident.displayName = "CreateIncident";

export default CreateIncident;
