// Definition des chemins et utilitaires de navigation pour les utilisateurs.

import { USER_ROUTES } from "../../../api/user/routes";
import type { NavigateFunction } from "react-router-dom";

// Regroupe les redirections vers les ecrans utilisateurs.
export const userNavigation = {
  navigateToUsers: (navigate: NavigateFunction) => {
    navigate(USER_ROUTES.LIST);
  },

  navigateToUserCreate: (navigate: NavigateFunction) => {
    navigate(USER_ROUTES.CREATE);
  },

  navigateToUserDetail: (navigate: NavigateFunction, userId: string) => {
    navigate(USER_ROUTES.VIEW(userId));
  },

  navigateToUserDetailEdit: (navigate: NavigateFunction, userId: string) => {
    navigate(USER_ROUTES.VIEW(userId), { state: { edit: true } });
  },

  navigateToUserEdit: (navigate: NavigateFunction, userId: string) => {
    navigate(USER_ROUTES.EDIT(userId));
  },
};
