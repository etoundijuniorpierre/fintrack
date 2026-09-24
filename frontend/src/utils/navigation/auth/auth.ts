// Fonctions utilitaires de gestion de redirection et droits d'acces d'authentification.

import type { NavigateFunction } from "react-router-dom";

// Regroupe les redirections du parcours d'authentification.
export const authNavigation = {
  navigateToLogin: (navigate: NavigateFunction) => {
    navigate("/login");
  },
};
