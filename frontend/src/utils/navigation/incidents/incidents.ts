// Definition des chemins et utilitaires de navigation pour les incidents.

import { INCIDENT_ROUTES } from "../../../api/incident/routes/routes";
import type { NavigateFunction, NavigateOptions } from "react-router-dom";

// Choisit l'identifiant d'URL d'un incident : le code metier quand il
// existe, sinon l'UUID technique
export const incidentPathIdentifier = (incident: {
  id: string;
  reference?: string | null;
}): string => incident.reference ?? incident.id;

// Regroupe les redirections vers les ecrans incidents.
export const incidentNavigation = {
  navigateToIncidents: (
    navigate: NavigateFunction,
    options?: NavigateOptions,
  ) => {
    if (options) {
      navigate(INCIDENT_ROUTES.LIST, options);
      return;
    }
    navigate(INCIDENT_ROUTES.LIST);
  },

  navigateToIncidentCreate: (navigate: NavigateFunction) => {
    navigate(INCIDENT_ROUTES.CREATE);
  },

  navigateToIncidentDetail: (navigate: NavigateFunction, id: string) => {
    navigate(INCIDENT_ROUTES.VIEW(id));
  },
};
