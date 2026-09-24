// Routes de navigation front-end du module utilisateurs.
import { APP_ROUTES } from "../../utils/constants";

// Expose la constante USER_ROUTES utilisee par routes.
export const USER_ROUTES = {
  LIST: APP_ROUTES.USERS,
  CREATE: `${APP_ROUTES.USERS}/create`,
  VIEW: (id: string) => `${APP_ROUTES.USERS}/${id}`,
  EDIT: (id: string) => `${APP_ROUTES.USERS}/edit/${id}`,
} as const;
