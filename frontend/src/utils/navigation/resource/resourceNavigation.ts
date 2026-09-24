// Navigation ressource : resout et ouvre les pages de detail depuis un journal ou une liste.

import { APP_ROUTES } from "../../constants";
import type { NavigateFunction } from "react-router-dom";

// Regroupe les redirections vers les entites metier referencees par l'audit.
export const resourceNavigation = {
  // Resout le chemin de detail d'une ressource connue.
  getResourceDetailPath: (
    resourceType: string,
    resourceId: string | undefined,
  ): string => {
    if (!resourceId || resourceId === "-" || resourceId === "undefined")
      return "";

    switch (resourceType) {
      case "INCIDENT":
        return APP_ROUTES.INCIDENTS_DETAIL(resourceId);
      case "USER":
        return `${APP_ROUTES.USERS}/${resourceId}`;
      case "ROLE":
        return APP_ROUTES.SETTINGS_ROLES_DETAILS(resourceId);
      case "AGENCY":
        return APP_ROUTES.SETTINGS_AGENCIES_DETAILS(resourceId);
      case "SERVICE":
        return APP_ROUTES.SETTINGS_SERVICES_DETAILS(resourceId);
      case "REPORT":
        return APP_ROUTES.REPORTS;
      default:
        return "";
    }
  },

  // Redirige vers le detail d'une ressource et ferme le contexte appelant si besoin.
  navigateToResourceDetail: (
    navigate: NavigateFunction,
    resourceType: string,
    resourceId: string | undefined,
    options?: { onClose?: () => void },
  ): void => {
    const path = resourceNavigation.getResourceDetailPath(
      resourceType,
      resourceId,
    );
    if (path) {
      if (options?.onClose) {
        options.onClose();
      }
      navigate(path);
    }
  },
};
