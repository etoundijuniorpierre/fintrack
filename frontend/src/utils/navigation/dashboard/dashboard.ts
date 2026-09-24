// Navigation utilitaire vers les differents indicateurs du tableau de bord.

import type { NavigateFunction } from "react-router-dom";

// Regroupe les redirections principales depuis le tableau de bord.
export const dashboardNavigation = {
  navigateToDashboard: (navigate: NavigateFunction) => {
    navigate("/dashboard");
  },

  navigateToIncidents: (navigate: NavigateFunction) => {
    navigate("/dashboard/incidents");
  },

  navigateToReports: (navigate: NavigateFunction) => {
    navigate("/dashboard/reports");
  },

  navigateToSettings: (navigate: NavigateFunction) => {
    navigate("/dashboard/settings");
  },

  navigateToAudit: (navigate: NavigateFunction) => {
    navigate("/dashboard/audit");
  },
};
